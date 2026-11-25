package com.yenaly.han1meviewer.worker

import android.content.Context
import androidx.core.content.edit

object HlsResumeStore {

    private const val PREFS = "hls_resume"

    data class Checkpoint(val signature: String, val segments: Int, val bytes: Long)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun key(videoCode: String, quality: String) = "$videoCode|$quality"

    fun signature(playlist: HlsDownloader.MediaPlaylist): String {
        fun path(url: String) = url.substringBefore('#').substringBefore('?')
        val first = playlist.segments.first().url
        val last = playlist.segments.last().url
        return "${playlist.segments.size}|${path(first).hashCode()}|${path(last).hashCode()}|" +
                (playlist.initSegmentUrl?.let { path(it).hashCode() } ?: 0)
    }

    fun load(context: Context, videoCode: String, quality: String): Checkpoint? {
        val raw = prefs(context).getString(key(videoCode, quality), null) ?: return null
        val parts = raw.split('~')
        if (parts.size != 3) return null
        val segments = parts[1].toIntOrNull() ?: return null
        val bytes = parts[2].toLongOrNull() ?: return null
        return Checkpoint(parts[0], segments, bytes)
    }

    fun save(context: Context, videoCode: String, quality: String, cp: Checkpoint) {
        prefs(context).edit { putString(key(videoCode, quality), "${cp.signature}~${cp.segments}~${cp.bytes}") }
    }

    fun clear(context: Context, videoCode: String, quality: String) {
        prefs(context).edit { remove(key(videoCode, quality)) }
    }

    fun clearAll(context: Context, videoCode: String) {
        val p = prefs(context)
        val prefix = "$videoCode|"
        p.edit { p.all.keys.filter { it.startsWith(prefix) }.forEach { remove(it) } }
    }
}
