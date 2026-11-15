package com.yenaly.han1meviewer.MissAV.data.parser

import com.yenaly.han1meviewer.EMPTY_STRING
import com.yenaly.han1meviewer.HJson
import com.yenaly.han1meviewer.MissAV.data.model.MissAvActress
import com.yenaly.han1meviewer.MissAV.data.model.MissAvActressLink
import com.yenaly.han1meviewer.MissAV.data.model.MissAvActressProfile
import com.yenaly.han1meviewer.MissAV.data.model.MissAvTweet
import com.yenaly.han1meviewer.MissAV.data.model.MissAvTweetsPage
import com.yenaly.han1meviewer.MissAV.data.model.MissAvListPage
import com.yenaly.han1meviewer.MissAV.data.model.MissAvLoginResponse
import com.yenaly.han1meviewer.MissAV.data.model.MissAvPlaylist
import com.yenaly.han1meviewer.MissAV.data.model.MissAvPlaylistOption
import com.yenaly.han1meviewer.MissAV.data.model.MissAvPlaylistOptionsResponse
import com.yenaly.han1meviewer.MissAV.data.model.MissAvSavedState
import com.yenaly.han1meviewer.MissAV.data.model.MissAvViewResponse
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI

object MissAvAccountParser {

    private val ITEM_KEY = Regex("""/api/items/([A-Za-z0-9]+)/(?:view|save)""")
    private val ACTRESS_ID_API = Regex("""/api/actresses/(\d+)/(?:view|save)""")
    private val ACTRESS_ID_IMG = Regex("""/actress/(\d+)""")
    private val PLAYLIST_KEY = Regex("""/playlists/([A-Za-z0-9]{6,})(?:[/?#].*)?$""")
    private val MAGNET = Regex("""magnet:\?xt=[^"'\s<>\\]+""")

    fun extractItemKey(body: String): String? = ITEM_KEY.find(body.replace("&amp;", "&"))?.groupValues?.get(1)

    fun extractActressLinks(document: Element): List<MissAvActressLink> =
        document.select("div.text-secondary > a[href*=/actresses/]")
            .mapNotNull { a ->
                val name = a.text().trim()
                val path = toPath(a.urlOf("href"))
                if (name.isEmpty() || path.isEmpty()) null else MissAvActressLink(name, path)
            }
            .distinctBy { it.path }

    fun extractMagnet(document: Element, body: String): String? {
        document.selectFirst("a[href^=magnet:]")?.attr("href")?.takeIf { it.isNotBlank() }?.let { return it }
        return MAGNET.find(body.replace("&amp;", "&"))?.value
    }

    fun extractActressId(body: String): String? =
        ACTRESS_ID_API.find(body)?.groupValues?.get(1)
            ?: ACTRESS_ID_IMG.find(body)?.groupValues?.get(1)

    fun parseActressProfile(body: String): MissAvActressProfile? {
        val document = parse(body)
        val hero = document.selectFirst("div.hero-pattern")
            ?: document.selectFirst("h4")?.parent()?.parent()
            ?: return null
        val name = hero.selectFirst("h4")?.text()?.trim()?.takeIf { it.isNotEmpty() }
        val img = hero.selectFirst("img")
        val avatar = img?.let { it.urlOf("src").ifBlank { it.urlOf("data-src") } }?.takeIf { it.isNotBlank() }
        val details = hero.select("p").map { it.text().replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotEmpty() }
        if (name == null && avatar == null && details.isEmpty()) return null
        return MissAvActressProfile(name, avatar, details)
    }

