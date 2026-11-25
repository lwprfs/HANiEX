package com.yenaly.han1meviewer.SitesHub.LeakGallery.common

import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.logic.network.SiteNetworkPrefs
import java.net.URLEncoder

object LeakGalleryConstants {

    val BASE_URL: String
        get() = Preferences.leakgalleryBaseUrl.trimEnd('/')

    val HOSTNAME: Array<String>
        get() = SiteNetworkPrefs.knownHosts(SiteType.LEAKGALLERY, listOf("leakgallery.com"))

    /** JSON API; every request needs the `x-app-token` that the site embeds in its pages. */
    const val API_URL = "https://api.leakgallery.com"

    /** Media (videos, photos, thumbnails, avatars) is served from here. */
    const val CDN_URL = "https://cdn.leakgallery.com"

    val PHOTO_COLUMN_OPTIONS = listOf(2, 3, 4)
    const val DEFAULT_PHOTO_COLUMNS = 3

    /** How the page number is written in the API path. */
    enum class PageStyle {
        /** Page 1 has no suffix, page n is `/n` (popular, profile). */
        BARE_FIRST,

        /** Pages are `/1`, `/2`, ... (feed, new profiles, tags). */
        FROM_ONE,

        /** Pages are `/0`, `/1`, ... (shorts). */
        FROM_ZERO,
    }

    data class Section(
        val key: String,
        val title: String,
        /** API path, may contain a query string. Page suffix is inserted before the `?`. */
        val path: String,
    )

    val SECTION_TRENDING = Section("trending", "Trending", "/popular/media/Last-Hour")
    val SECTION_TODAY = Section("today", "Today", "/popular/media/Day")
    val SECTION_RISING = Section("rising", "Rising", "/popular/media/Rising")
    val SECTION_ALL_TIME = Section("all_time", "All time", "/popular/media/All-Time")
    val SECTION_LATEST = Section("latest", "Latest", "/feed?type=All")
    val SECTION_PHOTOS = Section("photos", "Photos", "/feed?type=Photos")
    val SECTION_VIDEOS = Section("videos", "Videos", "/feed?type=Videos")
    val SECTION_MODELS = Section("models", "New models", "/popular/newest-profiles")
    val SECTION_SHORTS = Section("shorts", "Shorts", "/tiktok/trending")

    val HOME_SECTIONS: List<Section> = listOf(
        SECTION_TRENDING,
        SECTION_TODAY,
        SECTION_RISING,
        SECTION_ALL_TIME,
        SECTION_LATEST,
        SECTION_PHOTOS,
        SECTION_VIDEOS,
        SECTION_MODELS,
        SECTION_SHORTS,
    )

    val PROFILE_TYPES = listOf("All", "Photos", "Videos")

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    fun pageStyleFor(path: String): PageStyle {
        val p = "/" + path.substringBefore('?').trim('/')
        return when {
            p.startsWith("/tiktok") -> PageStyle.FROM_ZERO
            p.startsWith("/feed") || p.startsWith("/popular/newest-profiles") ||
                p.startsWith("/tag/") -> PageStyle.FROM_ONE
            else -> PageStyle.BARE_FIRST
        }
    }

    /** [page] is 1-based; returns the full API URL for it. */
    fun apiUrl(path: String, page: Int): String {
        val raw = path.trim()
        val base = "/" + raw.substringBefore('?').trim('/')
        val query = raw.substringAfter('?', "")
        val suffix = when (pageStyleFor(base)) {
            PageStyle.BARE_FIRST -> if (page > 1) "/$page" else ""
            PageStyle.FROM_ONE -> "/$page"
            PageStyle.FROM_ZERO -> "/${page - 1}"
        }
        return API_URL + base + suffix + if (query.isBlank()) "" else "?$query"
    }

    fun profilePath(username: String, type: String = "All"): String =
        "/profile/${encode(username)}?type=$type&sort=MostRecent&tag=0"

    fun tagPath(slug: String): String = "/tag/${encode(slug)}"

    /** `username` out of a `/profile/<username>[?...]` path (or null). */
    fun usernameFromProfilePath(path: String): String? {
        val p = path.substringBefore('?').trim('/').split('/')
        return if (p.size >= 2 && p[0] == "profile") p[1] else null
    }

    fun cdn(path: String): String {
        val p = path.trim()
        if (p.isEmpty()) return ""
        if (p.startsWith("http://") || p.startsWith("https://")) return p
        return CDN_URL + "/" + p.trimStart('/')
    }

    fun webUrl(username: String, id: String): String =
        if (username.isBlank()) "$BASE_URL/$id" else "$BASE_URL/$username/$id"

    fun postUrl(slug: String): String = "$BASE_URL/$slug"

    /** Accepts `https://leakgallery.com/<user>/<id>` or `/<user>/<title>-<id>` and returns the id. */
    fun parsePostReference(input: String): String? {
        val text = input.trim()
        if (!(text.startsWith("http://") || text.startsWith("https://"))) return null
        val uri = runCatching { java.net.URI(text) }.getOrNull() ?: return null
        val host = uri.host.orEmpty().lowercase()
        if (HOSTNAME.none { host == it || host.endsWith(".$it") }) return null
        val segments = uri.path.orEmpty().split('/').filter { it.isNotBlank() }
        if (segments.size != 2) return null
        val last = segments[1]
        if (last.all { it.isDigit() }) return last
        return Regex("""-(\d+)$""").find(last)?.groupValues?.get(1)
    }
}
