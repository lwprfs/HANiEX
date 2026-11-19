package com.yenaly.han1meviewer.FuckingSession.ui.video

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.FuckingSession.data.model.FuckingSessionDetail
import com.yenaly.han1meviewer.FuckingSession.data.model.FuckingSessionSource
import com.yenaly.han1meviewer.FuckingSession.data.remote.FuckingSessionRepo
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.player.SitePlayerState
import com.yenaly.han1meviewer.util.applicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

typealias FuckingSessionPlayerState = SitePlayerState

sealed interface FuckingSessionDetailState {
    data object Loading : FuckingSessionDetailState
    data class Success(val detail: FuckingSessionDetail) : FuckingSessionDetailState
    data class Error(val message: String) : FuckingSessionDetailState
}

sealed interface FuckingSessionStreamState {
    data object Idle : FuckingSessionStreamState
    data class Failed(val reason: String) : FuckingSessionStreamState
    data object Success : FuckingSessionStreamState
}

class FuckingSessionVideoViewModel : ViewModel() {

    private companion object {
        const val TAG = "FuckingSessionVideoVM"
    }

    private val _detailState = MutableStateFlow<FuckingSessionDetailState>(FuckingSessionDetailState.Loading)
    val detailState: StateFlow<FuckingSessionDetailState> = _detailState.asStateFlow()

    private val _playerState = MutableStateFlow(FuckingSessionPlayerState())
    val playerState: StateFlow<FuckingSessionPlayerState> = _playerState.asStateFlow()

    private val _streamState = MutableStateFlow<FuckingSessionStreamState>(FuckingSessionStreamState.Idle)
    val streamState: StateFlow<FuckingSessionStreamState> = _streamState.asStateFlow()

    private val _referer = MutableStateFlow("")
    val referer: StateFlow<String> = _referer.asStateFlow()

    private var currentSources: List<FuckingSessionSource> = emptyList()

    private var currentCode: String = ""
    private var loadJob: Job? = null

    fun load(code: String, force: Boolean = false) {
        if (!force && currentCode == code && _detailState.value is FuckingSessionDetailState.Success) return
        currentCode = code
        loadJob?.cancel()
        _detailState.value = FuckingSessionDetailState.Loading
        _streamState.value = FuckingSessionStreamState.Idle
        _referer.value = ""
        currentSources = emptyList()
        _playerState.update {
            it.copy(url = "", quality = "", qualityOptions = emptyMap(), isReady = false, isPlaying = false)
        }
        loadJob = viewModelScope.launch {
            FuckingSessionRepo.detail(code)
                .onSuccess { detail ->
                    _detailState.value = FuckingSessionDetailState.Success(detail)
                    applySources(detail)
                }
                .onFailure {
                    Log.e(TAG, "detail failed", it)
                    _detailState.value =
                        FuckingSessionDetailState.Error(it.message ?: it.javaClass.simpleName)
                }
        }
    }

    private fun pick(sources: List<FuckingSessionSource>): FuckingSessionSource? =
        sources.maxByOrNull { it.height } ?: sources.firstOrNull()

    private fun applySources(detail: FuckingSessionDetail) {
        val chosen = pick(detail.sources)
        if (chosen == null) {
            _streamState.value =
                FuckingSessionStreamState.Failed(applicationContext.getString(R.string.fuckingsession_err_no_link))
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
        _streamState.value = FuckingSessionStreamState.Success
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
