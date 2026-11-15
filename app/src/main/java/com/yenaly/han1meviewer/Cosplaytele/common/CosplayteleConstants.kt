package com.yenaly.han1meviewer.Cosplaytele.common

import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.logic.network.SiteNetworkPrefs
import java.net.URLEncoder

/**
 * Cosplaytele (WordPress + Flatsome theme).
 *
 * Page map taken from the HAR captures:
 *  - `/`                       latest posts (paged: `/page/N/`)
 *  - `/24-hours/`              most viewed in the last 24 hours (WordPress Popular Posts page)
 *  - `/top-search/`            most searched cosplayers / tags
 *  - `/best-cosplayer/`        best cosplayers
 *  - `/explore-categories/`    all categories + tags
 *  - `/category/<slug>/`       category archive (cosplay, cosplay-nudee, cosplay-ero, video-cosplayyy,
 *                              cosplayer names such as machi, sticky-bunny, game names such as genshin-impact)
 *  - `/tag/<slug>/`            tag archive (frieren, wuthering-waves, rezero ...)
 *  - `/<post-slug>/`           a post: photo gallery and/or video embed
 *  - `/?s=<query>`             full search, `/wp-admin/admin-ajax.php` (Ajax Search Lite) for live search
 */
object CosplayteleConstants {

    val BASE_URL: String
        get() = Preferences.cosplayteleBaseUrl.trimEnd('/')

    val HOSTNAME: Array<String>
        get() = SiteNetworkPrefs.knownHosts(SiteType.COSPLAYTELE, listOf("cosplaytele.com"))

    /** Video host used by the embedded players. */
    const val STREAM_HOST = "cossora.stream"
    const val STREAM_ORIGIN = "https://cossora.stream"
    const val STREAM_REFERER = "https://cossora.stream/"

    /** Ajax Search Lite instance id used by the site's own search box. */
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

    /** Sections of the home page, in display order. */
    val HOME_SECTIONS: List<Section> = listOf(
        Section("latest", "Latest", ""),
        Section("24h", "Top 24 hours", "/24-hours/"),
        Section("video", "Videos", "/category/video-cosplayyy/"),
        Section("top-search", "Top search", "/top-search/", SectionKind.TERMS),
        Section("best", "Best cosplayer", "/best-cosplayer/", SectionKind.TERMS),
        Section("explore", "Categories", "/explore-categories/", SectionKind.TERMS),
        Section("cosplay", "Cosplay", "/category/cosplay/"),
        Section("nude", "Nude", "/category/cosplay-nudee/"),
        Section("ero", "Ero", "/category/cosplay-ero/"),
    )

    val SECTION_24H = HOME_SECTIONS[1]
    val SECTION_TOP_SEARCH = HOME_SECTIONS[3]
    val SECTION_BEST = HOME_SECTIONS[4]
    val SECTION_EXPLORE = HOME_SECTIONS[5]

    fun pageUrl(path: String): String {
        val clean = path.trim()
        if (clean.startsWith("http://") || clean.startsWith("https://")) return clean
        return if (clean.isEmpty() || clean == "/") "$BASE_URL/" else BASE_URL + "/" + clean.trim('/') + "/"
    }

    /** Builds the url of [page] for an archive path (`/category/x/` -> `/category/x/page/2/`). */
    fun pagedUrl(path: String, page: Int): String {
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

    /** First url segments that are never a post. */
    val RESERVED_SEGMENTS = setOf(
        "category", "tag", "page", "author", "feed", "comments", "search", "wp-content", "wp-admin",
        "wp-json", "wp-includes", "wp-login.php", "xmlrpc.php", "24-hours", "top-search",
        "best-cosplayer", "explore-categories", "privacy-policy", "contact", "contact-us", "dmca",
        "about", "about-us", "terms", "terms-of-service", "disclaimer", "faq", "sitemap", "sitemap.xml",
        "cart", "checkout", "my-account", "shop", "request", "report", "donate",
    )

    /** `https://cosplaytele.com/the-herta-swimsuit-2/` -> `the-herta-swimsuit-2`. */
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
        if (seg in RESERVED_SEGMENTS || seg.contains('.')) return null
        return segments[0]
    }
}
