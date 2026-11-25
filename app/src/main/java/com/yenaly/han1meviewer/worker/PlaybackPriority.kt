package com.yenaly.han1meviewer.worker

import android.os.SystemClock
import kotlinx.coroutines.delay

object PlaybackPriority {

    private const val RECOVERY_MS = 6_000L
    private const val BUFFERING_STALE_MS = 45_000L
    private const val READ_PAUSE_MS = 40L

    @Volatile
    private var buffering = false

    @Volatile
    private var bufferingSince = 0L

    @Volatile
    private var stableSince = 0L

    fun onBuffering() {
        if (!buffering) bufferingSince = SystemClock.elapsedRealtime()
        buffering = true
    }

    fun onStable() {
        if (buffering) {
            buffering = false
            stableSince = SystemClock.elapsedRealtime()
        }
    }

    fun onStopped() {
        buffering = false
        stableSince = 0L
    }

    val isBuffering: Boolean
        get() {
            if (!buffering) return false
            if (SystemClock.elapsedRealtime() - bufferingSince > BUFFERING_STALE_MS) {
                buffering = false
                return false
            }
            return true
        }

    fun concurrencyCap(max: Int): Int {
        if (isBuffering) return minOf(2, max).coerceAtLeast(1)
        val since = stableSince
        if (since != 0L && SystemClock.elapsedRealtime() - since < RECOVERY_MS) {
            return (max / 2).coerceAtLeast(2).coerceAtMost(max)
        }
        return max
    }

    suspend fun yieldToPlayback() {
        if (isBuffering) delay(READ_PAUSE_MS)
    }
}
