package com.yenaly.han1meviewer.OnAir.common

import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.util.applicationContext

object OnAirQuality {
    const val ORIGINAL = "original"
    const val HLS = "hls"

    val CHOICES: List<String> =
        listOf(ORIGINAL, HLS, "1080", "720", "480", "360", "240", "144")

    fun isKnownChoice(q: String) = q in CHOICES

    fun pageQuality(q: String): String = if (q == HLS) ORIGINAL else q

    fun height(q: String): Int = when (q) {
        ORIGINAL -> Int.MAX_VALUE
        HLS -> Int.MAX_VALUE - 1
        else -> q.toIntOrNull() ?: 0
    }

    fun order(keys: Collection<String>): List<String> =
        keys.filter { it.isNotBlank() }.distinct().sortedByDescending(::height)

    fun label(q: String): String = when (q) {
        ORIGINAL -> applicationContext.getString(R.string.site_quality_original)
        HLS -> applicationContext.getString(R.string.onair_quality_auto_hls)
        else -> if (q.all(Char::isDigit)) "${q}p" else q
    }

    fun resolve(wanted: String, available: List<String>): String {
        if (available.isEmpty() || wanted in available) return wanted
        val numeric = available.mapNotNull { q -> q.toIntOrNull()?.let { q to it } }
        if (wanted == HLS) {
            if (ORIGINAL in available) return ORIGINAL
            return numeric.maxByOrNull { it.second }?.first ?: available.first()
        }
        val h = height(wanted)
        numeric.filter { it.second <= h }.maxByOrNull { it.second }?.let { return it.first }
        numeric.filter { it.second > h }.minByOrNull { it.second }?.let { return it.first }
        return when {
            ORIGINAL in available -> ORIGINAL
            HLS in available -> HLS
            else -> available.first()
        }
    }
}
