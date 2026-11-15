package com.yenaly.han1meviewer.Buondua.ui.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.Buondua.data.model.BuonduaAlbumPage
import com.yenaly.han1meviewer.Buondua.data.model.BuonduaLink
import com.yenaly.han1meviewer.Buondua.data.model.BuonduaPhoto
import com.yenaly.han1meviewer.Buondua.data.model.BuonduaVideo
import com.yenaly.han1meviewer.Buondua.data.remote.BuonduaRepo
import com.yenaly.han1meviewer.Buondua.data.repository.BuonduaHistoryRepo
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BuonduaAlbumUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val code: String = "",
    val title: String = "",
    val pageUrl: String = "",
    val coverUrl: String = "",
    val photoCount: Int? = null,
    val videoCount: Int? = null,
    val loadedPages: Int = 0,
    val totalPages: Int = 1,
    val photos: List<BuonduaPhoto> = emptyList(),
    val videos: List<BuonduaVideo> = emptyList(),
    val tags: List<BuonduaLink> = emptyList(),
    val related: List<HanimeInfo> = emptyList(),
    val isLoadingMore: Boolean = false,
) {
    val hasMorePages: Boolean get() = loadedPages in 1 until totalPages
}

class BuonduaAlbumViewModel : ViewModel() {

    private val _state = MutableStateFlow(BuonduaAlbumUiState())
    val state: StateFlow<BuonduaAlbumUiState> = _state.asStateFlow()

    private val _inline = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val inlinePlayable: StateFlow<Map<String, Boolean>> = _inline.asStateFlow()

    private var code: String = ""
    private var job: Job? = null
    private val probed = HashSet<String>()

    fun load(albumCode: String, force: Boolean = false) {
        if (!force && code == albumCode && _state.value.loadedPages > 0) return
        code = albumCode
        job?.cancel()
        probed.clear()
        _inline.value = emptyMap()
        _state.value = BuonduaAlbumUiState(code = albumCode)
        job = viewModelScope.launch {
            BuonduaRepo.album(albumCode, 1)
                .onSuccess { page ->
                    apply(page, replace = true)
                    runCatching {
                        BuonduaHistoryRepo.recordVisit(albumCode, page.title, page.coverUrl, 1)
                    }
                }
                .onFailure { e ->
                    _state.value = BuonduaAlbumUiState(
                        code = albumCode,
                        isLoading = false,
                        error = e.message ?: e.javaClass.simpleName,
                    )
                }
        }
    }

    fun retry() = load(code, force = true)

    fun loadNextPage() {
        val s = _state.value
        if (s.isLoading || s.isLoadingMore || !s.hasMorePages) return
        _state.update { it.copy(isLoadingMore = true) }
        val next = s.loadedPages + 1
        job = viewModelScope.launch {
            BuonduaRepo.album(code, next)
                .onSuccess { apply(it, replace = false) }
                .onFailure { _state.update { old -> old.copy(isLoadingMore = false) } }
        }
    }

    private fun apply(page: BuonduaAlbumPage, replace: Boolean) {
        _state.update { old ->
            val photos = if (replace) page.photos
            else (old.photos.map { it.url } + page.photos.map { it.url }).distinct()
                .mapIndexed { i, u -> BuonduaPhoto(u, i) }
            val videos = (if (replace) page.videos else old.videos + page.videos).distinctBy { it.url }
            old.copy(
                isLoading = false,
                error = null,
                title = if (replace) page.title else old.title,
                pageUrl = page.pageUrl,
                coverUrl = old.coverUrl.ifBlank { page.coverUrl },
                photoCount = page.photoCount ?: old.photoCount,
                videoCount = page.videoCount ?: old.videoCount,
                loadedPages = page.page.coerceAtLeast(if (replace) 1 else old.loadedPages + 1),
                totalPages = page.totalPages,
                photos = photos,
                videos = videos,
                tags = if (replace) page.tags else old.tags,
                related = if (replace) page.related else old.related,
                isLoadingMore = false,
            )
        }
        probeNewVideos(page.videos)
    }

    private fun probeNewVideos(videos: List<BuonduaVideo>) {
        videos.filter { probed.add(it.url) }.forEach { video ->
            viewModelScope.launch {
                val ok = BuonduaRepo.isInlinePlayable(video.url)
                _inline.update { it + (video.url to ok) }
            }
        }
    }
}
