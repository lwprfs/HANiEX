package com.yenaly.han1meviewer.OnAir.common

/**
 * Remembers the thumbnail of each video as seen on listing pages (home, search, tag, user,
 * recommended sidebar). The watch page route only carries the video id, so this lets the
 * detail screen save the real thumbnail of *that* video into history, downloads and playlists
 * instead of borrowing some other video's picture.
 */
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
