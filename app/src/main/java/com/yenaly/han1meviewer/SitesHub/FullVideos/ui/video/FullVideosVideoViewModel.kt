package com.yenaly.han1meviewer.SitesHub.FullVideos.ui.video

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.SitesHub.FullVideos.common.FullVideosConstants
import com.yenaly.han1meviewer.SitesHub.FullVideos.common.FullVideosSettings
import com.yenaly.han1meviewer.SitesHub.FullVideos.data.model.FullVideosDetail
import com.yenaly.han1meviewer.SitesHub.FullVideos.data.model.FullVideosSource
import com.yenaly.han1meviewer.SitesHub.FullVideos.data.remote.FullVideosRepo
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.player.SitePlayerState
import com.yenaly.han1meviewer.util.applicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

typealias FullVideosPlayerState = SitePlayerState

sealed interface FullVideosDetailState {
    data object Loading : FullVideosDetailState
    data class Success(val detail: FullVideosDetail) : FullVideosDetailState
    data class Error(val message: String) : FullVideosDetailState
}

sealed interface FullVideosStreamState {
    data object Idle : FullVideosStreamState
    data class Failed(val reason: String) : FullVideosStreamState
    data object Success : FullVideosStreamState
}

class FullVideosVideoViewModel : ViewModel() {

    private companion object {
        const val TAG = "FullVideosVideoVM"
    }

    private val _detailState = MutableStateFlow<FullVideosDetailState>(FullVideosDetailState.Loading)
    val detailState: StateFlow<FullVideosDetailState> = _detailState.asStateFlow()

    private val _playerState = MutableStateFlow(FullVideosPlayerState())
    val playerState: StateFlow<FullVideosPlayerState> = _playerState.asStateFlow()

    private val _streamState = MutableStateFlow<FullVideosStreamState>(FullVideosStreamState.Idle)
    val streamState: StateFlow<FullVideosStreamState> = _streamState.asStateFlow()

    val referer: StateFlow<String> = MutableStateFlow("").asStateFlow()

    private var currentCode: String = ""
    private var loadJob: Job? = null

    fun load(code: String, force: Boolean = false) {
        if (!force && currentCode == code && _detailState.value is FullVideosDetailState.Success) return
        currentCode = code
        loadJob?.cancel()
        _detailState.value = FullVideosDetailState.Loading
        _streamState.value = FullVideosStreamState.Idle
        _playerState.update {
            it.copy(url = "", quality = "", qualityOptions = emptyMap(), isReady = false, isPlaying = false)
        }
        loadJob = viewModelScope.launch {
            FullVideosRepo.detail(code)
                .onSuccess { detail ->
                    _detailState.value = FullVideosDetailState.Success(detail)
                    applySources(detail)
                }
                .onFailure {
                    Log.e(TAG, "detail failed", it)
                    _detailState.value =
                        FullVideosDetailState.Error(it.message ?: it.javaClass.simpleName)
                }
        }
    }

    private fun pick(sources: List<FullVideosSource>): FullVideosSource? {
        val target = FullVideosSettings.preferredQuality.removeSuffix("p").toIntOrNull() ?: 1080
        val desc = sources.sortedByDescending { it.height }
        return desc.firstOrNull { it.height in 1..target } ?: desc.lastOrNull()
    }

    private fun applySources(detail: FullVideosDetail) {
        val chosen = pick(detail.sources)
        if (chosen == null) {
            _streamState.value =
                FullVideosStreamState.Failed(applicationContext.getString(R.string.fullvideos_err_no_link))
            return
        }
        _playerState.update {
            it.copy(
                url = chosen.url,
                quality = chosen.label,
                qualityOptions = detail.sources.associate { s -> s.label to s.url },
                isReady = true,
            )
        }
        _streamState.value = FullVideosStreamState.Success
    }

    fun retry() = load(currentCode, force = true)

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
    }
}
