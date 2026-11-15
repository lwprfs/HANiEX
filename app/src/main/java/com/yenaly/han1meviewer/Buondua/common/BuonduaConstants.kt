package com.yenaly.han1meviewer.Buondua.common

import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.logic.network.SiteNetworkPrefs
import java.net.URLEncoder

object BuonduaConstants {

    val BASE_URL: String
        get() = Preferences.buonduaBaseUrl.trimEnd('/')

    val HOSTNAME: Array<String>
        get() = SiteNetworkPrefs.knownHosts(SiteType.BUONDUA, listOf("buondua.com"))

    private val IMAGE_HOST_SUFFIXES = listOf("buondua.com", "blogspot.com", "misskon.com")

    private val AD_HOST_PARTS = listOf(
        "bkcdn.net", "bxcdn.net", "strpst.com", "growcdnssedge.com", "xxxjmp.com",
        "rmhfrtnd.com", "magsrv.com", "doppiocdn.com", "trendbuzz.to", "kinkluxads.com",
        "stripchatgirls.com", "penguin888.xyz", "googletagmanager.com", "google-analytics.com",
    )

    fun isAdHost(host: String): Boolean {
        val h = host.lowercase()
        return AD_HOST_PARTS.any { h == it || h.endsWith(".$it") }
    }

    fun isImageHost(host: String): Boolean {
        val h = host.lowercase()
        return !isAdHost(h) && IMAGE_HOST_SUFFIXES.any { h == it || h.endsWith(".$it") }
    }

    fun isSiteHost(host: String): Boolean {
        val h = host.lowercase()
        return HOSTNAME.any { h == it || h.endsWith(".$it") }
    }

    val REFERER: String get() = "$BASE_URL/"

    data class Category(val key: String, val title: String, val path: String)

    val CATEGORIES: List<Category> = listOf(
        Category("latest", "Latest", ""),
        Category("hot", "Hot", "/hot"),
        Category("collection", "Collection", "/collection"),
    )

    fun categoryUrl(path: String): String =
        if (path.isEmpty()) "$BASE_URL/" else "$BASE_URL$path"

    fun searchUrl(query: String): String =
        "$BASE_URL/?search=" + URLEncoder.encode(query, "UTF-8")

    fun albumUrl(code: String, page: Int = 1): String {
        val base = "$BASE_URL/${code.trim('/')}"
        return if (page <= 1) base else "$base?page=$page"
    }

    private val ALBUM_ID_REGEX = Regex("""-(\d{3,})$""")

    fun albumIdOf(code: String): String? =
        ALBUM_ID_REGEX.find(code.trim('/'))?.groupValues?.get(1)

    fun albumCodeFromUrl(url: String): String? {
        val path = runCatching { java.net.URI(url.trim()).rawPath }.getOrNull()
            ?: url.substringBefore('?').substringBefore('#')
        val slug = path.trim('/').substringAfterLast('/')
        if (slug.isBlank() || ALBUM_ID_REGEX.find(slug) == null) return null
        return slug
    }

    fun parseAlbumReference(input: String): String? {
        val text = input.trim()
        if (text.startsWith("http://") || text.startsWith("https://")) {
            val host = runCatching { java.net.URI(text).host }.getOrNull().orEmpty()
            if (!isSiteHost(host)) return null
            return albumCodeFromUrl(text)
        }
        return null
    }
}
