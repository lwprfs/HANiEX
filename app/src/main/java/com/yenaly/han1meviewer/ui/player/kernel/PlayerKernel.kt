package com.yenaly.han1meviewer.ui.player.kernel

import android.content.Context
import android.view.Surface

enum class KernelType {
    ExoPlayer,
    MpvPlayer;

    companion object {

        fun fromString(name: String?): KernelType = when (name) {
            MpvPlayer.name -> MpvPlayer
            else -> ExoPlayer
        }
    }
}

data class MediaRequest(
    val url: String,
    val title: String = "",
    val headers: Map<String, String> = emptyMap(),
    val looping: Boolean = false,
    val startPositionMs: Long = 0L,
    val speed: Float = 1.0f,
)

interface KernelListener {

    fun onPreparing() {}

    fun onPrepared() {}

    fun onBuffering() {}

    fun onPlaying() {}

    fun onPaused() {}

    fun onCompleted() {}

    fun onSeekComplete() {}

    fun onBufferedUpdate(percent: Int) {}

    fun onVideoSizeChanged(width: Int, height: Int) {}

    fun onError(error: Throwable? = null) {}
}

interface PlayerKernel {

    val type: KernelType

    var listener: KernelListener?

    val videoWidth: Int

    val videoHeight: Int

    val isPlaying: Boolean

    val currentPositionMs: Long

    val durationMs: Long

    fun prepare(request: MediaRequest)

    fun start()

    fun pause()

    fun seekTo(positionMs: Long)

    fun setSpeed(speed: Float)

    fun setLooping(looping: Boolean)

    fun setSurface(surface: Surface?)

    fun onSurfaceSizeChanged(width: Int, height: Int) {}

    fun setSuperResolution(index: Int) {}

    fun release()
}

object PlayerKernelFactory {

    fun create(type: KernelType, context: Context): PlayerKernel {
        return when (type) {
            KernelType.ExoPlayer -> ExoPlayerKernel(context.applicationContext)
            KernelType.MpvPlayer -> MpvKernel(context.applicationContext)
        }
    }
}
