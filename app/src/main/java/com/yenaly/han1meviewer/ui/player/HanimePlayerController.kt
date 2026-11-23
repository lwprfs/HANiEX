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
import kotlinx.coroutines.withContext
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

    /** 预加载的媒体地址（[preload] 后非空；换源/起播/出错时清除）。 */
    private var preloadedUrl: String? = null

    /** 预加载的内核是否已就绪（可被 startVideo 直接续用）。 */
    private var preloadedReady = false

    // ---------------- 音频焦点 ----------------

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
        preloadedUrl = null
        preloadedReady = false
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

    /**
     * 预加载当前清晰度（URL 就绪即调用）：暂停态构建内核并预缓冲，不起播。
     *
     * UI 保持 IDLE（海报 + 播放键）；就绪后回填时长，startVideo 命中时
     * 直接续用该内核秒开。非 WiFi 未确认流量时跳过；失败静默放弃。
     */
    fun preload() {
        val state = _uiState.value
        if (!state.hasMedia || state.state != PlayerState.IDLE) return
        if (preloadedUrl != null) return
        if (!wifiTipShowed && !isWifiConnected()) return
        val url = qualityLinks.getOrNull(state.currentQualityIndex)?.second ?: return
        Log.i(TAG, "preload: $url")
        // 不带历史位置起播：时长未知无法判别越界进度，就绪后再按钳制规则预定位
        reprepareKernel(positionMs = 0L, startPaused = true)
        // reprepareKernel 会清空预加载标记，这里在其后标记
        preloadedUrl = url
    }

    private fun startPlayback() {
        val quality = qualityLinks.getOrNull(_uiState.value.currentQualityIndex) ?: return
        Log.i(TAG, "startPlayback: ${quality.first}")

        HAnalytics.logEvent(
            FirebaseConstants.EVENT_PLAYBACK_START,
            FirebaseConstants.PARAM_QUALITY to quality.first,
            FirebaseConstants.PARAM_KERNEL to kernelType.name,
            FirebaseConstants.PARAM_METHOD to "start",
        )
        if (preloadedUrl == quality.second && preloadedReady) {
            // 命中预加载：续用已就绪的内核（保留已缓冲数据）
            preloadedUrl = null
            preloadedReady = false
            requestAudioFocus()
            _uiState.update { it.copy(state = PlayerState.BUFFERING) }
            kernel.start()
            return
        }
        preloadedUrl = null
        preloadedReady = false
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
        Log.i(TAG, "switchQuality: index=$index @$position")
        reprepareKernel(positionMs = position)
    }

    /**
     * 释放并按当前状态重建内核，随后以给定位置准备媒体。
     *
     * 重建后先把当前 surface 挂回新内核（MPV 必须在 loadfile 前 attach surface，
     * 否则 vo 初始化会原生崩溃）。
     *
     * @param startPaused true 表示加载完成后保持暂停（预加载用），默认起播
     */
    private fun reprepareKernel(positionMs: Long, startPaused: Boolean = false) {
        preloadedUrl = null
        preloadedReady = false
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
                startPaused = startPaused,
            )
        )
    }

    fun release() {
        preloadedUrl = null
        preloadedReady = false
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
            if (kernel.type == KernelType.MpvPlayer) {
                // mpv 播完（EOF）后文件已卸载、fd 已释放，seek 回 0 不会
                // 重新起播，必须重新加载（循环模式不触发 END_FILE 故无此问题）
                _uiState.update { it.copy(state = PlayerState.PREPARING) }
                reprepareKernel(positionMs = 0L)
                return
            }
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

    override fun onPrepared() {
        if (preloadedUrl != null) {
            // 预加载就绪：标记可续用，回填时长并按钳制规则预定位历史进度
            preloadedReady = true
            onPreloadReady()
        }
    }

    /**
     * 预加载就绪（后台等待时长就绪）：回填时长，历史进度可安全续播时在
     * 暂停态预 seek（起播即位于续播位置）。时长未知不预定位，交给
     * [maybeRestoreWatchProgress] 的守卫兜底。
     */
    private fun onPreloadReady() {
        scope.launch(Dispatchers.Default) {
            val duration = awaitKernelDuration()
            if (duration > 0) {
                _uiState.update { ui ->
                    if (ui.durationMs != duration) ui.copy(durationMs = duration) else ui
                }
            }
            val resumeTo = savedProgressMs
            if (!Preferences.allowResumePlayback || !isResumableProgress(resumeTo, duration)) {
                return@launch
            }
            withContext(Dispatchers.Main) {
                // 仍处于预加载暂停态才预定位；onPrepared 可能触发两次
                // （mpv 的 FILE_LOADED 与暂停态 RESTART 各一次），防重入
                if (preloadedUrl != null && _uiState.value.state == PlayerState.IDLE &&
                    !hasRestoredProgress
                ) {
                    hasRestoredProgress = true
                    kernel.seekTo(resumeTo)
                    _events.tryEmit(PlayerEvent.ShowResumeButton(resumeTo))
                    Log.i(TAG, "preload pre-seek to ${resumeTo}ms (duration=${duration}ms)")
                }
            }
        }
    }

    /** 等待内核总时长就绪（就绪时机不定，ExoPlayer 同步查询带超时，故在 Default 线程轮询）。 */
    private suspend fun awaitKernelDuration(): Long {
        for (i in 0 until 25) {
            val duration = kernel.durationMs
            if (duration > 0) return duration
            delay(300.milliseconds)
        }
        return 0L
    }

    /**
     * 历史进度是否可安全续播：距结尾须多于 [RESUME_THRESHOLD_MS]——
     * 到达/超过结尾的进度可能已被越界 seek 污染，续播会"打开即播完"。
     */
    private fun isResumableProgress(progressMs: Long, durationMs: Long): Boolean {
        return progressMs > RESUME_THRESHOLD_MS &&
                durationMs > 0 &&
                progressMs < durationMs - RESUME_THRESHOLD_MS
    }

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
        if (preloadedUrl != null && _uiState.value.state == PlayerState.IDLE) {
            // 预加载失败：静默放弃，用户点播放时走正常起播
            Log.w(TAG, "preload failed, ignore: $error")
            preloadedUrl = null
            preloadedReady = false
            return
        }
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
        // 进度越界（见 isResumableProgress）不续播，从头开始
        val duration = kernel.durationMs
        if (duration > 0 && !isResumableProgress(savedProgressMs, duration)) {
            hasRestoredProgress = true
            Log.w(
                TAG,
                "saved progress ${savedProgressMs}ms out of range (duration=${duration}ms), skip resume",
            )
            return
        }
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
