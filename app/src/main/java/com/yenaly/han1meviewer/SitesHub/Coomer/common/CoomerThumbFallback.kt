package com.yenaly.han1meviewer.SitesHub.Coomer.common

import java.util.concurrent.ConcurrentHashMap

/**
 * Many post thumbnails on the image hosts are missing (HTTP 404) or the post has no image at all.
 * The parser remembers the creator avatar for every thumbnail it hands out, and the image loader
 * swaps it in when the original cannot be fetched.
 */
object CoomerThumbFallback {

    private const val MAX = 4000
    private val map = ConcurrentHashMap<String, String>()

    fun register(thumbUrl: String, fallbackUrl: String) {
        if (thumbUrl.isBlank() || fallbackUrl.isBlank() || thumbUrl == fallbackUrl) return
        if (map.size > MAX) map.clear()
        map[thumbUrl] = fallbackUrl
    }

    fun get(thumbUrl: String): String? = map[thumbUrl]
}
