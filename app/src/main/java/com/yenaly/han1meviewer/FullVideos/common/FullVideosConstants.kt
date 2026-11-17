package com.yenaly.han1meviewer.FullVideos.common

object FullVideosConstants {

    val BASE_URL: String
        get() = com.yenaly.han1meviewer.Preferences.fullVideosBaseUrl.trimEnd('/')

    val HOSTNAME: Array<String>
        get() = com.yenaly.han1meviewer.logic.network.SiteNetworkPrefs.knownHosts(
            com.yenaly.han1meviewer.SiteType.FULLVIDEOS,
            listOf("fullvideos.to"),
        )

    val QUALITIES = listOf("2160p", "1080p", "720p", "480p", "360p")
    const val DEFAULT_QUALITY = "1080p"

    data class Section(val key: String, val title: String, val path: String)

    val SECTIONS: List<Section> = listOf(
        Section("latest", "Latest", "/latest-updates"),
        Section("top-rated", "Top Rated", "/top-rated"),
        Section("most-popular", "Most Viewed", "/most-popular"),
    )

    val TIME_FILTERS: Map<String, List<Section>> = mapOf(
        "latest" to listOf(
            Section("all", "All", ""),
            Section("3-days", "3 Days", "/3-days"),
            Section("this-week", "This Week", "/this-week"),
            Section("this-month", "This Month", "/this-month"),
            Section("3-months", "3 Months", "/3-months"),
            Section("6-months", "6 Months", "/6-months"),
        ),
        "top-rated" to listOf(
            Section("default", "Default", ""),
            Section("today", "Today", "/today"),
            Section("month", "This Month", "/month"),
        ),
        "most-popular" to listOf(
            Section("all", "All Time", ""),
            Section("today", "Today", "/today"),
            Section("week", "This Week", "/week"),
            Section("month", "This Month", "/month"),
        ),
    )

    val ENTITY_SORTS: List<Section> = listOf(
        Section("default", "Default", ""),
        Section("latest", "Latest", "/latest-updates"),
        Section("top-rated", "Top Rated", "/top-rated"),
        Section("most-popular", "Most Viewed", "/most-popular"),
    )

    enum class EntityKind(val key: String, val path: String) {
        CATEGORIES("categories", "/categories"),
        MODELS("models", "/models"),
        SITES("sites", "/sites");

        companion object {
            fun fromKey(key: String): EntityKind = entries.firstOrNull { it.key == key } ?: CATEGORIES
        }
    }

    fun entitySorts(kind: EntityKind): List<Section> = when (kind) {
        EntityKind.CATEGORIES -> emptyList()
        EntityKind.MODELS -> listOf(
            Section("popular", "Most Viewed", ""),
            Section("top-rated", "Top Rated", "/top-rated"),
            Section("title", "A-Z", "/title"),
            Section("total-videos", "Most Videos", "/total-videos"),
        )
        EntityKind.SITES -> listOf(
            Section("recent", "Recently Updated", ""),
            Section("top-rated", "Top Rated", "/top-rated"),
            Section("title-az", "A-Z", "/title-az"),
            Section("title-za", "Z-A", "/title-za"),
            Section("total-videos", "Most Videos", "/total-videos"),
        )
    }

    private fun pageSuffix(page: Int) = if (page <= 1) "/" else "/$page/"

    fun pagedUrl(path: String, page: Int): String {
        val clean = "/" + path.trim('/').trim()
        return if (clean == "/") "$BASE_URL${pageSuffix(page)}"
        else "$BASE_URL$clean${pageSuffix(page)}"
    }

    fun entityListUrl(kind: EntityKind, sortPath: String, page: Int): String {
        val base = kind.path + sortPath
        val url = pagedUrl(base, page)
        return if (kind == EntityKind.MODELS) "$url?gender_id=0" else url
    }

    fun searchUrl(query: String, page: Int): String {
        val slug = query.trim().replace(Regex("\\s+"), "-")
        val q = java.net.URLEncoder.encode(slug, "UTF-8").replace("+", "%20")
        return "$BASE_URL/search/$q${pageSuffix(page)}"
    }

    fun codeId(code: String): String = code.substringBefore('-')

    fun codeSlug(code: String): String = code.substringAfter('-', "")

    fun makeCode(id: String, slug: String): String =
        if (slug.isBlank()) id else "$id-$slug"

    private val VIDEO_PATH_REGEX = Regex("""/videos/(\d+)(?:/([^/?#]*))?""")

    fun codeFromUrl(url: String): String? {
        val m = VIDEO_PATH_REGEX.find(url) ?: return null
        return makeCode(m.groupValues[1], m.groupValues.getOrNull(2).orEmpty())
    }

    fun parseVideoReference(input: String): String? {
        val text = input.trim()
        if (text.startsWith("id:", ignoreCase = true)) {
            val id = text.substring(3).trim()
            return id.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
        }
        if (text.startsWith("http://") || text.startsWith("https://")) {
            val host = runCatching { java.net.URI(text).host }.getOrNull().orEmpty().lowercase()
            if (HOSTNAME.none { host == it || host.endsWith(".$it") }) return null
            return codeFromUrl(text)
        }
        return null
    }

    fun videoUrl(code: String): String {
        val id = codeId(code)
        val slug = codeSlug(code)
        return if (slug.isBlank()) "$BASE_URL/videos/$id/" else "$BASE_URL/videos/$id/$slug/"
    }
}
