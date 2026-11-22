package com.yenaly.han1meviewer.OnAir.common

object OnAirThumbCache {

    private const val MAX_ENTRIES = 2000

    private val map = object : LinkedHashMap<String, String>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean =
            size > MAX_ENTRIES
    }

    fun put(videoId: String, thumbUrl: String?) {
        if (videoId.isBlank() || thumbUrl.isNullOrBlank()) return
        synchronized(map) { map[videoId] = thumbUrl }
    }

    fun get(videoId: String): String? {
        if (videoId.isBlank()) return null
        return synchronized(map) { map[videoId] }
    }
}
