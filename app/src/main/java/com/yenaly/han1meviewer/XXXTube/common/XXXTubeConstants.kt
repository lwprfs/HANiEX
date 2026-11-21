package com.yenaly.han1meviewer.XXXTube.common

object XXXTubeConstants {

    val BASE_URL: String
        get() = com.yenaly.han1meviewer.Preferences.xxxTubeBaseUrl.trimEnd('/')

    val HOSTNAME: Array<String>
        get() = com.yenaly.han1meviewer.logic.network.SiteNetworkPrefs.knownHosts(
            com.yenaly.han1meviewer.SiteType.XXXTUBE,
            listOf("x-x-x.tube"),
        )

    /** A home tab. [path] is a site-relative path that may carry a `?by=` sort query. */
    data class Section(val key: String, val title: String, val path: String)

    val SECTIONS: List<Section> = listOf(
        Section("latest", "Latest", "/videos/"),
        Section("top-rated", "Top Rated", "/videos/?by=rating"),
        Section("most-viewed", "Most Viewed", "/videos/?by=video_viewed"),
        Section("longest", "Longest", "/videos/?by=duration"),
        Section("favourited", "Most Favourited", "/videos/?by=most_favourited"),
    )

    /** Sort options for model / category / tag video listings; [path] is the query to append. */
    val ORDERS: List<Section> = listOf(
        Section("latest", "Latest", "?by=post_date"),
        Section("rating", "Top Rated", "?by=rating"),
        Section("viewed", "Most Viewed", "?by=video_viewed"),
        Section("duration", "Longest", "?by=duration"),
        Section("favourited", "Most Favourited", "?by=most_favourited"),
    )

    enum class EntityKind(val key: String, val listPath: String, val searchSegment: String) {
        MODELS("models", "/models/", "models/"),
        CATEGORIES("categories", "/categories/", "categories/");

        /** Matches `/models/<slug>/` (or `/categories/<slug>/`) and captures the slug. */
        val itemRegex: Regex = Regex("""^/$key/([^/?#]+)/?$""")

        companion object {
            fun fromKey(key: String?): EntityKind =
                entries.firstOrNull { it.key == key } ?: MODELS
        }
    }

    enum class SearchType(val segment: String) {
        VIDEOS(""), MODELS("models/"), CATEGORIES("categories/")
    }

    private val VIDEO_PATH = Regex("""^/videos/(\d+)(?:/([^/?#]+))?/?$""")

    fun absUrl(pathOrUrl: String): String =
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) pathOrUrl
        else BASE_URL + (if (pathOrUrl.startsWith("/")) pathOrUrl else "/$pathOrUrl")

    /** Joins a listing path with a sort query (`?by=...`), merging if the path already has one. */
    fun withQuery(path: String, query: String): String {
        if (query.isEmpty()) return path
        val q = query.removePrefix("?")
        return if (path.contains('?')) "$path&$q" else "$path?$q"
    }

    /** `/models/<slug>/` and `/categories/<slug>/` list their videos under `.../videos/`. */
    fun listingPathFor(path: String): String {
        val clean = path.substringBefore('?').substringBefore('#')
        val isEntity = EntityKind.entries.any { it.itemRegex.matches(clean) }
        return if (isEntity) clean.trimEnd('/') + "/videos/" else clean
    }

    fun searchUrl(query: String, type: SearchType): String {
        val slug = query.trim()
            .replace(Regex("""\s+"""), "-")
            .replace("?", "")
            .replace("&", "-")
            .replace("/", "-")
        val encoded = java.net.URLEncoder.encode(slug, "UTF-8").replace("+", "%20")
        return "$BASE_URL/search/${type.segment}$encoded/"
    }

    /** A video code is `<id>-<slug>` (slash free, so it is safe as a file / route segment). */
    fun codeFromPath(path: String): String? {
        val clean = path.substringBefore('?').substringBefore('#')
        val m = VIDEO_PATH.find(clean) ?: return null
        val id = m.groupValues[1]
        val slug = m.groupValues.getOrNull(2).orEmpty()
        return if (slug.isEmpty()) id else "$id-$slug"
    }

    fun codeFromUrl(url: String): String? {
        val path = runCatching { java.net.URI(url).path }.getOrNull() ?: url
        return codeFromPath(path)
    }

    fun idOf(code: String): String = code.substringBefore('-')

    fun slugOf(code: String): String = if ('-' in code) code.substringAfter('-') else ""

    fun videoUrl(code: String): String {
        val slug = slugOf(code)
        return if (slug.isEmpty()) "$BASE_URL/videos/${idOf(code)}/"
        else "$BASE_URL/videos/${idOf(code)}/$slug/"
    }

    fun parseVideoReference(input: String): String? {
        val text = input.trim()
        if (!(text.startsWith("http://") || text.startsWith("https://"))) return null
        val host = runCatching { java.net.URI(text).host }.getOrNull().orEmpty().lowercase()
        if (HOSTNAME.none { host == it || host.endsWith(".$it") }) return null
        return codeFromUrl(text)
    }
}
