package com.yenaly.han1meviewer.FuckingSession.common

object FuckingSessionConstants {

    val BASE_URL: String
        get() = com.yenaly.han1meviewer.Preferences.fuckingSessionBaseUrl.trimEnd('/')

    val HOSTNAME: Array<String>
        get() = com.yenaly.han1meviewer.logic.network.SiteNetworkPrefs.knownHosts(
            com.yenaly.han1meviewer.SiteType.FUCKINGSESSION,
            listOf("fuckingsession.com"),
        )

    data class Section(val key: String, val title: String, val path: String)

    val SECTIONS: List<Section> = listOf(
        Section("latest", "Latest", "/"),
        Section("hd-porn", "HD Porn", "/category/hd-porn"),
        Section("anal", "Anal", "/category/anal"),
        Section("milf", "MILF", "/category/milf"),
        Section("asian-uncensored", "Asian Uncensored", "/category/asian-uncensored"),
    )

    val ORDERS: List<Section> = listOf(
        Section("newest", "Latest", ""),
        Section("oldest", "Oldest", "?order=oldest"),
        Section("most_commented", "Most Discussed", "?order=most_commented"),
    )

    private val RESERVED_SEGMENTS = setOf(
        "category", "tag", "page", "author", "search", "feed", "comments", "wp-content",
        "wp-admin", "wp-includes", "wp-json", "wp-login.php", "xmlrpc.php", "sitemap.xml",
        "sitemap_index.xml", "robots.txt", "tags", "categories",
    )

    private fun splitPath(path: String): Pair<String, String> {
        val idx = path.indexOf('?')
        val base = if (idx >= 0) path.substring(0, idx) else path
        val query = if (idx >= 0) path.substring(idx) else ""
        return base to query
    }

    fun pagedUrl(path: String, page: Int): String {
        val (base, query) = splitPath(path)
        val clean = base.trim('/').trim()
        val root = if (clean.isEmpty()) "$BASE_URL/" else "$BASE_URL/$clean/"
        val paged = if (page <= 1) root else "${root}page/$page/"
        return paged + query
    }

    fun searchUrl(query: String, page: Int): String {
        val q = java.net.URLEncoder.encode(query.trim(), "UTF-8")
        return if (page <= 1) "$BASE_URL/?s=$q" else "$BASE_URL/page/$page/?s=$q"
    }

    fun codeFromUrl(url: String): String? {
        val path = runCatching { java.net.URI(url).path }.getOrNull().orEmpty()
        val segments = path.split('/').filter { it.isNotBlank() }
        if (segments.size != 1) return null
        val slug = segments[0]
        if (slug.lowercase() in RESERVED_SEGMENTS || '.' in slug) return null
        return slug
    }

    fun parseVideoReference(input: String): String? {
        val text = input.trim()
        if (!(text.startsWith("http://") || text.startsWith("https://"))) return null
        val host = runCatching { java.net.URI(text).host }.getOrNull().orEmpty().lowercase()
        if (HOSTNAME.none { host == it || host.endsWith(".$it") }) return null
        return codeFromUrl(text)
    }

    fun videoUrl(code: String): String = "$BASE_URL/$code/"
}
