package com.yenaly.han1meviewer.SitesHub.XXXTube.ui.video

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.SitesHub.XXXTube.data.model.XXXTubeDetail
import com.yenaly.han1meviewer.SitesHub.XXXTube.data.model.XXXTubeSource
import com.yenaly.han1meviewer.SitesHub.XXXTube.data.remote.XXXTubeRepo
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.player.SitePlayerState
import com.yenaly.han1meviewer.util.applicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

typealias XXXTubePlayerState = SitePlayerState

sealed interface XXXTubeDetailState {
    data object Loading : XXXTubeDetailState
    data class Success(val detail: XXXTubeDetail) : XXXTubeDetailState
    data class Error(val message: String) : XXXTubeDetailState
}

sealed interface XXXTubeStreamState {
    data object Idle : XXXTubeStreamState
    data class Failed(val reason: String) : XXXTubeStreamState
    data object Success : XXXTubeStreamState
}

class XXXTubeVideoViewModel : ViewModel() {

    private companion object {
        const val TAG = "XXXTubeVideoVM"
    }

    private val _detailState = MutableStateFlow<XXXTubeDetailState>(XXXTubeDetailState.Loading)
    val detailState: StateFlow<XXXTubeDetailState> = _detailState.asStateFlow()

    private val _playerState = MutableStateFlow(XXXTubePlayerState())
    val playerState: StateFlow<XXXTubePlayerState> = _playerState.asStateFlow()

    private val _streamState = MutableStateFlow<XXXTubeStreamState>(XXXTubeStreamState.Idle)
    val streamState: StateFlow<XXXTubeStreamState> = _streamState.asStateFlow()

    private val _referer = MutableStateFlow("")
    val referer: StateFlow<String> = _referer.asStateFlow()

    private var currentSources: List<XXXTubeSource> = emptyList()

    private var currentCode: String = ""
    private var loadJob: Job? = null

    fun load(code: String, force: Boolean = false) {
        if (!force && currentCode == code && _detailState.value is XXXTubeDetailState.Success) return
        currentCode = code
        loadJob?.cancel()
        _detailState.value = XXXTubeDetailState.Loading
        _streamState.value = XXXTubeStreamState.Idle
        _referer.value = ""
        currentSources = emptyList()
        _playerState.update {
            it.copy(url = "", quality = "", qualityOptions = emptyMap(), isReady = false, isPlaying = false)
        }
        loadJob = viewModelScope.launch {
            XXXTubeRepo.detail(code)
                .onSuccess { detail ->
                    _detailState.value = XXXTubeDetailState.Success(detail)
                    applySources(detail)
                }
                .onFailure {
                    Log.e(TAG, "detail failed", it)
                    _detailState.value =
                        XXXTubeDetailState.Error(it.message ?: it.javaClass.simpleName)
                }
        }
    }

    private fun pick(sources: List<XXXTubeSource>): XXXTubeSource? =
        sources.maxByOrNull { it.height } ?: sources.firstOrNull()

    private fun applySources(detail: XXXTubeDetail) {
        val chosen = pick(detail.sources)
        if (chosen == null) {
            _streamState.value =
                XXXTubeStreamState.Failed(applicationContext.getString(R.string.xxxtube_err_no_link))
            return
        }
        currentSources = detail.sources
        _referer.value = chosen.referer
        _playerState.update {
            it.copy(
                url = chosen.url,
                quality = chosen.label,
                qualityOptions = detail.sources.associate { s -> s.label to s.url },
                isReady = true,
            )
        }
        _streamState.value = XXXTubeStreamState.Success
    }

    fun retry() = load(currentCode, force = true)

    fun setQuality(quality: String) {
        val url = _playerState.value.qualityOptions[quality] ?: return
        currentSources.firstOrNull { it.label == quality }?.let { _referer.value = it.referer }
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
