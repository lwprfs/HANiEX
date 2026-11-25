package com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.post

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryConstants
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryDetail
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote.LeakGalleryRepo
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.repository.LeakGalleryHistoryRepo
import com.yenaly.han1meviewer.ui.component.player.SitePlayerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

typealias LeakGalleryPlayerState = SitePlayerState

sealed interface LeakGalleryDetailState {
    data object Loading : LeakGalleryDetailState
    data class Success(val detail: LeakGalleryDetail) : LeakGalleryDetailState
    data class Error(val message: String) : LeakGalleryDetailState
}

class LeakGalleryPostViewModel : ViewModel() {

    private companion object {
        const val TAG = "LeakGalleryPostVM"
    }

    private val _detailState = MutableStateFlow<LeakGalleryDetailState>(LeakGalleryDetailState.Loading)
    val detailState: StateFlow<LeakGalleryDetailState> = _detailState.asStateFlow()

    private val _playerState = MutableStateFlow(LeakGalleryPlayerState())
    val playerState: StateFlow<LeakGalleryPlayerState> = _playerState.asStateFlow()

    val referer: String get() = "${LeakGalleryConstants.BASE_URL}/"
    val origin: String get() = LeakGalleryConstants.BASE_URL

    private var currentSlug = ""
    private var loadJob: Job? = null

    fun load(slug: String, force: Boolean = false) {
        if (!force && currentSlug == slug && _detailState.value is LeakGalleryDetailState.Success) return
        currentSlug = slug
        loadJob?.cancel()
        _detailState.value = LeakGalleryDetailState.Loading
        _playerState.update {
            it.copy(url = "", quality = "", qualityOptions = emptyMap(), isReady = false, isPlaying = false)
        }
        loadJob = viewModelScope.launch {
            LeakGalleryRepo.detail(slug)
                .onSuccess { detail ->
                    _detailState.value = LeakGalleryDetailState.Success(detail)
                    runCatching {
                        LeakGalleryHistoryRepo.touch(slug, detail.title, detail.coverUrl)
                    }
                    if (detail.hasVideo) {
                        _playerState.update {
                            it.copy(
                                url = detail.mediaUrl,
                                quality = "",
                                qualityOptions = emptyMap(),
                                isReady = true,
                                position = 0L,
                            )
                        }
                    }
                }
                .onFailure {
                    Log.e(TAG, "detail failed", it)
                    _detailState.value = LeakGalleryDetailState.Error(it.message ?: it.javaClass.simpleName)
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
