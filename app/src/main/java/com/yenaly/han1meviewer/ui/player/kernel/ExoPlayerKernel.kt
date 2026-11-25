package com.yenaly.han1meviewer.ui.player.kernel

import com.yenaly.han1meviewer.util.logHost

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.yenaly.han1meviewer.worker.PlaybackPriority
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@OptIn(UnstableApi::class)
class ExoPlayerKernel(
    private val context: Context,
) : PlayerKernel {

    companion object {
        private const val TAG = "ExoPlayerKernel"

        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 20_000
    }

    override val type = KernelType.ExoPlayer

    override var listener: KernelListener? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    private var mediaHandler: Handler? = null
    private var mediaHandlerThread: HandlerThread? = null

    @Volatile
    private var pendingSurface: Surface? = null

    @Volatile
    private var pendingPrepare: MediaRequest? = null

    @Volatile
    private var _exoPlayer: ExoPlayer? = null

    @Volatile
    private var released = false

    private var prevSeek = 0L

    @Volatile
    private var networkSource = false

    private var prepareStartMs = 0L
    private var firstReadyMs = 0L
    private var firstFrameMs = 0L
    private var rebufferCount = 0
    private var rebufferTotalMs = 0L
    private var rebufferStartMs = 0L

    @Volatile
    override var videoWidth: Int = 0
        private set

    @Volatile
    override var videoHeight: Int = 0
        private set

    private val exoListener = object : Player.Listener {

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            val realWidth = videoSize.width * videoSize.pixelWidthHeightRatio
            val realHeight = videoSize.height
            videoWidth = realWidth.toInt()
            videoHeight = realHeight
            Log.i(TAG, "onVideoSizeChanged: realWidth=$realWidth, realHeight=$realHeight")
            mainHandler.post { listener?.onVideoSizeChanged(realWidth.toInt(), realHeight) }
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            pushBufferedPercent()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            Log.i(TAG, "onPlaybackStateChanged: $playbackState")
            pushBufferedPercent()
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    if (firstReadyMs != 0L && rebufferStartMs == 0L && _exoPlayer?.playWhenReady == true) {
                        rebufferCount++
                        rebufferStartMs = SystemClock.elapsedRealtime()
                    }
                    if (networkSource && _exoPlayer?.playWhenReady == true) PlaybackPriority.onBuffering()
                    mainHandler.post { listener?.onBuffering() }
                }

                Player.STATE_READY -> {
                    Log.i(TAG, "STATE_READY reached")
                    if (firstReadyMs == 0L) firstReadyMs = SystemClock.elapsedRealtime() - prepareStartMs
                    if (rebufferStartMs != 0L) {
                        rebufferTotalMs += SystemClock.elapsedRealtime() - rebufferStartMs
                        rebufferStartMs = 0L
                    }
                    PlaybackPriority.onStable()
                    runOnPlayerThread {
                        val pwr = _exoPlayer?.playWhenReady
                        Log.i(TAG, "STATE_READY playWhenReady=$pwr")
                        if (pwr == true) {
                            mainHandler.post {
                                Log.i(TAG, "notifying onPrepared+onPlaying")
                                listener?.onPrepared()
                                listener?.onPlaying()
                            }
                        } else {

                            mainHandler.post {
                                listener?.onPrepared()
                                listener?.onPaused()
                            }
                        }
                    }
                }

                Player.STATE_ENDED -> {
                    PlaybackPriority.onStopped()
                    mainHandler.post { listener?.onCompleted() }
                }

                Player.STATE_IDLE -> PlaybackPriority.onStopped()
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            Log.i(
                TAG,
                "onPlayWhenReadyChanged: $playWhenReady, reason=$reason, " +
                        "playbackState=${_exoPlayer?.playbackState}"
            )
            if (_exoPlayer?.playbackState == Player.STATE_READY) {
                if (playWhenReady) {
                    mainHandler.post { listener?.onPlaying() }
                } else {
                    mainHandler.post { listener?.onPaused() }
                }
            }
        }

        override fun onRenderedFirstFrame() {
            if (firstFrameMs == 0L) {
                firstFrameMs = SystemClock.elapsedRealtime() - prepareStartMs
                Log.i(TAG, "time to first frame=${firstFrameMs}ms")
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            PlaybackPriority.onStopped()
            Log.e(TAG, "onPlayerError: $error")
            mainHandler.post { listener?.onError(error) }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                runOnPlayerThread {
                    val playing = _exoPlayer?.playWhenReady == true
                    mainHandler.post {
                        listener?.onSeekComplete()
                        if (playing) {
                            listener?.onPlaying()
                        } else {

                            listener?.onPaused()
                        }
                    }
                }
            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) = Unit
    }

    private fun pushBufferedPercent() {
        val per = _exoPlayer?.bufferedPercentage ?: return
        mainHandler.post { listener?.onBufferedUpdate(per) }
    }

    override fun prepare(request: MediaRequest) {

        val surfaceToKeep = pendingSurface
        val listenerToKeep = listener
        if (surfaceToKeep == null) {

            pendingPrepare = request
            released = false
            return
        }
        pendingPrepare = null
        release()
        pendingSurface = surfaceToKeep
        listener = listenerToKeep
        released = false
        Log.i(TAG, "prepare: ${request.url.logHost()}")
        val thread = HandlerThread("ExoPlayerKernel").also { it.start() }
        val handler = Handler(thread.looper)
        mediaHandlerThread = thread
        mediaHandler = handler
        handler.post {
            if (released) {
                thread.quitSafely()
                return@post
            }
            val player = buildPlayer(request)
            if (released) {

                runCatching { player.release() }
                thread.quitSafely()
                return@post
            }
            _exoPlayer = player
            pendingSurface?.let { player.setVideoSurface(it) }
            pendingSurface = null
        }
    }

    private fun buildPlayer(request: MediaRequest): ExoPlayer {

        prepareStartMs = SystemClock.elapsedRealtime()
        firstReadyMs = 0L
        firstFrameMs = 0L
        rebufferCount = 0
        rebufferTotalMs = 0L
        rebufferStartMs = 0L
        networkSource = request.url.startsWith("http", ignoreCase = true)

        val trackSelector = DefaultTrackSelector(
            context,
            AdaptiveTrackSelection.Factory(
                 10_000,
                 15_000,
                 25_000,
                 0.75f,
            ),
        )
        val renderersFactory = DefaultRenderersFactory(context)
        val player = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(PlayerTuning.createLoadControl(context))
            .setBandwidthMeter(PlayerTuning.bandwidthMeter(context))
            .build()
        player.addListener(exoListener)

        val dataSourceFactory = DefaultDataSource.Factory(
            context,
            DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(request.headers)
                .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
                .setReadTimeoutMs(READ_TIMEOUT_MS)
                .setAllowCrossProtocolRedirects(true),
        )
        val url = request.url
        Log.i(TAG, "URL Link = ${url.logHost()}")
        val mediaSource = if (url.contains(".m3u8")) {
            HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(url))
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(url))
        }

        player.repeatMode =
            if (request.looping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.setMediaSource(mediaSource)
        if (request.startPositionMs > 0) {
            player.seekTo(request.startPositionMs)
        }
        if (request.speed != 1.0f) {
            player.playbackParameters = PlaybackParameters(request.speed, 1.0f)
        }
        player.prepare()

        player.playWhenReady = !request.startPaused
        Log.i(TAG, "buildPlayer done, pendingSurface=$pendingSurface")
        mainHandler.post { listener?.onPreparing() }
        return player
    }

    override fun start() {
        mediaHandler?.post {
            _exoPlayer?.playWhenReady = true
        }
    }

    override fun pause() {
        mediaHandler?.post {
            _exoPlayer?.playWhenReady = false
        }
    }

    override fun seekTo(positionMs: Long) {
        mediaHandler?.post {
            val player = _exoPlayer ?: return@post
            if (positionMs != prevSeek) {
                if (positionMs >= player.bufferedPosition) {

                    mainHandler.post { listener?.onBuffering() }
                }

                player.seekTo(positionMs)
                prevSeek = positionMs
            }
        }
    }

    override fun setSpeed(speed: Float) {
        mediaHandler?.post {
            _exoPlayer?.playbackParameters = PlaybackParameters(speed, 1.0f)
        }
    }

    override fun setLooping(looping: Boolean) {
        mediaHandler?.post {
            _exoPlayer?.repeatMode =
                if (looping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }
    }

    override fun setSurface(surface: Surface?) {
        Log.i(TAG, "setSurface: $surface")
        pendingSurface = surface
        if (surface != null) {
            val deferred = pendingPrepare
            if (deferred != null) {

                pendingPrepare = null
                prepare(deferred)
                return
            }
        }
        mediaHandler?.post {
            val player = _exoPlayer ?: return@post
            pendingSurface = null
            player.setVideoSurface(surface)
        }
    }

    override fun release() {
        if (prepareStartMs != 0L && (firstReadyMs != 0L || rebufferCount > 0)) {
            Log.i(
                TAG,
                "playback metrics: initialBuffer=${firstReadyMs}ms, firstFrame=${firstFrameMs}ms, " +
                        "rebuffers=$rebufferCount, rebufferTime=${rebufferTotalMs}ms, " +
                        "bandwidth=${PlayerTuning.bandwidthMeter(context).bitrateEstimate / 1000}kbps"
            )
        }
        prepareStartMs = 0L
        PlaybackPriority.onStopped()
        released = true
        pendingSurface = null
        pendingPrepare = null
        listener = null
        val handler = mediaHandler
        val thread = mediaHandlerThread
        val player = _exoPlayer
        mediaHandler = null
        mediaHandlerThread = null
        _exoPlayer = null
        videoWidth = 0
        videoHeight = 0
        if (handler == null || thread == null) {
            player?.release()
            return
        }

        handler.post {
            try {

                player?.release() ?: _exoPlayer?.release()
            } catch (_: Exception) {
            }

            handler.postDelayed({
                handler.removeCallbacksAndMessages(null)
                thread.quitSafely()
            }, 500)
        }
    }

    private inline fun <T> runOnPlayerThread(crossinline block: () -> T?): T? {
        val handler = mediaHandler ?: return null
        if (Looper.myLooper() == handler.looper) {
            return block()
        }

        val result = AtomicReference<T?>()
        val latch = CountDownLatch(1)
        handler.post {
            try {
                result.set(block())
            } finally {
                latch.countDown()
            }
        }
        val finished = latch.await(300, TimeUnit.MILLISECONDS)
        return if (finished) result.get() else null
    }

    override val isPlaying: Boolean
        get() = runOnPlayerThread { _exoPlayer?.playWhenReady } == true

    override val currentPositionMs: Long
        get() {
            val pos = runOnPlayerThread { _exoPlayer?.currentPosition }
            if (pos == null) Log.w(TAG, "currentPositionMs: runOnPlayerThread timed out/null")
            return pos ?: 0L
        }

    override val durationMs: Long
        get() {
            val dur = runOnPlayerThread { _exoPlayer?.duration }
            if (dur == null) Log.w(TAG, "durationMs: runOnPlayerThread timed out/null")
            return dur ?: 0L
        }
}
