package com.yenaly.han1meviewer.Cosplaytele.common

import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.logic.network.SiteNetworkPrefs
import java.net.URLEncoder

object CosplayteleConstants {

    val BASE_URL: String
        get() = Preferences.cosplayteleBaseUrl.trimEnd('/')

    val HOSTNAME: Array<String>
        get() = SiteNetworkPrefs.knownHosts(SiteType.COSPLAYTELE, listOf("cosplaytele.com"))

    const val STREAM_HOST = "cossora.stream"
    const val STREAM_ORIGIN = "https://cossora.stream"
    const val STREAM_REFERER = "https://cossora.stream/"

    const val AJAX_SEARCH_ID = "3"

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

    val SECTION_LATEST = Section("latest", "Latest", "")
    val SECTION_24H = Section("24h", "Top 24 hours", "/24-hours/")
    val SECTION_3D = Section("3d", "Top 3 days", "/3-day/")
    val SECTION_7D = Section("7d", "Top 7 days", "/7-day/")
    val SECTION_VIDEOS = Section("video", "Videos", "/category/video-cosplayyy/")
    val SECTION_TOP_SEARCH = Section("top-search", "Top search", "/top-search/", SectionKind.TERMS)
    val SECTION_BEST = Section("best", "Best cosplayer", "/best-cosplayer/", SectionKind.TERMS)
    val SECTION_EXPLORE = Section("explore", "Categories", "/explore-categories/", SectionKind.TERMS)

    val HOME_SECTIONS: List<Section> = listOf(
        SECTION_LATEST,
        SECTION_7D,
        SECTION_VIDEOS,
        Section("cosplay", "Cosplay", "/category/cosplay/"),
        Section("nude", "Nude", "/category/cosplay-nudee/"),
        Section("ero", "Ero", "/category/cosplay-ero/"),
    )

    val PAGES_SECTIONS: List<Section> = listOf(
        SECTION_24H,
        SECTION_3D,
        SECTION_7D,
        SECTION_BEST,
        SECTION_TOP_SEARCH,
        SECTION_EXPLORE,
    )

    private val PERIOD_SLUG_REGEX =
        Regex("""^(top-)?\d+-(hours?|days?|weeks?|months?)$""", RegexOption.IGNORE_CASE)

    fun isPeriodSlug(slug: String): Boolean = PERIOD_SLUG_REGEX.matches(slug.trim('/'))

    fun isRankingUrl(url: String): Boolean {
        val path = url.substringBefore('?').substringBefore('#')
            .substringAfter("://").substringAfter('/', "")
        val segments = path.split('/').filter { it.isNotBlank() }
        return segments.size == 1 && isPeriodSlug(segments[0])
    }

    fun pageUrl(path: String): String {
        val clean = path.trim()
        if (clean.startsWith("http://") || clean.startsWith("https://")) return clean
        val q = clean.indexOf('?')
        if (q >= 0) {
            val base = clean.substring(0, q).trim('/')
            val query = clean.substring(q)
            return BASE_URL + (if (base.isEmpty()) "/" else "/$base/") + query
        }
        return if (clean.isEmpty() || clean == "/") "$BASE_URL/" else BASE_URL + "/" + clean.trim('/') + "/"
    }

    fun pagedUrl(path: String, page: Int): String {
        if (path.contains("?s=")) {
            val raw = path.substringAfter("?s=").substringBefore('&')
            val query = runCatching { java.net.URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)
            return searchUrl(query, page)
        }
        val first = pageUrl(path)
        return if (page <= 1) first else first.trimEnd('/') + "/page/$page/"
    }

    fun categoryPath(slug: String) = "/category/$slug/"
    fun tagPath(slug: String) = "/tag/$slug/"

    fun searchUrl(query: String, page: Int = 1): String {
        val q = URLEncoder.encode(query, "UTF-8")
        return if (page <= 1) "$BASE_URL/?s=$q" else "$BASE_URL/page/$page/?s=$q"
    }

    fun postUrl(slug: String): String = "$BASE_URL/${slug.trim('/')}/"

    val RESERVED_SEGMENTS = setOf(
        "category", "tag", "page", "author", "feed", "comments", "search", "wp-content", "wp-admin",
        "wp-json", "wp-includes", "wp-login.php", "xmlrpc.php", "24-hours", "top-search",
        "best-cosplayer", "explore-categories", "privacy-policy", "contact", "contact-us", "dmca",
        "about", "about-us", "terms", "terms-of-service", "disclaimer", "faq", "sitemap", "sitemap.xml",
        "cart", "checkout", "my-account", "shop", "request", "report", "donate",
    )

    fun parsePostReference(input: String): String? {
        val text = input.trim()
        if (text.isEmpty()) return null
        if (text.startsWith("http://") || text.startsWith("https://")) {
            val uri = runCatching { java.net.URI(text) }.getOrNull() ?: return null
            val host = uri.host.orEmpty().lowercase()
            if (HOSTNAME.none { host == it || host.endsWith(".$it") }) return null
            return slugFromPath(uri.path.orEmpty())
        }
        return null
    }

    fun slugFromPath(path: String): String? {
        val segments = path.split('/').filter { it.isNotBlank() }
        if (segments.size != 1) return null
        val seg = segments[0].lowercase()
        if (seg in RESERVED_SEGMENTS || seg.contains('.') || isPeriodSlug(seg)) return null
        return segments[0]
    }
}
