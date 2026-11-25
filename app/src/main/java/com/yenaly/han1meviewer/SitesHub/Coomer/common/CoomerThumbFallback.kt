package com.yenaly.han1meviewer.SitesHub.Coomer.common

import java.util.concurrent.ConcurrentHashMap

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
