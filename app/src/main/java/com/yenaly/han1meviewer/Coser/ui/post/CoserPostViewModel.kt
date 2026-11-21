package com.yenaly.han1meviewer.Coser.ui.post

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.Coser.common.CoserConstants
import com.yenaly.han1meviewer.Coser.common.CoserSettings
import com.yenaly.han1meviewer.Coser.data.model.CoserDetail
import com.yenaly.han1meviewer.Coser.data.model.CoserStream
import com.yenaly.han1meviewer.Coser.data.remote.CoserNetwork
import com.yenaly.han1meviewer.Coser.data.remote.CoserRepo
import com.yenaly.han1meviewer.Coser.data.remote.CoserStreamResolver
import com.yenaly.han1meviewer.Coser.data.repository.CoserHistoryRepo
import com.yenaly.han1meviewer.ui.component.player.SitePlayerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

typealias CoserPlayerState = SitePlayerState

sealed interface CoserDetailState {
    data object Loading : CoserDetailState
    data class Success(val detail: CoserDetail) : CoserDetailState
    data class Error(val message: String) : CoserDetailState
}

sealed interface CoserStreamState {
    data object Idle : CoserStreamState
    data object Loading : CoserStreamState
    data class Failed(val reason: String) : CoserStreamState
    data object Success : CoserStreamState
}

class CoserPostViewModel : ViewModel() {

    private companion object {
        const val TAG = "CoserPostVM"
    }

    private val _detailState = MutableStateFlow<CoserDetailState>(CoserDetailState.Loading)
    val detailState: StateFlow<CoserDetailState> = _detailState.asStateFlow()

    private val _playerState = MutableStateFlow(CoserPlayerState())
    val playerState: StateFlow<CoserPlayerState> = _playerState.asStateFlow()

    private val _streamState = MutableStateFlow<CoserStreamState>(CoserStreamState.Idle)
    val streamState: StateFlow<CoserStreamState> = _streamState.asStateFlow()

    private val _selectedVideo = MutableStateFlow(0)
    val selectedVideo: StateFlow<Int> = _selectedVideo.asStateFlow()

    private val _selectedMirror = MutableStateFlow(0)
    val selectedMirror: StateFlow<Int> = _selectedMirror.asStateFlow()

    private val _referer = MutableStateFlow(CoserConstants.STREAM_REFERER)
    val referer: StateFlow<String> = _referer.asStateFlow()
    private val _origin = MutableStateFlow(CoserConstants.STREAM_ORIGIN)
    val origin: StateFlow<String> = _origin.asStateFlow()

    private var currentSlug = ""
    private var loadJob: Job? = null
    private var streamJob: Job? = null

    fun load(slug: String, force: Boolean = false) {
        if (!force && currentSlug == slug && _detailState.value is CoserDetailState.Success) return
        currentSlug = slug
        loadJob?.cancel()
        streamJob?.cancel()
        _detailState.value = CoserDetailState.Loading
        _streamState.value = CoserStreamState.Idle
        _selectedVideo.value = 0
        _selectedMirror.value = 0
        _playerState.update {
            it.copy(url = "", quality = "", qualityOptions = emptyMap(), isReady = false, isPlaying = false)
        }
        loadJob = viewModelScope.launch {
            CoserRepo.detail(slug)
                .onSuccess { detail ->
                    _detailState.value = CoserDetailState.Success(detail)
                    runCatching {
                        CoserHistoryRepo.touch(slug, detail.title, detail.coverUrl)
                    }
                    if (detail.hasVideo) resolve(detail, 0)
                }
                .onFailure {
                    Log.e(TAG, "detail failed", it)
                    _detailState.value = CoserDetailState.Error(it.message ?: it.javaClass.simpleName)
                }
        }
    }

    fun selectVideo(index: Int) {
        val detail = (_detailState.value as? CoserDetailState.Success)?.detail ?: return
        if (index !in detail.videos.indices) return
        _selectedMirror.value = 0
        resolve(detail, index)
    }

    fun selectMirror(mirror: Int) {
        val detail = (_detailState.value as? CoserDetailState.Success)?.detail ?: return
        val video = detail.videos.getOrNull(_selectedVideo.value) ?: return
        if (mirror !in video.mirrors.indices) return
        _selectedMirror.value = mirror
        resolve(detail, _selectedVideo.value, mirror)
    }

    fun retry() {
        val detail = (_detailState.value as? CoserDetailState.Success)?.detail ?: return
        resolve(detail, _selectedVideo.value.coerceIn(0, (detail.videos.size - 1).coerceAtLeast(0)))
    }

    private fun resolve(detail: CoserDetail, index: Int, mirror: Int = _selectedMirror.value) {
        val ref = detail.videos.getOrNull(index) ?: return
        streamJob?.cancel()
        _selectedVideo.value = index
        streamJob = viewModelScope.launch {
            _streamState.value = CoserStreamState.Loading
            _playerState.update { it.copy(url = "", isReady = false) }
            val streams: List<CoserStream> =
                runCatching { CoserStreamResolver.resolve(ref, detail.pageUrl, mirror) }
                    .onFailure { Log.e(TAG, "resolve failed", it) }
                    .getOrDefault(emptyList())
            val stream = CoserStreamResolver.choose(streams, CoserSettings.preferredQuality)
            if (stream == null) {
                _streamState.value = CoserStreamState.Failed(ref.url)
                return@launch
            }
            _referer.value = stream.referer
            _origin.value = stream.origin
            CoserNetwork.rememberStreamReferer(currentSlug, stream.referer)
            _playerState.update {
                it.copy(
                    url = stream.url,
                    quality = stream.label,
                    qualityOptions = streams.associate { s -> s.label to s.url },
                    isReady = true,
                )
            }
            _streamState.value = CoserStreamState.Success
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
