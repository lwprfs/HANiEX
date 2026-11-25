package com.yenaly.han1meviewer.ui.player.kernel

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.yenaly.han1meviewer.worker.PlaybackPriority

@OptIn(UnstableApi::class)
object PlayerTuning {

    private const val TAG = "PlayerTuning"
    private const val MIB = 1024 * 1024

    private const val SLOW_BPS = 3_000_000L
    private const val FAST_BPS = 25_000_000L

    fun bandwidthMeter(context: Context): DefaultBandwidthMeter =
        DefaultBandwidthMeter.getSingletonInstance(context.applicationContext)

    fun createLoadControl(context: Context): LoadControl {
        val app = context.applicationContext
        val am = app.getSystemService(ActivityManager::class.java)
        val memoryClassMb = am?.memoryClass?.takeIf { it > 0 } ?: 256
        val lowRam = am?.isLowRamDevice == true

        val (targetBufferBytes, baseMinMs, baseMaxMs) = when {
            lowRam || memoryClassMb <= 128 -> Triple(24 * MIB, 8_000, 15_000)
            memoryClassMb <= 256 -> Triple(48 * MIB, 10_000, 20_000)
            memoryClassMb <= 384 -> Triple(64 * MIB, 15_000, 30_000)
            else -> Triple(96 * MIB, 20_000, 40_000)
        }

        val bps = bandwidthMeter(app).bitrateEstimate
        var minMs = baseMinMs
        var maxMs = baseMaxMs
        var startMs = 1_000
        var afterRebufferMs = 2_000
        when {
            bps in 1 until SLOW_BPS -> {
                minMs += 5_000
                maxMs += 10_000
                startMs = 1_500
                afterRebufferMs = 4_000
            }
            bps >= FAST_BPS -> {
                minMs = minOf(minMs, 10_000)
                maxMs = minOf(maxMs, 25_000)
                startMs = 800
            }
        }
        if (maxMs < minMs) maxMs = minMs
        Log.i(
            TAG,
            "memoryClass=${memoryClassMb}MB lowRam=$lowRam bandwidth=${bps / 1000}kbps " +
                    "target=${targetBufferBytes / MIB}MB buffer=${minMs / 1000}-${maxMs / 1000}s " +
                    "start=${startMs}ms rebuffer=${afterRebufferMs}ms"
        )
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(minMs, maxMs, startMs, afterRebufferMs)
            .setTargetBufferBytes(targetBufferBytes)
            .setPrioritizeTimeOverSizeThresholds(false)
            .build()
    }

    fun attachPlaybackPriority(player: ExoPlayer) {
        player.addListener(object : Player.Listener {
            private fun isNetwork(): Boolean {
                val scheme = player.currentMediaItem?.localConfiguration?.uri?.scheme
                return scheme == "http" || scheme == "https"
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> if (isNetwork() && player.playWhenReady) {
                        PlaybackPriority.onBuffering()
                    }
                    Player.STATE_READY -> PlaybackPriority.onStable()
                    Player.STATE_IDLE, Player.STATE_ENDED -> PlaybackPriority.onStopped()
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) PlaybackPriority.onStable()
            }
        })
    }
}
