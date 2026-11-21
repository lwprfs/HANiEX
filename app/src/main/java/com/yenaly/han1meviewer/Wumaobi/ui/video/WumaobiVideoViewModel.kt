package com.yenaly.han1meviewer.Wumaobi.ui.video

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.Wumaobi.common.WumaobiConstants
import com.yenaly.han1meviewer.Wumaobi.common.WumaobiSettings
import com.yenaly.han1meviewer.Wumaobi.data.model.WumaobiDetail
import com.yenaly.han1meviewer.Wumaobi.data.model.WumaobiSource
import com.yenaly.han1meviewer.Wumaobi.data.remote.WumaobiRepo
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.player.SitePlayerState
import com.yenaly.han1meviewer.util.applicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

typealias WumaobiPlayerState = SitePlayerState

sealed interface WumaobiDetailState {
    data object Loading : WumaobiDetailState
    data class Success(val detail: WumaobiDetail) : WumaobiDetailState
    data class Error(val message: String) : WumaobiDetailState
}

sealed interface WumaobiStreamState {
    data object Idle : WumaobiStreamState
    data class Failed(val reason: String) : WumaobiStreamState
    data object Success : WumaobiStreamState
}

class WumaobiVideoViewModel : ViewModel() {

    private companion object {
        const val TAG = "WumaobiVideoVM"
    }

    private val _detailState = MutableStateFlow<WumaobiDetailState>(WumaobiDetailState.Loading)
    val detailState: StateFlow<WumaobiDetailState> = _detailState.asStateFlow()

    private val _playerState = MutableStateFlow(WumaobiPlayerState())
    val playerState: StateFlow<WumaobiPlayerState> = _playerState.asStateFlow()

    private val _streamState = MutableStateFlow<WumaobiStreamState>(WumaobiStreamState.Idle)
    val streamState: StateFlow<WumaobiStreamState> = _streamState.asStateFlow()

    val referer: StateFlow<String> = MutableStateFlow("").asStateFlow()

    private val _favMessage = MutableStateFlow<String?>(null)
    val favMessage: StateFlow<String?> = _favMessage.asStateFlow()

    fun consumeFavMessage() {
        _favMessage.value = null
    }

    /** Pushes the chosen favorite collections for this video and mirrors them in the UI state. */
    fun saveFavorites(code: String, collectionIds: List<String>) {
        viewModelScope.launch {
            WumaobiRepo.editFav(code, collectionIds)
                .onSuccess {
                    (_detailState.value as? WumaobiDetailState.Success)?.let { current ->
                        val d = current.detail
                        _detailState.value = WumaobiDetailState.Success(
                            d.copy(
                                favCollections = d.favCollections.map { c ->
                                    c.copy(checked = c.id in collectionIds)
                                },
                            ),
                        )
                    }
                    _favMessage.value = applicationContext.getString(R.string.wumaobi_fav_saved)
                }
                .onFailure {
                    _favMessage.value = it.message ?: it.javaClass.simpleName
                }
        }
    }

    private var currentCode: String = ""
    private var loadJob: Job? = null

    fun load(code: String, force: Boolean = false) {
        if (!force && currentCode == code && _detailState.value is WumaobiDetailState.Success) return
        currentCode = code
        loadJob?.cancel()
        _detailState.value = WumaobiDetailState.Loading
        _streamState.value = WumaobiStreamState.Idle
        _playerState.update {
            it.copy(url = "", quality = "", qualityOptions = emptyMap(), isReady = false, isPlaying = false)
        }
        loadJob = viewModelScope.launch {
            WumaobiRepo.detail(code)
                .onSuccess { detail ->
                    _detailState.value = WumaobiDetailState.Success(detail)
                    applySources(detail)
                }
                .onFailure {
                    Log.e(TAG, "detail failed", it)
                    _detailState.value =
                        WumaobiDetailState.Error(it.message ?: it.javaClass.simpleName)
                }
        }
    }

    private fun pick(sources: List<WumaobiSource>): WumaobiSource? {
        val wantHigh = WumaobiSettings.preferredQuality == "High"
        return sources.firstOrNull { it.hq == wantHigh } ?: sources.firstOrNull()
    }

    private fun applySources(detail: WumaobiDetail) {
        val chosen = pick(detail.sources)
        if (chosen == null) {
            _streamState.value =
                WumaobiStreamState.Failed(applicationContext.getString(R.string.wumaobi_err_no_link))
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
        _streamState.value = WumaobiStreamState.Success
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
