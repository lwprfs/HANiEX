package com.yenaly.han1meviewer.SitesHub.Buondua.ui.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.SitesHub.Buondua.data.model.BuonduaAlbumPage
import com.yenaly.han1meviewer.SitesHub.Buondua.data.model.BuonduaLink
import com.yenaly.han1meviewer.SitesHub.Buondua.data.model.BuonduaPhoto
import com.yenaly.han1meviewer.SitesHub.Buondua.data.model.BuonduaVideo
import com.yenaly.han1meviewer.SitesHub.Buondua.data.remote.BuonduaRepo
import com.yenaly.han1meviewer.SitesHub.Buondua.data.repository.BuonduaHistoryRepo
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.ui.component.player.SitePlayerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val loadMoreFailed: Boolean = false,
) {
    val hasMorePages: Boolean get() = loadedPages in 1 until totalPages

    val hasVideos: Boolean get() = videos.isNotEmpty() || (videoCount ?: 0) > 0
}

class BuonduaAlbumViewModel : ViewModel() {

    private val _state = MutableStateFlow(BuonduaAlbumUiState())
    val state: StateFlow<BuonduaAlbumUiState> = _state.asStateFlow()

    private val _player = MutableStateFlow(SitePlayerState())
    val playerState: StateFlow<SitePlayerState> = _player.asStateFlow()

    private val _selectedVideo = MutableStateFlow(0)
    val selectedVideo: StateFlow<Int> = _selectedVideo.asStateFlow()

    private var code: String = ""
    private var photosOnly: Boolean = false
    private var job: Job? = null
    private var autoJob: Job? = null

    private val photosByPage = java.util.TreeMap<Int, List<String>>()
    private val donePages = HashSet<Int>()
    private val failedPages = HashSet<Int>()

    fun load(albumCode: String, force: Boolean = false, photosOnly: Boolean = false) {
        if (!force && code == albumCode && _state.value.loadedPages > 0) return
        code = albumCode
        this.photosOnly = photosOnly
        job?.cancel()
        autoJob?.cancel()
        photosByPage.clear()
        donePages.clear()
        failedPages.clear()
        _selectedVideo.value = 0
        _player.value = SitePlayerState()
        _state.value = BuonduaAlbumUiState(code = albumCode)
        job = viewModelScope.launch {
            BuonduaRepo.album(albumCode, 1)
                .onSuccess { page ->
                    apply(page, requested = 1, replace = true)
                    donePages += 1
                    if (photosOnly) {

                        loadAllPages()
                    } else {
                        runCatching {
                            BuonduaHistoryRepo.recordVisit(albumCode, page.title, page.coverUrl, 1)
                        }
                        fetchMissingVideos(page)
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

    fun retry() = load(code, force = true, photosOnly = photosOnly)

    fun loadNextPage() {
        val s = _state.value
        if (autoJob?.isActive == true) return
        if (s.isLoading || s.isLoadingMore || !s.hasMorePages) return
        _state.update { it.copy(isLoadingMore = true) }
        val next = s.loadedPages + 1
        job = viewModelScope.launch {
            val page = fetchPage(next)
            if (page != null) {
                apply(page, requested = next, replace = false)
                donePages += next
            } else {
                _state.update { old -> old.copy(isLoadingMore = false, loadMoreFailed = true) }
            }
        }
    }

    fun retryMissingPages() = loadAllPages()

    private suspend fun fetchPage(page: Int): BuonduaAlbumPage? {
        repeat(3) { attempt ->
            BuonduaRepo.album(code, page).onSuccess { return it }
            delay(400L * (attempt + 1))
        }
        return null
    }

    private fun loadAllPages() {
        if (autoJob?.isActive == true) return
        autoJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true, loadMoreFailed = false) }
            var next = 2
            while (next <= BuonduaRepo.MAX_PAGES) {
                val s = _state.value

                val probing = next > s.totalPages
                val expected = s.photoCount
                if (probing && (expected == null || s.photos.size >= expected)) break
                if (next in donePages) { next++; continue }

                val page = fetchPage(next)
                if (page == null) {
                    if (probing) break
                    failedPages += next
                } else {
                    val before = _state.value.photos.size
                    apply(page, requested = next, replace = false)
                    donePages += next
                    failedPages -= next
                    _state.update { it.copy(isLoadingMore = true) }
                    if (probing && _state.value.photos.size == before) break
                }
                next++
            }
            _state.update { it.copy(isLoadingMore = false, loadMoreFailed = failedPages.isNotEmpty()) }
        }
    }

    private fun apply(page: BuonduaAlbumPage, requested: Int, replace: Boolean) {
        if (replace) photosByPage.clear()
        photosByPage[requested] = page.photos.map { it.url }
        _state.update { old ->
            val photos = photosByPage.values.flatten().distinct()
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
                loadedPages = maxOf(old.loadedPages, requested),
                totalPages = maxOf(old.totalPages, page.totalPages, requested),
                photos = photos,
                videos = videos,
                tags = if (replace) page.tags else old.tags,
                related = if (replace) page.related else old.related,
                isLoadingMore = false,
            )
        }
        if (!photosOnly) selectFirstVideoIfNeeded()
    }

    private fun fetchMissingVideos(first: BuonduaAlbumPage) {
        val expected = first.videoCount ?: 0
        if (expected <= first.videos.size || first.totalPages <= 1) return
        viewModelScope.launch {
            val all = runCatching { BuonduaRepo.allVideos(code, first) }.getOrNull() ?: return@launch
            _state.update { old -> old.copy(videos = (old.videos + all).distinctBy { it.url }) }
            selectFirstVideoIfNeeded()
        }
    }

    private fun selectFirstVideoIfNeeded() {
        if (_player.value.url.isNotBlank()) return
        selectVideo(0)
    }

    fun selectVideo(index: Int) {
        val video = _state.value.videos.getOrNull(index) ?: return
        _selectedVideo.value = index
        _player.update {
            it.copy(url = video.url, quality = "", qualityOptions = emptyMap(), isReady = true, isPlaying = false, position = 0L, duration = 0L)
        }
    }

    fun setSpeed(speed: Float) = _player.update { it.copy(speed = speed) }
    fun setPlaying(playing: Boolean) = _player.update { it.copy(isPlaying = playing) }
    fun setPosition(position: Long, duration: Long) = _player.update {
        it.copy(position = position, duration = if (duration > 0) duration else it.duration)
    }
    fun setFullscreen(fullscreen: Boolean) = _player.update { it.copy(isFullscreen = fullscreen) }
}
