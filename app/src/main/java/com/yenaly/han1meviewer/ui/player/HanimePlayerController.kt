package com.yenaly.han1meviewer.ui.player

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.view.Surface
import com.yenaly.han1meviewer.FirebaseConstants
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.ResolutionLinkMap
import com.yenaly.han1meviewer.ui.player.HanimePlayerController.Companion.RESUME_THRESHOLD_MS
import com.yenaly.han1meviewer.ui.player.kernel.KernelListener
import com.yenaly.han1meviewer.ui.player.kernel.KernelType
import com.yenaly.han1meviewer.ui.player.kernel.MediaRequest
import com.yenaly.han1meviewer.ui.player.kernel.PlayerKernel
import com.yenaly.han1meviewer.ui.player.kernel.PlayerKernelFactory
import com.yenaly.han1meviewer.util.HAnalytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class HanimePlayerController(
    context: Context,

    val kernelType: KernelType,
) : KernelListener {

    companion object {
        private const val TAG = "HanimePlayerController"

        private const val RESUME_THRESHOLD_MS = 5_000L

        private const val PROGRESS_INTERVAL_MS = 100L
    }

    private val appContext = context.applicationContext

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var kernel: PlayerKernel = PlayerKernelFactory.create(kernelType, appContext)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _progress = MutableStateFlow(PlayerProgress())
    val progress: StateFlow<PlayerProgress> = _progress.asStateFlow()

    private val _events = MutableSharedFlow<PlayerEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<PlayerEvent> = _events.asSharedFlow()

    private var qualityLinks: List<Pair<String, String>> = emptyList()

    private var savedProgressMs = 0L

    private var hasRestoredProgress = false

    private var wifiTipShowed = false

    private var speedBoostBase = PlayerDefaults.DEF_SPEED

    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Volatile
    private var pausedByAudioFocusLoss = false

    private var audioFocusRequest: AudioFocusRequest? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                -> {
                if (_uiState.value.state == PlayerState.PLAYING) {
                    pausedByAudioFocusLoss = true
                    pause()
                }
            }

            AudioManager.AUDIOFOCUS_GAIN -> {
                if (pausedByAudioFocusLoss) {
                    pausedByAudioFocusLoss = false
                    play()
                }
            }
        }
    }

    init {
        kernel.listener = this

        scope.launch {
            var lastState: PlayerState? = null
            _uiState.collect { ui ->
                if (ui.state != lastState) {
                    Log.i(TAG, "UI state: $lastState -> ${ui.state}")
                    lastState = ui.state
                }
            }
        }
    }

    fun setMedia(
        title: String,
        videoUrls: ResolutionLinkMap,
        savedProgressMs: Long = 0L,
        posterUrl: String? = null,
    ) {
        this.savedProgressMs = savedProgressMs
        hasRestoredProgress = false
        if (Preferences.disableMobileDataWarning) wifiTipShowed = true

        val links = videoUrls.entries.map { it.key to it.value.link }
        qualityLinks = links

        val preferredQuality = Preferences.videoQuality
        val index = links.indexOfFirst { it.first == preferredQuality }
            .takeIf { it >= 0 } ?: 0

        val preferredSpeedIndex = PlayerDefaults.speedArray
            .indexOfFirst { it == Preferences.playerSpeed }
            .takeIf { it >= 0 } ?: PlayerDefaults.DEF_SPEED_INDEX

        _uiState.value = PlayerUiState(
            title = title,
            qualities = links.map { it.first },
            currentQualityIndex = index,
            speedIndex = preferredSpeedIndex,
            posterUrl = posterUrl,
            hasMedia = links.isNotEmpty(),
        )
        _progress.value = PlayerProgress()
        Log.i(
            TAG,
            "setMedia: qualities=${links.map { it.first }}, " +
                    "preferred=$preferredQuality, index=$index, savedProgress=$savedProgressMs"
        )
    }

    fun startVideo() {
        if (!_uiState.value.hasMedia) return
        if (!wifiTipShowed && !isWifiConnected()) {
            _events.tryEmit(PlayerEvent.RequestWifiConfirmation)
            return
        }
        startPlayback()
    }

    fun confirmWifiAndPlay() {
        wifiTipShowed = true
        startPlayback()
    }

    private fun startPlayback() {
        val quality = qualityLinks.getOrNull(_uiState.value.currentQualityIndex) ?: return
        Log.i(TAG, "startPlayback: ${quality.first} -> ${quality.second}")

        HAnalytics.logEvent(
            FirebaseConstants.EVENT_PLAYBACK_START,
            FirebaseConstants.PARAM_QUALITY to quality.first,
            FirebaseConstants.PARAM_KERNEL to kernelType.name,
            FirebaseConstants.PARAM_METHOD to "start",
        )
        _uiState.update { it.copy(state = PlayerState.PREPARING) }
        reprepareKernel(positionMs = 0L)
    }

    fun retry() {
        if (!_uiState.value.hasMedia) return

        HAnalytics.logEvent(
            FirebaseConstants.EVENT_PLAYBACK_START,
            FirebaseConstants.PARAM_QUALITY to (qualityLinks.getOrNull(_uiState.value.currentQualityIndex)?.first),
            FirebaseConstants.PARAM_KERNEL to kernelType.name,
            FirebaseConstants.PARAM_METHOD to "retry",
        )
        _uiState.update { it.copy(state = PlayerState.PREPARING) }
        reprepareKernel(positionMs = 0L)
    }

    fun switchQuality(index: Int) {
        if (!_uiState.value.hasMedia) return
        if (index == _uiState.value.currentQualityIndex) return
        val position = kernel.currentPositionMs

        HAnalytics.logEvent(
            FirebaseConstants.EVENT_VIDEO_QUALITY_SWITCH,
            FirebaseConstants.PARAM_FROM_QUALITY to (qualityLinks.getOrNull(_uiState.value.currentQualityIndex)?.first),
            FirebaseConstants.PARAM_QUALITY to (qualityLinks.getOrNull(index)?.first),
            FirebaseConstants.PARAM_KERNEL to kernelType.name,
        )
        _uiState.update { it.copy(currentQualityIndex = index, state = PlayerState.PREPARING) }
        Log.i(TAG, "switchQuality: ${_uiState.value.qualities.getOrNull(index)} @$position")
        reprepareKernel(positionMs = position)
    }

    private fun reprepareKernel(positionMs: Long) {
        kernel.release()
        kernel = PlayerKernelFactory.create(kernel.type, appContext)
        kernel.listener = this
        currentSurface?.let { kernel.setSurface(it) }
        val state = _uiState.value
        val quality = qualityLinks.getOrNull(state.currentQualityIndex) ?: return
        kernel.prepare(
            MediaRequest(
                url = quality.second,
                title = state.title,
                looping = state.isLooping,
                startPositionMs = positionMs,
                speed = PlayerDefaults.speedArray[state.speedIndex],
            )
        )
    }

    fun release() {
        stopTicker()
        abandonAudioFocus()
        kernel.release()
        currentSurface?.release()
        currentSurface = null
        scope.cancel()
    }

    fun play() {
        requestAudioFocus()
        if (_uiState.value.state == PlayerState.COMPLETED) {
            kernel.seekTo(0L)
        }
        kernel.start()
        _uiState.update { it.copy(state = PlayerState.PLAYING) }
    }

    fun pause() {
        kernel.pause()
        _uiState.update { it.copy(state = PlayerState.PAUSED) }
    }

    fun togglePlayPause() {
        if (_uiState.value.state == PlayerState.PLAYING) pause() else play()
    }

    fun seekTo(positionMs: Long) {
        kernel.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun restartFromBeginning() {
        hasRestoredProgress = true
        play()
        seekTo(0L)
    }

    fun setSpeedIndex(index: Int) {
        val speed = PlayerDefaults.speedArray.getOrNull(index) ?: return
        _uiState.update { it.copy(speedIndex = index) }
        val playing = kernel.isPlaying
        kernel.setSpeed(speed)
        if (!playing) kernel.pause()
    }

    fun beginSpeedBoost() {
        val state = _uiState.value
        speedBoostBase = PlayerDefaults.speedArray[state.speedIndex]
        kernel.setSpeed(speedBoostBase * Preferences.longPressSpeedTime)
    }

    fun endSpeedBoost() {
        kernel.setSpeed(speedBoostBase)
    }

    fun setLooping(looping: Boolean) {
        _uiState.update { it.copy(isLooping = looping) }
        kernel.setLooping(looping)
    }

    fun setSuperResolution(index: Int) {
        _uiState.update { it.copy(superResolutionIndex = index) }
        kernel.setSuperResolution(index)
    }

    fun setGestureLocked(locked: Boolean) {
        _uiState.update { it.copy(isGestureLocked = locked) }
    }

    private var currentSurface: Surface? = null

    fun attachSurfaceTexture(surfaceTexture: SurfaceTexture) {
        currentSurface?.release()
        currentSurface = Surface(surfaceTexture)
        kernel.setSurface(currentSurface)
    }

    fun onSurfaceSizeChanged(width: Int, height: Int) {
        kernel.onSurfaceSizeChanged(width, height)
    }

    override fun onPreparing() = Unit

    override fun onPrepared() = Unit

    override fun onBuffering() {
        _uiState.update { state ->
            if (state.state in BUFFERING_STATES) state.copy(state = PlayerState.BUFFERING) else state
        }
    }

    override fun onPlaying() {
        maybeRestoreWatchProgress()
        _uiState.update { it.copy(state = PlayerState.PLAYING) }
        startTicker()
    }

    override fun onPaused() {
        _uiState.update { state ->
            if (state.state == PlayerState.PLAYING || state.state == PlayerState.BUFFERING) {
                state.copy(state = PlayerState.PAUSED)
            } else {
                state
            }
        }
    }

    override fun onCompleted() {
        stopTicker()
        abandonAudioFocus()
        _uiState.update { it.copy(state = PlayerState.COMPLETED) }
    }

    override fun onSeekComplete() = Unit

    override fun onBufferedUpdate(percent: Int) {
        _progress.update { it.copy(bufferedPercent = percent) }
    }

    override fun onVideoSizeChanged(width: Int, height: Int) {
        _uiState.update { it.copy(videoWidth = width, videoHeight = height) }
    }

    override fun onError(error: Throwable?) {
        stopTicker()
        abandonAudioFocus()
        Log.e(TAG, "onError: $error")

        HAnalytics.logError(
            eventName = FirebaseConstants.EVENT_PLAYER_ERROR,
            source = "player",
            throwable = error,
            FirebaseConstants.PARAM_KERNEL to kernelType.name,
            FirebaseConstants.PARAM_QUALITY to (qualityLinks.getOrNull(_uiState.value.currentQualityIndex)?.first),
        )
        _uiState.update { it.copy(state = PlayerState.ERROR) }
    }

    private fun maybeRestoreWatchProgress() {
        if (hasRestoredProgress || !_uiState.value.hasMedia) return
        if (savedProgressMs <= RESUME_THRESHOLD_MS) return
        if (!Preferences.allowResumePlayback) return
        hasRestoredProgress = true
        kernel.seekTo(savedProgressMs)
        _events.tryEmit(PlayerEvent.ShowResumeButton(savedProgressMs))
    }

    private var tickerJob: Job? = null

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        Log.i(TAG, "startTicker")
        tickerJob = scope.launch {
            while (isActive) {
                _progress.value = PlayerProgress(
                    positionMs = kernel.currentPositionMs,
                    durationMs = kernel.durationMs,
                    bufferedPercent = _progress.value.bufferedPercent,
                )
                delay(PROGRESS_INTERVAL_MS.milliseconds)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun requestAudioFocus() {
        if (audioFocusRequest != null) return
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()
            .also { audioManager.requestAudioFocus(it) }
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
        pausedByAudioFocusLoss = false
    }

    val isPlaying: Boolean
        get() = kernel.isPlaying

    val isActiveForPip: Boolean
        get() = _uiState.value.state == PlayerState.PLAYING

    val currentPositionMs: Long
        get() = kernel.currentPositionMs

    val durationMs: Long
        get() = kernel.durationMs

    val isPortraitVideo: Boolean
        get() = _uiState.value.videoWidth > 0 &&
                _uiState.value.videoHeight > 0 &&
                _uiState.value.videoWidth <= _uiState.value.videoHeight

    private fun isWifiConnected(): Boolean {
        val cm = appContext.getSystemService(ConnectivityManager::class.java) ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}

private val BUFFERING_STATES = setOf(
    PlayerState.PREPARING,
    PlayerState.BUFFERING,
    PlayerState.PLAYING,
    PlayerState.PAUSED,
)
