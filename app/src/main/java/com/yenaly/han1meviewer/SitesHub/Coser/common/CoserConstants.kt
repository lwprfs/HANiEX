package com.yenaly.han1meviewer.SitesHub.Coser.common

import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.logic.network.SiteNetworkPrefs
import java.net.URLEncoder

object CoserConstants {

    val BASE_URL: String
        get() = Preferences.coserBaseUrl.trimEnd('/')

    val HOSTNAME: Array<String>
        get() = SiteNetworkPrefs.knownHosts(SiteType.COSER, listOf("coser.tv"))

    const val STREAM_ORIGIN = "https://coser.tv"
    const val STREAM_REFERER = "https://coser.tv/"

    val QUALITIES = listOf("auto", "1080p", "720p", "480p", "360p")
    const val DEFAULT_QUALITY = "auto"

    val PHOTO_COLUMN_OPTIONS = listOf(2, 3, 4)
    const val DEFAULT_PHOTO_COLUMNS = 3

    enum class SectionKind { POSTS, TERMS }

    data class Section(
        val key: String,
        val title: String,
        val path: String,
        val kind: SectionKind = SectionKind.POSTS,
    ) {
        val collectTerms: Boolean get() = kind == SectionKind.TERMS
    }

    val SECTION_LATEST = Section("latest", "Latest", "/latest/")
    val SECTION_TRENDING = Section("trending", "Weekly trending", "/trending/")
    val SECTION_MOST_VIEWED = Section("most-viewed", "Most viewed", "/most-viewed/")
    val SECTION_SERIES = Section("series", "Series", "/series/", SectionKind.TERMS)

    val HOME_SECTIONS: List<Section> = listOf(
        SECTION_TRENDING,
        SECTION_LATEST,
        SECTION_MOST_VIEWED,
        SECTION_SERIES,
    )

    data class Filters(
        val mins: Int = 0,
        val quality: Int = 0,
        val sort: String = "",
    ) {
        val isDefault: Boolean get() = mins == 0 && quality == 0 && sort.isEmpty()

        fun toQuery(): String = buildList {
            if (mins > 0) add("mins=$mins")
            if (sort.isNotEmpty()) add("sort=$sort")
            if (quality > 0) add("quality=$quality")
        }.joinToString("&")
    }

    val LENGTH_OPTIONS = listOf(0 to "Any", 5 to "5m+", 10 to "10m+", 20 to "20m+", 30 to "30m+", 60 to "1h+")
    val QUALITY_OPTIONS = listOf(0 to "Any", 720 to "720p+", 1080 to "1080p+", 2160 to "4K")
    val SORT_OPTIONS = listOf("" to "Default", "latest" to "Newest", "trending" to "Trending", "most-viewed" to "Most viewed")

    private fun cleanPath(path: String): String {
        val clean = path.trim()
        if (clean.isEmpty() || clean == "/") return "/"
        return "/" + clean.trim('/') + "/"
    }

    fun pagedUrl(path: String, page: Int, filters: Filters = Filters()): String {
        val raw = path.trim()
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
        val base = cleanPath(raw.substringBefore('?'))
        val params = ArrayList<String>()
        raw.substringAfter('?', "").takeIf { it.isNotBlank() }?.let { params += it }
        filters.toQuery().takeIf { it.isNotEmpty() }?.let { params += it }
        if (page > 1) params += "page=$page"
        return BASE_URL + base + if (params.isEmpty()) "" else "?" + params.joinToString("&")
    }

    fun pageUrl(path: String, filters: Filters = Filters()): String = pagedUrl(path, 1, filters)

    fun seriesPath(slug: String) = "/series/$slug/"
    fun tagPath(slug: String) = "/tag/$slug/"

    fun searchPath(query: String): String = "/search/?q=" + URLEncoder.encode(query, "UTF-8")

    fun searchUrl(query: String, page: Int = 1, filters: Filters = Filters()): String =
        pagedUrl(searchPath(query), page, filters)

    fun postUrl(slug: String): String = "$BASE_URL/post/${slug.trim('/')}/"

    fun parsePostReference(input: String): String? {
        val text = input.trim()
        if (!(text.startsWith("http://") || text.startsWith("https://"))) return null
        val uri = runCatching { java.net.URI(text) }.getOrNull() ?: return null
        val host = uri.host.orEmpty().lowercase()
        if (HOSTNAME.none { host == it || host.endsWith(".$it") }) return null
        return slugFromPath(uri.path.orEmpty())
    }

    private val LANG_PREFIXES = setOf("ja", "zh-hans", "ko")

    fun slugFromPath(path: String): String? {
        var segments = path.split('/').filter { it.isNotBlank() }
        if (segments.firstOrNull() in LANG_PREFIXES) segments = segments.drop(1)
        if (segments.size != 2 || segments[0] != "post") return null
        return segments[1].takeIf { it.isNotBlank() }
    }
}
