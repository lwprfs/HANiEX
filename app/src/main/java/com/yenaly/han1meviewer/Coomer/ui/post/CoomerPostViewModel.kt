package com.yenaly.han1meviewer.Coomer.ui.post

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.Coomer.common.CoomerConstants
import com.yenaly.han1meviewer.Coomer.data.model.CoomerDetail
import com.yenaly.han1meviewer.Coomer.data.remote.CoomerRepo
import com.yenaly.han1meviewer.Coomer.data.repository.CoomerHistoryRepo
import com.yenaly.han1meviewer.ui.component.player.SitePlayerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

typealias CoomerPlayerState = SitePlayerState

sealed interface CoomerDetailState {
    data object Loading : CoomerDetailState
    data class Success(val detail: CoomerDetail) : CoomerDetailState
    data class Error(val message: String) : CoomerDetailState
}

class CoomerPostViewModel : ViewModel() {

    private companion object {
        const val TAG = "CoomerPostVM"
    }

    private val _detailState = MutableStateFlow<CoomerDetailState>(CoomerDetailState.Loading)
    val detailState: StateFlow<CoomerDetailState> = _detailState.asStateFlow()

    private val _playerState = MutableStateFlow(CoomerPlayerState())
    val playerState: StateFlow<CoomerPlayerState> = _playerState.asStateFlow()

    private val _selectedVideo = MutableStateFlow(0)
    val selectedVideo: StateFlow<Int> = _selectedVideo.asStateFlow()

    val referer: String get() = "${CoomerConstants.BASE_URL}/"
    val origin: String get() = CoomerConstants.BASE_URL

    private var currentSlug = ""
    private var loadJob: Job? = null

    fun load(slug: String, force: Boolean = false) {
        if (!force && currentSlug == slug && _detailState.value is CoomerDetailState.Success) return
        currentSlug = slug
        loadJob?.cancel()
        _detailState.value = CoomerDetailState.Loading
        _selectedVideo.value = 0
        _playerState.update {
            it.copy(url = "", quality = "", qualityOptions = emptyMap(), isReady = false, isPlaying = false)
        }
        loadJob = viewModelScope.launch {
            CoomerRepo.detail(slug)
                .onSuccess { detail ->
                    _detailState.value = CoomerDetailState.Success(detail)
                    runCatching {
                        CoomerHistoryRepo.touch(slug, detail.title, detail.coverUrl)
                    }
                    if (detail.hasVideo) play(detail, 0)
                }
                .onFailure {
                    Log.e(TAG, "detail failed", it)
                    _detailState.value = CoomerDetailState.Error(it.message ?: it.javaClass.simpleName)
                }
        }
    }

    fun selectVideo(index: Int) {
        val detail = (_detailState.value as? CoomerDetailState.Success)?.detail ?: return
        if (index !in detail.videos.indices) return
        play(detail, index)
    }

    private fun play(detail: CoomerDetail, index: Int) {
        val url = detail.videos.getOrNull(index) ?: return
        _selectedVideo.value = index
        _playerState.update {
            it.copy(
                url = url,
                quality = "",
                qualityOptions = emptyMap(),
                isReady = true,
                position = 0L,
            )
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
