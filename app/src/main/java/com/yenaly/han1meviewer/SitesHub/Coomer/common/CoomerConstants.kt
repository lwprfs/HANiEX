package com.yenaly.han1meviewer.SitesHub.Coomer.common

import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.logic.network.SiteNetworkPrefs
import java.net.URLEncoder

object CoomerConstants {

    val BASE_URL: String
        get() = Preferences.coomerBaseUrl.trimEnd('/')

    val HOSTNAME: Array<String>
        get() = SiteNetworkPrefs.knownHosts(SiteType.COOMER, listOf("coomerfans.com"))

    val PHOTO_COLUMN_OPTIONS = listOf(2, 3, 4)
    const val DEFAULT_PHOTO_COLUMNS = 3

    data class Section(
        val key: String,
        val title: String,
        val path: String,
    )

    val SECTION_CREATORS = Section("creators", "Creators", "/")

    val SECTION_FAVORITED = Section("favorited", "Favorited", "/favorited")

    val SECTION_POSTS = Section("posts", "Posts", "/post-search")

    val HOME_SECTIONS: List<Section> = listOf(
        SECTION_CREATORS,
        SECTION_FAVORITED,
        SECTION_POSTS,
    )

    const val SEARCH_PATH_CREATORS = "/"
    const val SEARCH_PATH_POSTS = "/post-search"

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    fun pagedUrl(path: String, page: Int): String {
        val raw = path.trim()
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
        val base = "/" + raw.substringBefore('?').trimStart('/')
        val params = ArrayList<String>()
        raw.substringAfter('?', "").takeIf { it.isNotBlank() }?.let { params += it }
        if (page > 1) params += "page=$page"
        return BASE_URL + base + if (params.isEmpty()) "" else "?" + params.joinToString("&")
    }

    fun pageUrl(path: String): String = pagedUrl(path, 1)

    fun searchPath(query: String, posts: Boolean): String =
        (if (posts) SEARCH_PATH_POSTS else SEARCH_PATH_CREATORS) + "?q=" + encode(query)

    fun searchUrl(query: String, posts: Boolean, page: Int = 1): String =
        pagedUrl(searchPath(query, posts), page)

    fun creatorPath(slug: String): String {
        val parts = slug.trim('/').split('/')
        return "/u/" + parts.joinToString("/") { encode(it) } + "/"
    }

    fun postUrl(slug: String): String = "$BASE_URL/p/${slug.trim('/')}/"

    fun parsePostReference(input: String): String? {
        val text = input.trim()
        if (!(text.startsWith("http://") || text.startsWith("https://"))) return null
        val uri = runCatching { java.net.URI(text) }.getOrNull() ?: return null
        val host = uri.host.orEmpty().lowercase()
        if (HOSTNAME.none { host == it || host.endsWith(".$it") }) return null
        return slugFromPath(uri.path.orEmpty())
    }

    fun slugFromPath(path: String): String? {
        val segments = path.split('/').filter { it.isNotBlank() }
        if (segments.size < 2 || segments[0] != "p") return null
        return segments[1].takeIf { id -> id.isNotEmpty() && id.all { it.isDigit() } }
    }

    fun creatorIdFromPostPath(path: String): String? {
        val segments = path.split('/').filter { it.isNotBlank() }
        if (segments.size < 3 || segments[0] != "p") return null
        return segments[2].takeIf { id -> id.isNotEmpty() && id.all { it.isDigit() } }
    }

    fun avatarUrl(creatorId: String): String = "$BASE_URL/istorage/$creatorId.jpg"

    fun creatorSlugFromPath(path: String): String? {
        val segments = path.split('/').filter { it.isNotBlank() }
        if (segments.size < 3 || segments[0] != "u") return null
        val service = segments[1]
        val id = segments[2]
        if (id.isEmpty() || !id.all { it.isDigit() }) return null
        val name = segments.getOrNull(3)
        return if (name.isNullOrBlank()) "$service/$id" else "$service/$id/$name"
    }
}
