package com.yenaly.han1meviewer.Wumaobi.common

object WumaobiConstants {

    val BASE_URL: String
        get() = com.yenaly.han1meviewer.Preferences.wumaobiBaseUrl.trimEnd('/')

    val HOSTNAME: Array<String>
        get() = com.yenaly.han1meviewer.logic.network.SiteNetworkPrefs.knownHosts(
            com.yenaly.han1meviewer.SiteType.WUMAOBI,
            listOf("wumaobi.com", "paipancon.com", "baihuse.com", "shavedjav.com"),
        )

    /** "High" prefers HQ (VIP) lines when the account can play them, "Low" the standard line. */
    val QUALITIES = listOf("High", "Low")
    const val DEFAULT_QUALITY = "High"

    /**
     * [sort] is the value of the site's `sort` cookie (hits / id / faves); `null` leaves the
     * cookie alone (used for pages that do not support sorting such as /recommend).
     */
    data class Section(
        val key: String,
        val title: String,
        val path: String,
        val sort: String? = null,
    )

    val SECTIONS: List<Section> = listOf(
        Section("hits", "Hottest", "/", "hits"),
        Section("id", "Newest", "/", "id"),
        Section("faves", "Most Faves", "/", "faves"),
        Section("recommend", "Recommended", "/recommend"),
    )

    val TIME_FILTERS: Map<String, List<Section>> = emptyMap()

    /** Sort options for any video list that sits under an actress / series / tag / search path. */
    val ENTITY_SORTS: List<Section> = listOf(
        Section("hits", "Hottest", "", "hits"),
        Section("id", "Newest", "", "id"),
        Section("faves", "Most Faves", "", "faves"),
    )

    enum class EntityKind(val key: String, val path: String) {
        TAGS("tags", "/category_list"),
        ACTRESS("actress", "/actress"),
        SERIES("series", "/category_list");

        companion object {
            fun fromKey(key: String): EntityKind = entries.firstOrNull { it.key == key } ?: TAGS
        }
    }

    /** For tags / series the section value is the `category_sort` cookie; actresses are unsorted. */
    fun entitySorts(kind: EntityKind): List<Section> = when (kind) {
        EntityKind.ACTRESS -> emptyList()
        EntityKind.TAGS, EntityKind.SERIES -> listOf(
            Section("counts", "Count", "", "counts"),
            Section("total_hits", "Total Hits", "", "total_hits"),
            Section("avg_hits", "Avg Hits", "", "avg_hits"),
            Section("max_hits", "Max Hits", "", "max_hits"),
        )
    }

    fun encodeSegment(text: String): String =
        java.net.URLEncoder.encode(text.trim(), "UTF-8").replace("+", "%20")

    /** `/series/FC2` -> page 2 is `/series/FC2/page-2.html`; the home page is `/page-2.html`. */
    fun pagedUrl(path: String, page: Int): String {
        val clean = "/" + path.trim('/').trim()
        return when {
            clean == "/" -> if (page <= 1) "$BASE_URL/" else "$BASE_URL/page-$page.html"
            page <= 1 -> "$BASE_URL$clean"
            else -> "$BASE_URL$clean/page-$page.html"
        }
    }

    fun entityListUrl(kind: EntityKind, page: Int): String = when (kind) {
        EntityKind.ACTRESS -> pagedUrl(kind.path, page)
        else -> "$BASE_URL${kind.path}"
    }

    fun searchUrl(query: String, page: Int): String =
        pagedUrl("/search/${encodeSegment(query)}", page)

    fun favFolderUrl(name: String, page: Int): String =
        pagedUrl("/user/fav/${encodeSegment(name)}", page)

    private val VIDEO_PATH_REGEX = Regex("""/?(\d+)-\d+-\d+\.html""")

    fun codeFromUrl(url: String): String? {
        val path = url.substringBefore('?').substringBefore('#')
        return VIDEO_PATH_REGEX.find(path)?.groupValues?.get(1)
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

    fun videoUrl(code: String): String = "$BASE_URL/$code-1-1.html"
}
