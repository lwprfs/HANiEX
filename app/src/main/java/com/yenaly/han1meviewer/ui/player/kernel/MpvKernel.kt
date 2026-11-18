package com.yenaly.han1meviewer.ui.player.kernel

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.Surface
import androidx.core.net.toUri
import com.yenaly.han1meviewer.BuildConfig
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.USER_AGENT
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.util.AnimeShaders
import com.yenaly.han1meviewer.util.AnimeShaders.getCert
import `is`.xyz.mpv.MPVLib

class MpvKernel(
    private val context: Context,
) : PlayerKernel {

    companion object {
        private const val TAG = "MpvKernel"

        private const val END_OF_FILE_TOLERANCE_SEC = 2.0
    }

    override val type = KernelType.MpvPlayer

    override var listener: KernelListener? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    private var pendingSurface: Surface? = null

    private var currentSurface: Surface? = null

    private var pendingPath: String? = null
    private var pendingRequest: MediaRequest? = null

    private val certFile = getCert(context)

    @Volatile
    private var mpvTimePos = 0.0

    @Volatile
    private var mpvCacheDuration = 0.0

    @Volatile
    private var mpvDuration = 0.0

    @Volatile
    private var pausedByUser = false

    private val STALL_THRESHOLD_MS = 3000L

    private val STALL_CACHE_EMPTY_SEC = 1.0

    @Volatile
    private var lastTimePosAtMs = 0L

    private val STALL_TO_ERROR_WINDOW_MS = 20_000L
    private val STALL_TO_ERROR_MIN_ADVANCE_SEC = 0.5

    private var stallCheckpointAtMs = 0L

    @Volatile
    private var stallCheckpointPos = 0.0

    private var stallReported = false

    private val stallWatchdog = object : Runnable {
        override fun run() {
            if (!fileActive) return
            val now = android.os.SystemClock.elapsedRealtime()
            when {
                pausedByUser -> {

                    stallReported = false
                    stallCheckpointAtMs = now
                    stallCheckpointPos = mpvTimePos
                }

                now - lastTimePosAtMs > STALL_THRESHOLD_MS && mpvCacheDuration < STALL_CACHE_EMPTY_SEC -> {

                    if (!stallReported) {
                        stallReported = true
                        Log.w(TAG, "playback stalled (no advance, cache empty), report BUFFERING")
                        listener?.onBuffering()
                    }

                    if (now - stallCheckpointAtMs >= STALL_TO_ERROR_WINDOW_MS) {
                        val advanced = mpvTimePos - stallCheckpointPos
                        if (advanced < STALL_TO_ERROR_MIN_ADVANCE_SEC) {
                            fileActive = false
                            Log.e(
                                TAG,
                                "playback dead: only ${advanced}s advanced in " +
                                        "${STALL_TO_ERROR_WINDOW_MS / 1000}s, report ERROR",
                            )
                            stopStallWatchdog()
                            listener?.onError(IllegalStateException("playback stalled"))
                            return
                        }
                        stallCheckpointAtMs = now
                        stallCheckpointPos = mpvTimePos
                    }
                }

                stallReported -> {
                    stallReported = false
                    stallCheckpointAtMs = now
                    stallCheckpointPos = mpvTimePos
                    Log.i(TAG, "playback resumed after stall")
                    listener?.onPlaying()
                }
            }
            mainHandler.postDelayed(this, 1000)
        }
    }

    private fun startStallWatchdog() {
        mainHandler.removeCallbacks(stallWatchdog)
        stallReported = false
        val now = android.os.SystemClock.elapsedRealtime()
        lastTimePosAtMs = now
        stallCheckpointAtMs = now
        stallCheckpointPos = mpvTimePos
        mainHandler.postDelayed(stallWatchdog, 1000)
    }

    private fun stopStallWatchdog() {
        mainHandler.removeCallbacks(stallWatchdog)
    }

    private val prepareTimeout = Runnable {
        if (pendingLoad) {
            pendingLoad = false
            Log.e(TAG, "prepare timeout, report ERROR")
            listener?.onError(IllegalStateException("prepare timeout"))
        }
    }

    private fun schedulePrepareTimeout() {
        cancelPrepareTimeout()
        val timeoutMs = Preferences.mpvNetworkTimeout.coerceAtLeast(10) * 1000L + 15_000L
        mainHandler.postDelayed(prepareTimeout, timeoutMs)
    }

    private fun cancelPrepareTimeout() {
        mainHandler.removeCallbacks(prepareTimeout)
    }

    private var currentPfd: ParcelFileDescriptor? = null
    private var detachFd: Int? = null
    private var pfdFilePath = false

    private var reportedVideoWidth = 0
    private var reportedVideoHeight = 0
    private var lastReportedSizeW = 0
    private var lastReportedSizeH = 0

    @Volatile
    private var pendingLoad = false

    @Volatile
    private var fileActive = false

    override val videoWidth: Int
        get() = MPVLib.getPropertyInt("video-params/w") ?: 0

    override val videoHeight: Int
        get() = MPVLib.getPropertyInt("video-params/h") ?: 0

    private val mpvOptions: Map<String, String>
        get() = buildMap {

            if (Preferences.enableGPUNextRenderer) {
                put("vo", "gpu-next")
            } else {
                put("vo", "gpu")
            }

            put("profile", when (Preferences.mpvProfile) {
                "gpu-hq" -> "gpu-hq"
                "fast" -> "fast"
                else -> "default"
            })

            put("hwdec", when (Preferences.mpvHwdec) {
                "Auto" -> "auto"
                "HW" -> "mediacodec-copy"
                "HW+" -> "mediacodec"
                "Vulkan" -> "vulkan-copy"
                "vulkan+" -> "vulkan"
                "SW" -> "no"
                else -> "auto"
            })

            put("msg-level", "all=" + if (BuildConfig.DEBUG) "debug" else "warn")

            if (Preferences.mpvInterpolation) {
                put("interpolation", "yes")
                put("tscale", "oversample")
                put("video-sync", "display-resample")
            }

            put("cache", "yes")
            put("cache-secs", Preferences.mpvCacheSecs.toString())
            put("vd-lavc-threads", Runtime.getRuntime().availableProcessors().toString())
            if (Preferences.mpvFramedrop) {
                put("framedrop", "vo")
            } else {
                put("framedrop", "no")
            }

            put("deband", if (Preferences.mpvDeband) "yes" else "no")
            put("cache-pause", "no")
            put("network-timeout", Preferences.mpvNetworkTimeout.toString())

            put("tls-ca-file", certFile)
            put("tls-verify", if (Preferences.mpvTlsVerify) "no" else "yes")

            val proxyIp = Preferences.proxyIp
            val proxyPort = Preferences.proxyPort
            if (proxyIp.isNotBlank() && proxyPort != -1) {
                val proxyUrl = "$proxyIp:$proxyPort"
                if (Preferences.proxyType == HProxySelector.TYPE_HTTP) {
                    put("http-proxy", "http://$proxyUrl")
                }
            }
            put("user-agent", USER_AGENT)
        }

    private val observedProperties = listOf(
        "time-pos" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
        "duration" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
        "pause" to MPVLib.mpvFormat.MPV_FORMAT_FLAG,
        "playback-active" to MPVLib.mpvFormat.MPV_FORMAT_FLAG,
        "video-params/w" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
        "video-params/h" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
        "demuxer-cache-duration" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
        "paused-for-cache" to MPVLib.mpvFormat.MPV_FORMAT_FLAG,
    )

    private fun parseCustomMpvParams(): LinkedHashMap<String, String> {
        val map = linkedMapOf<String, String>()
        Preferences.customMpvParams.split(";").forEach { entry ->
            val trimmedEntry = entry.trim()
            if (trimmedEntry.isEmpty() || trimmedEntry.startsWith("#")) return@forEach
            val parts = trimmedEntry.split(",", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim()
                val value = parts[1].trim()
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    map[key] = value
                }
            }
        }
        return map
    }

    private fun init() {
        mpvOptions.forEach { (key, value) ->
            Log.i("MPV_config", "$key,$value")
            MPVLib.setOptionString(key, value)
        }
        try {
            parseCustomMpvParams().forEach { (key, value) ->
                Log.i("MPV_config", "custom: $key,$value")
                MPVLib.setOptionString(key, value)
            }
        } catch (e: Exception) {
            GlobalToasts.show(e.message.orEmpty(), level = GlobalToasts.ToastLevel.ERROR)
        }
        observedProperties.forEach { (name, type) ->
            MPVLib.observeProperty(name, type)
        }
        MPVLib.addObserver(mpvEventObserver)
    }

    private fun prepareUri(uri: Uri): String? {
        return when (uri.scheme) {
            "http", "https" -> uri.toString()

            "file", "content" -> {
                try {
                    currentPfd = context.contentResolver.openFileDescriptor(uri, "r")
                    detachFd = currentPfd?.detachFd()
                    Log.d(TAG, "Detached FD = $detachFd")
                    pfdFilePath = true
                    if (detachFd != null) "fd://$detachFd" else null
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            else -> null
        }
    }

    private fun releaseCurrentPfd(from: String? = null) {
        if (!pfdFilePath) return
        try {
            currentPfd?.let {
                Log.i(TAG, "Closing currentPfd: $it")
                it.close()
            }
            detachFd?.let { fd ->
                Log.i(TAG, "Closing detachFd: $fd")
                try {
                    ParcelFileDescriptor.adoptFd(fd).close()
                } catch (e: Exception) {
                    Log.w(TAG, "detachFd $fd already closed or invalid", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing PFD", e)
        } finally {
            currentPfd = null
            detachFd = null
            mainHandler.postDelayed({
                Log.i(TAG, "${from ?: "releaseCurrentPfd"} completed. PFD cleared.")
            }, 200)
        }
    }

    override fun prepare(request: MediaRequest) {
        init()
        if (request.url.isEmpty()) {
            Log.e(TAG, "视频链接为空")
            return
        }
        Log.i(TAG, "URL Link = ${request.url}")
        MPVLib.setOptionString("force-window", "yes")

        val path = prepareUri(request.url.toUri()) ?: return
        if (currentSurface != null) {
            loadFile(path, request)
        } else {

            pendingPath = path
            pendingRequest = request
        }
        listener?.onPreparing()
    }

    private fun loadFile(path: String, request: MediaRequest) {

        MPVLib.setPropertyBoolean("pause", false)
        if (request.startPositionMs > 0) {

            MPVLib.setOptionString("start", (request.startPositionMs / 1000.0).toString())
        }
        MPVLib.setPropertyDouble("speed", request.speed.toDouble())
        schedulePrepareTimeout()
        MPVLib.command(arrayOf("loadfile", path))
        if (request.looping) setLooping(true)
    }

    override fun start() {
        MPVLib.setPropertyBoolean("pause", false)
    }

    override fun pause() {
        MPVLib.setPropertyBoolean("pause", true)
    }

    override fun seekTo(positionMs: Long) {
        Log.i(TAG, "seekTo: ${positionMs / 1000.0}")
        MPVLib.command(arrayOf("seek", (positionMs / 1000.0).toString(), "absolute", "exact"))
    }

    override fun setSpeed(speed: Float) {
        MPVLib.setPropertyDouble("speed", speed.toDouble())
    }

    override fun setLooping(looping: Boolean) {
        MPVLib.setPropertyString("loop-file", if (looping) "inf" else "no")
    }

    override fun setSurface(surface: Surface?) {
        currentSurface = surface
        if (surface != null) {
            MPVLib.attachSurface(surface)

            val path = pendingPath
            val request = pendingRequest
            if (path != null && request != null) {
                pendingPath = null
                pendingRequest = null
                loadFile(path, request)
            }
        } else {
            MPVLib.detachSurface()
        }
    }

    override fun onSurfaceSizeChanged(width: Int, height: Int) {
        Log.d(TAG, "onSurfaceSizeChanged ${width}x$height")
        MPVLib.setPropertyString("android-surface-size", "${width}x$height")
    }

    override val isPlaying: Boolean

        get() = MPVLib.getPropertyBoolean("pause") == false

    override val currentPositionMs: Long
        get() = ((MPVLib.getPropertyDouble("time-pos") ?: 0.0).toLong()) * 1000

    override val durationMs: Long
        get() = ((MPVLib.getPropertyDouble("duration") ?: 0.0).toLong()) * 1000

    private fun clearSuperResolution() {
        MPVLib.command(arrayOf("change-list", "glsl-shaders", "clr", ""))
    }

    override fun setSuperResolution(index: Int) {
        if (index != 0) {
            val cmd = arrayOf(
                "change-list", "glsl-shaders", "set",
                AnimeShaders.getShader(context, index),
            )
            MPVLib.command(cmd)
        } else {
            clearSuperResolution()
        }
    }

    override fun release() {
        stopStallWatchdog()
        cancelPrepareTimeout()
        fileActive = false
        pendingLoad = false
        clearSuperResolution()
        MPVLib.setPropertyBoolean("pause", true)
        MPVLib.command(arrayOf("loadfile", "", "replace"))
        MPVLib.setPropertyString("vo", "null")
        MPVLib.setOptionString("force-window", "no")
        MPVLib.detachSurface()
        MPVLib.removeObserver(mpvEventObserver)
        mainHandler.postDelayed({ releaseCurrentPfd("mpv-release") }, 200)
        currentSurface = null
        pendingSurface = null
        pendingPath = null
        pendingRequest = null
        listener = null
    }

    private val mpvEventObserver = object : MPVLib.EventObserver {

        override fun eventProperty(property: String) = Unit

        override fun eventProperty(property: String, value: Long) {

            when (property) {
                "video-params/w" -> reportedVideoWidth = value.toInt()
                "video-params/h" -> reportedVideoHeight = value.toInt()
            }
            val w = reportedVideoWidth
            val h = reportedVideoHeight
            if (w > 0 && h > 0 && (w != lastReportedSizeW || h != lastReportedSizeH)) {
                lastReportedSizeW = w
                lastReportedSizeH = h
                mainHandler.post { listener?.onVideoSizeChanged(w, h) }
            }
        }

        override fun eventProperty(property: String, value: Boolean) {
            when (property) {

                "pause" -> pausedByUser = value

                "paused-for-cache" -> mainHandler.post {
                    if (value) listener?.onBuffering() else listener?.onPlaying()
                }
            }
        }

        override fun eventProperty(property: String, value: String) = Unit

        override fun eventProperty(property: String, value: `is`.xyz.mpv.MPVNode) = Unit

        override fun eventProperty(property: String, value: Double) {
            when (property) {
                "time-pos" -> {
                    mpvTimePos = value

                    lastTimePosAtMs = android.os.SystemClock.elapsedRealtime()
                }
                "demuxer-cache-duration" -> mpvCacheDuration = value
                "duration" -> mpvDuration = value
            }
            if (mpvDuration > 0) {
                val bufferedTime = (mpvTimePos + mpvCacheDuration).coerceAtMost(mpvDuration)
                val bufferPercent = ((bufferedTime / mpvDuration) * 100).toInt()
                mainHandler.post { listener?.onBufferedUpdate(bufferPercent) }
            }
        }

        override fun event(eventId: Int) {
            mainHandler.post {
                when (eventId) {
                    MPVLib.mpvEventId.MPV_EVENT_START_FILE -> {

                        mpvTimePos = 0.0
                        mpvCacheDuration = 0.0
                        mpvDuration = 0.0
                        pausedByUser = false
                        pendingLoad = true
                        fileActive = false
                        stopStallWatchdog()
                        listener?.onPreparing()
                    }

                    MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED -> {

                        pendingLoad = false
                        fileActive = true
                        cancelPrepareTimeout()
                        startStallWatchdog()
                        listener?.onPrepared()
                    }

                    MPVLib.mpvEventId.MPV_EVENT_PLAYBACK_RESTART -> {

                        listener?.onPlaying()
                    }

                    MPVLib.mpvEventId.MPV_EVENT_END_FILE -> {
                        stopStallWatchdog()
                        cancelPrepareTimeout()
                        when {

                            pendingLoad -> {
                                pendingLoad = false
                                releaseCurrentPfd("MPV_EVENT_END_FILE(error)")
                                listener?.onError(null)
                            }

                            fileActive -> {
                                fileActive = false

                                val reachedEnd = mpvDuration <= 0.0 ||
                                        mpvTimePos >= mpvDuration - END_OF_FILE_TOLERANCE_SEC
                                if (reachedEnd) {
                                    releaseCurrentPfd("MPV_EVENT_END_FILE")
                                    listener?.onCompleted()
                                } else {
                                    Log.e(
                                        TAG,
                                        "stream ended at ${mpvTimePos}s / ${mpvDuration}s, treat as error",
                                    )
                                    releaseCurrentPfd("MPV_EVENT_END_FILE(interrupted)")
                                    listener?.onError(null)
                                }
                            }

                            else -> Log.i(TAG, "ignore stale END_FILE (no active load)")
                        }
                    }

                    MPVLib.mpvEventId.MPV_EVENT_SHUTDOWN -> {
                        Log.e(TAG, "event: $eventId")
                    }
                }
            }
        }
    }
}