    fun parseTweets(json: String): MissAvTweetsPage {
        val root = JSONObject(json)
        val arr = root.optJSONArray("data")
        val items = mutableListOf<MissAvTweet>()
        if (arr != null) for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val media = mutableListOf<String>()
            val m = o.optJSONArray("media")
            if (m != null) for (k in 0 until m.length()) {
                val v = m.opt(k)
                val url = when (v) {
                    is String -> v
                    is JSONObject -> listOf("url", "media_url_https", "media_url", "src", "image_url", "preview_image_url", "thumbnail")
                        .firstNotNullOfOrNull { key -> v.optString(key).takeIf { it.startsWith("http") } }
                    else -> null
                }
                if (!url.isNullOrBlank()) media += url
            }
            items += MissAvTweet(
                id = o.opt("id")?.toString() ?: "$i",
                text = o.optString("text"),
                createdAt = o.optString("created_at"),
                createdAtHuman = o.optString("created_at_for_humans"),
                media = media,
            )
        }
        val next = if (root.isNull("next_cursor")) null else root.optString("next_cursor").takeIf { it.isNotBlank() }
        return MissAvTweetsPage(items, next)
    }

    fun parseVideoList(body: String): MissAvListPage<HanimeInfo> {
        val document = parse(body)
        var items = document.select("div.thumbnail").mapNotNull { card ->
            val info = MissAvParser.parseVideoCardPublic(card)
            info.takeIf { it.videoCode.isNotBlank() && it.coverUrl.isNotBlank() || it.title.isNotBlank() && it.videoCode.isNotBlank() }
        }
        if (items.isEmpty()) {

            items = document.select("img[data-src*=fourhoi.com], img[src*=fourhoi.com]")
                .filter { !it.attr("data-src").plus(it.attr("src")).contains("/actress/") }
                .mapNotNull { img ->
                    val a = img.parents().firstOrNull { it.tagName() == "a" && it.hasAttr("href") }
                        ?: return@mapNotNull null
                    val code = a.attr("href").substringBefore('?').trimEnd('/').substringAfterLast('/')
                    if (code.isBlank()) return@mapNotNull null
                    HanimeInfo(
                        title = img.attr("alt").ifBlank { a.text() }.ifBlank { code },
                        coverUrl = img.urlOf("data-src").ifBlank { img.urlOf("src") },
                        videoCode = code,
                        itemType = HanimeInfo.NORMAL,
                    )
                }
        }
        return MissAvListPage(items.distinctBy { it.videoCode }, hasNext(document))
    }

    fun parseActresses(body: String): MissAvListPage<MissAvActress> {
        val document = parse(body)
        val list = document.select("a[href*=/actresses/]").mapNotNull { a ->
            val img = a.selectFirst("img") ?: return@mapNotNull null
            val src = img.urlOf("data-src").ifBlank { img.urlOf("src") }
            if (src.isBlank()) return@mapNotNull null
            val path = toPath(a.urlOf("href"))
            if (path.isEmpty() || path.endsWith("/ranking") || path.endsWith("/actresses")) return@mapNotNull null
            val name = img.attr("alt").ifBlank { a.text() }.trim()
                .ifBlank { path.substringAfterLast('/').let { decode(it) } }
            MissAvActress(
                id = ACTRESS_ID_IMG.find(src)?.groupValues?.get(1),
                name = name,
                avatarUrl = src,
                path = path,
            )
        }.distinctBy { it.path }
        return MissAvListPage(list, hasNext(document))
    }

    fun parsePlaylists(body: String): List<MissAvPlaylist> {
        val document = parse(body)
        val seen = LinkedHashSet<String>()
        val result = mutableListOf<MissAvPlaylist>()
        document.select("a[href*=/playlists/]").forEach { a ->
            val href = a.urlOf("href")
            val key = PLAYLIST_KEY.find(href)?.groupValues?.get(1) ?: return@forEach
            if (key == "create" || !seen.add(key)) return@forEach
            val img = a.selectFirst("img")
            val lines = a.select("*").map { it.ownText().trim() }.filter { it.isNotEmpty() }
            val name = (a.selectFirst("h1,h2,h3,h4,h5,h6,.font-medium,.font-semibold")?.text()
                ?: lines.firstOrNull() ?: a.text()).trim().ifBlank { key }
            val info = lines.firstOrNull { it != name }
            result += MissAvPlaylist(
                key = key,
                name = name,
                coverUrl = img?.let { it.urlOf("data-src").ifBlank { it.urlOf("src") } }?.takeIf { it.isNotBlank() },
                info = info,
            )
        }
        return result
    }

    fun parsePlaylistTitle(body: String): String? {
        val d = parse(body)
        return d.selectFirst("h1")?.text()?.takeIf { it.isNotBlank() }
    }

    fun hasNext(document: Document): Boolean =
        document.selectFirst("a[rel=next]") != null

    fun parseLogin(json: String): MissAvLoginResponse? =
        runCatching { HJson.decodeFromString<MissAvLoginResponse>(json) }.getOrNull()

    fun parseErrorMessage(json: String): String? = runCatching {
        val o = JSONObject(json)
        val errors = o.optJSONObject("errors")
        if (errors != null) {
            val msgs = mutableListOf<String>()
            errors.keys().forEach { k ->
                val arr = errors.optJSONArray(k)
                if (arr != null) for (i in 0 until arr.length()) msgs += arr.optString(i)
            }
            if (msgs.isNotEmpty()) return@runCatching msgs.joinToString("\n")
        }
        o.optString("message").takeIf { it.isNotBlank() }
    }.getOrNull()

    fun parseViewState(json: String): MissAvSavedState =
        runCatching {
            val r = HJson.decodeFromString<MissAvViewResponse>(json)
            MissAvSavedState(loggedIn = r.user != null, saved = r.saved)
        }.getOrDefault(MissAvSavedState(loggedIn = false, saved = false))

    fun parsePlaylistOptions(json: String): List<MissAvPlaylistOption> =
        runCatching {
            HJson.decodeFromString<MissAvPlaylistOptionsResponse>(json).data
                .map { MissAvPlaylistOption(it.key, it.name, it.isAdded) }
        }.getOrDefault(emptyList())

    data class FormSpec(val action: String, val fields: Map<String, String>, val nameField: String)

    fun parseCreatePlaylistForm(body: String, pageUrl: String): FormSpec? {
        val document = Jsoup.parse(body, pageUrl)
        val nameInput = document.selectFirst("form input[name=name]") ?: return null
        val form = nameInput.parents().firstOrNull { it.tagName() == "form" } ?: return null
        val fields = linkedMapOf<String, String>()
        form.select("input[name], textarea[name], select[name]").forEach { el ->
            val n = el.attr("name")
            when {
                el.tagName() == "textarea" -> fields[n] = el.text()
                el.tagName() == "select" ->
                    fields[n] = el.selectFirst("option[selected]")?.attr("value")
                        ?: el.selectFirst("option")?.attr("value") ?: EMPTY_STRING
                el.attr("type") in setOf("checkbox", "radio") ->
                    if (el.hasAttr("checked")) fields[n] = el.attr("value").ifBlank { "on" }
                el.attr("type") in setOf("submit", "button", "file") -> Unit
                else -> fields[n] = el.attr("value")
            }
        }
        val action = form.urlOf("action").ifBlank { pageUrl }
        return FormSpec(action, fields, "name")
    }

    private fun parse(body: String): Document = Jsoup.parse(body, Preferences.missAvBaseUrl)

    private fun Element.urlOf(attr: String): String = absUrl(attr).ifBlank { attr(attr) }

    fun toPath(url: String): String = runCatching {
        if (url.isBlank()) return@runCatching ""
        val u = URI(url.replace(" ", "%20"))
        val p = u.rawPath ?: ""
        p.takeIf { it.isNotBlank() }?.let { decode(it) }?.let { if (it.startsWith("/")) it else "/$it" } ?: ""
    }.getOrDefault("")

    private fun decode(s: String) = runCatching { java.net.URLDecoder.decode(s.replace("+", "%2B"), "UTF-8") }.getOrDefault(s)
}
