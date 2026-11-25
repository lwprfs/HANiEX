package com.yenaly.han1meviewer.SitesHub.Cosplaytele.ui.post

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.common.CosplayteleConstants
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.common.CosplayteleSettings
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.data.model.CosplayteleDetail
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.data.model.CosplayteleStream
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.data.remote.CosplayteleNetwork
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.data.remote.CosplayteleRepo
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.data.remote.CosplayteleStreamResolver
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.data.repository.CosplayteleHistoryRepo
import com.yenaly.han1meviewer.ui.component.player.SitePlayerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

typealias CosplaytellePlayerState = SitePlayerState

sealed interface CosplayteleDetailState {
    data object Loading : CosplayteleDetailState
    data class Success(val detail: CosplayteleDetail) : CosplayteleDetailState
    data class Error(val message: String) : CosplayteleDetailState
}

sealed interface CosplayteleStreamState {
    data object Idle : CosplayteleStreamState
    data object Loading : CosplayteleStreamState
    data class Failed(val reason: String) : CosplayteleStreamState
    data object Success : CosplayteleStreamState
}

class CosplaytellePostViewModel : ViewModel() {

    private companion object {
        const val TAG = "CosplaytellePostVM"
    }

    private val _detailState = MutableStateFlow<CosplayteleDetailState>(CosplayteleDetailState.Loading)
    val detailState: StateFlow<CosplayteleDetailState> = _detailState.asStateFlow()

    private val _playerState = MutableStateFlow(CosplaytellePlayerState())
    val playerState: StateFlow<CosplaytellePlayerState> = _playerState.asStateFlow()

    private val _streamState = MutableStateFlow<CosplayteleStreamState>(CosplayteleStreamState.Idle)
    val streamState: StateFlow<CosplayteleStreamState> = _streamState.asStateFlow()

    private val _selectedVideo = MutableStateFlow(0)
    val selectedVideo: StateFlow<Int> = _selectedVideo.asStateFlow()

    private val _referer = MutableStateFlow(CosplayteleConstants.STREAM_REFERER)
    val referer: StateFlow<String> = _referer.asStateFlow()
    private val _origin = MutableStateFlow(CosplayteleConstants.STREAM_ORIGIN)
    val origin: StateFlow<String> = _origin.asStateFlow()

    private var currentSlug = ""
    private var loadJob: Job? = null
    private var streamJob: Job? = null

    fun load(slug: String, force: Boolean = false) {
        if (!force && currentSlug == slug && _detailState.value is CosplayteleDetailState.Success) return
        currentSlug = slug
        loadJob?.cancel()
        streamJob?.cancel()
        _detailState.value = CosplayteleDetailState.Loading
        _streamState.value = CosplayteleStreamState.Idle
        _selectedVideo.value = 0
        _playerState.update {
            it.copy(url = "", quality = "", qualityOptions = emptyMap(), isReady = false, isPlaying = false)
        }
        loadJob = viewModelScope.launch {
            CosplayteleRepo.detail(slug)
                .onSuccess { detail ->
                    _detailState.value = CosplayteleDetailState.Success(detail)
                    runCatching {
                        CosplayteleHistoryRepo.touch(slug, detail.title, detail.coverUrl)
                    }
                    if (detail.hasVideo) resolve(detail, 0)
                }
                .onFailure {
                    Log.e(TAG, "detail failed", it)
                    _detailState.value = CosplayteleDetailState.Error(it.message ?: it.javaClass.simpleName)
                }
        }
    }

    fun selectVideo(index: Int) {
        val detail = (_detailState.value as? CosplayteleDetailState.Success)?.detail ?: return
        if (index !in detail.videos.indices) return
        resolve(detail, index)
    }

    fun retry() {
        val detail = (_detailState.value as? CosplayteleDetailState.Success)?.detail ?: return
        resolve(detail, _selectedVideo.value.coerceIn(0, (detail.videos.size - 1).coerceAtLeast(0)))
    }

    private fun resolve(detail: CosplayteleDetail, index: Int) {
        val ref = detail.videos.getOrNull(index) ?: return
        streamJob?.cancel()
        _selectedVideo.value = index
        streamJob = viewModelScope.launch {
            _streamState.value = CosplayteleStreamState.Loading
            _playerState.update { it.copy(url = "", isReady = false) }
            val streams: List<CosplayteleStream> =
                runCatching { CosplayteleStreamResolver.resolve(ref, detail.pageUrl) }
                    .onFailure { Log.e(TAG, "resolve failed", it) }
                    .getOrDefault(emptyList())
            val stream = CosplayteleStreamResolver.choose(streams, CosplayteleSettings.preferredQuality)
            if (stream == null) {
                _streamState.value = CosplayteleStreamState.Failed(ref.url)
                return@launch
            }
            _referer.value = stream.referer
            _origin.value = stream.origin
            CosplayteleNetwork.rememberStreamReferer(currentSlug, stream.referer)
            _playerState.update {
                it.copy(
                    url = stream.url,
                    quality = stream.label,
                    qualityOptions = streams.associate { s -> s.label to s.url },
                    isReady = true,
                )
            }
            _streamState.value = CosplayteleStreamState.Success
        }
    }

    fun setQuality(quality: String) {
        val url = _playerState.value.qualityOptions[quality] ?: return
        _playerState.update { it.copy(quality = quality, url = url) }
    }

    fun setSpeed(speed: Float) = _playerState.update { it.copy(speed = speed) }
    fun setPlaying(playing: Boolean) = _playerState.update { it.copy(isPlaying = playing) }
    fun setPosition(position: Long, duration: Long) = _playerState.update {
        it.copy(position = position, duration = if (duration > 0) duration else it.duration)
    }
    fun setFullscreen(fullscreen: Boolean) = _playerState.update { it.copy(isFullscreen = fullscreen) }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
        streamJob?.cancel()
    }
}
