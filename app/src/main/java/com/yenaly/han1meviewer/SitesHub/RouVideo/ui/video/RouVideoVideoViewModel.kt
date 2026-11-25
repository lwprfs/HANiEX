package com.yenaly.han1meviewer.SitesHub.RouVideo.ui.video

import com.yenaly.han1meviewer.ui.component.player.SitePlayerState
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.SitesHub.RouVideo.common.RouVideoConstants
import com.yenaly.han1meviewer.SitesHub.RouVideo.common.RouVideoSettings
import com.yenaly.han1meviewer.SitesHub.RouVideo.data.model.RouVideoDetail
import com.yenaly.han1meviewer.SitesHub.RouVideo.data.remote.RouVideoAccountRepo
import com.yenaly.han1meviewer.SitesHub.RouVideo.data.remote.RouVideoAuth
import com.yenaly.han1meviewer.SitesHub.RouVideo.data.remote.RouVideoNotSignedInException
import com.yenaly.han1meviewer.SitesHub.RouVideo.data.remote.RouVideoRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

typealias RouVideoPlayerState = SitePlayerState

sealed interface RouVideoDetailState {
    data object Loading : RouVideoDetailState
    data class Success(val detail: RouVideoDetail) : RouVideoDetailState
    data class Error(val message: String) : RouVideoDetailState
}

data class RouVideoViewerUi(
    val loaded: Boolean = false,
    val favorite: Boolean = false,
    val followed: Boolean = false,
    val positionMs: Long = 0L,
    val busy: Boolean = false,

    val sessionExpired: Boolean = false,
)

class RouVideoVideoViewModel : ViewModel() {

    private companion object {
        const val TAG = "RouVideoVideoVM"
    }

    private val _detailState = MutableStateFlow<RouVideoDetailState>(RouVideoDetailState.Loading)
    val detailState: StateFlow<RouVideoDetailState> = _detailState.asStateFlow()

    private val _playerState = MutableStateFlow(RouVideoPlayerState())
    val playerState: StateFlow<RouVideoPlayerState> = _playerState.asStateFlow()

    private val _viewer = MutableStateFlow(RouVideoViewerUi())
    val viewer: StateFlow<RouVideoViewerUi> = _viewer.asStateFlow()

    private var progressSent = false

    private var currentId = ""
    private var loadJob: Job? = null

    fun load(id: String, force: Boolean = false) {
        if (!force && currentId == id && _detailState.value is RouVideoDetailState.Success) return
        currentId = id
        loadJob?.cancel()
        _viewer.value = RouVideoViewerUi()
        progressSent = false
        _detailState.value = RouVideoDetailState.Loading
        _playerState.update {
            it.copy(url = "", quality = "", qualityOptions = emptyMap(), isReady = false, isPlaying = false)
        }
        loadJob = viewModelScope.launch {
            RouVideoRepo.detail(id)
                .onSuccess { detail ->
                    _detailState.value = RouVideoDetailState.Success(detail)

                    val url = RouVideoConstants.absolute(detail.streamPath)
                    _playerState.update {
                        it.copy(
                            url = url,
                            quality = "",
                            qualityOptions = emptyMap(),
                            isReady = true,
                        )
                    }
                    loadViewer(detail.id, detail.series?.id)
                    if (RouVideoSettings.reportPlays) {
                        launch { RouVideoRepo.reportPlay(detail.id, detail.tags) }
                    }
                }
                .onFailure {
                    Log.e(TAG, "detail failed", it)
                    _detailState.value =
                        RouVideoDetailState.Error(it.message ?: it.javaClass.simpleName)
                }
        }
    }

    fun loadViewer(videoId: String = currentId, seriesId: String? = null) {
        if (!RouVideoAuth.isLoggedIn) {
            _viewer.value = RouVideoViewerUi()
            return
        }
        val series = seriesId
            ?: (_detailState.value as? RouVideoDetailState.Success)?.detail?.series?.id
        viewModelScope.launch {
            val state = RouVideoAccountRepo.viewerState(videoId)
            val followed = if (series != null) {
                RouVideoAccountRepo.followedSeriesIds().getOrNull()?.contains(series) == true
            } else false
            state.onSuccess { v ->
                _viewer.value = RouVideoViewerUi(
                    loaded = true,
                    favorite = v.favorite,
                    followed = followed,
                    positionMs = (v.positionSec * 1000).toLong(),
                )
            }.onFailure { e ->
                if (e is RouVideoNotSignedInException) {
                    RouVideoAuth.onSessionRejected()
                    _viewer.value = RouVideoViewerUi(sessionExpired = true)
                }
            }
        }
    }

    fun toggleFavorite() {
        val v = _viewer.value
        if (!RouVideoAuth.isLoggedIn || v.busy || currentId.isEmpty()) return
        _viewer.value = v.copy(favorite = !v.favorite, busy = true)
        viewModelScope.launch {
            RouVideoAccountRepo.toggleFavorite(currentId)
                .onSuccess { _viewer.update { it.copy(busy = false) } }
                .onFailure { e ->
                    if (e is RouVideoNotSignedInException) RouVideoAuth.onSessionRejected()
                    _viewer.update {
                        it.copy(
                            favorite = v.favorite,
                            busy = false,
                            sessionExpired = e is RouVideoNotSignedInException,
                        )
                    }
                }
        }
    }

    fun toggleFollow() {
        val detail = (_detailState.value as? RouVideoDetailState.Success)?.detail ?: return
        val series = detail.series ?: return
        val v = _viewer.value
        if (!RouVideoAuth.isLoggedIn || v.busy) return
        val target = !v.followed
        _viewer.value = v.copy(followed = target, busy = true)
        viewModelScope.launch {
            val episode = detail.episodes.firstOrNull { it.id == detail.id }?.episode
            RouVideoAccountRepo.setFollow(series.id, target, if (target) episode else null)
                .onSuccess { _viewer.update { it.copy(busy = false) } }
                .onFailure { e ->
                    if (e is RouVideoNotSignedInException) RouVideoAuth.onSessionRejected()
                    _viewer.update {
                        it.copy(
                            followed = v.followed,
                            busy = false,
                            sessionExpired = e is RouVideoNotSignedInException,
                        )
                    }
                }
        }
    }

    fun syncProgress(positionMs: Long, ended: Boolean) {
        if (!RouVideoAuth.isLoggedIn || currentId.isEmpty() || positionMs <= 0L) return
        val id = currentId
        val first = !progressSent
        progressSent = true
        RouVideoAuth.appScope.launch {
            RouVideoAccountRepo.saveProgress(id, positionMs / 1000.0, ended, first)
                .onFailure { e ->
                    if (e is RouVideoNotSignedInException) RouVideoAuth.onSessionRejected()
                }
        }
    }

    fun reloadStream() {
        val detail = (_detailState.value as? RouVideoDetailState.Success)?.detail ?: return
        _playerState.update { it.copy(url = "", isReady = false) }
        viewModelScope.launch {
            _playerState.update {
                it.copy(url = RouVideoConstants.absolute(detail.streamPath), isReady = true)
            }
        }
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
    }
}
