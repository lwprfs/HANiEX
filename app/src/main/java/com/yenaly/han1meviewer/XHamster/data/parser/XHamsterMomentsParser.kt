package com.yenaly.han1meviewer.XHamster.data.parser

import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShort
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShortComment
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShortComments
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShortMeta
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterSubtitleTrack
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

/** Parses the `moments` / `moments-by-id-list` payloads and the comment collection. */
object XHamsterMomentsParser {

    data class Page(
        val items: List<Pair<XHamsterShort, XHamsterShortMeta>>,
        val hasNext: Boolean,
    )

    private fun JsonObject.o(key: String) = this[key] as? JsonObject
    private fun JsonObject.a(key: String) = this[key] as? JsonArray
    private fun JsonObject.s(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it != "null" }
    private fun JsonObject.l(key: String) = (this[key] as? JsonPrimitive)?.longOrNull
    private fun JsonObject.b(key: String) = (this[key] as? JsonPrimitive)?.booleanOrNull

    fun parse(json: String): Page {
        val root = runCatching { Json.parseToJsonElement(json) }.getOrNull() as? JsonObject
            ?: return Page(emptyList(), false)
        val out = ArrayList<Pair<XHamsterShort, XHamsterShortMeta>>()
        root.a("items").orEmpty().forEach { el ->
            val o = el as? JsonObject ?: return@forEach
            val id = o.l("id") ?: return@forEach
            val sources = o.o("sources")
            val hls = sources?.o("hls")?.o("h264")?.s("url")
            val progressive = sources?.o("standard")?.a("h264").orEmpty()
                .mapNotNull { it as? JsonObject }
                .filter { it.s("quality") != "auto" }
                .maxByOrNull { it.s("quality")?.filter(Char::isDigit)?.toIntOrNull() ?: 0 }
                ?.s("url")
            val rating = o.o("ratingModel")
            val state = rating?.l("state") ?: 0L
            val landing = o.o("landing")
            val subtitles = (o.o("subtitles")?.a("tracks") ?: o.a("subtitles")).orEmpty()
                .mapNotNull { t ->
                    val to = t as? JsonObject ?: return@mapNotNull null
                    val url = to.s("url") ?: to.s("src") ?: to.s("file") ?: return@mapNotNull null
                    val lang = to.s("lang") ?: to.s("language") ?: to.s("code") ?: ""
                    val label = to.s("label") ?: to.s("name") ?: to.s("title") ?: lang.ifBlank { "Subtitles" }
                    XHamsterSubtitleTrack(label = label, lang = lang, url = url)
                }
            val short = XHamsterShort(
                id = id,
                title = o.s("title").orEmpty(),
                pageUrl = o.s("pageURL").orEmpty(),
                videoUrl = hls ?: progressive.orEmpty(),
                coverUrl = o.s("posterUrl") ?: o.s("thumbUrl").orEmpty(),
                views = o.l("views"),
            )
            val meta = XHamsterShortMeta(
                streamUrl = hls ?: progressive,
                likes = rating?.l("likes") ?: 0L,
                liked = state > 0L,
                dislikes = rating?.l("dislikes") ?: 0L,
                disliked = state < 0L,
                favorite = o.b("isFavorite") ?: false,
                commentsCount = o.l("comments") ?: 0L,
                commentsDisabled = o.b("isCommentsDisabled") ?: false,
                subtitles = subtitles,
                ownerName = landing?.s("name"),
                ownerAvatar = landing?.s("logo"),
                ownerPath = landing?.s("link")?.substringAfter("xhamster.com", "")
                    ?.removeSuffix("/shorts")?.ifBlank { null },
            )
            out += short to meta
        }
        return Page(out, root.b("hasNextPage") ?: false)
    }

    private fun plain(html: String): String =
        androidx.core.text.HtmlCompat.fromHtml(html, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY)
            .toString().replace('\u00A0', ' ').trim()

    fun parseComments(json: String): XHamsterShortComments {
        val root = runCatching { Json.parseToJsonElement(json) }.getOrNull()
        val first: JsonElement = (root as? JsonArray)?.firstOrNull()
            ?: (root as? JsonObject)?.a("data")?.firstOrNull()
            ?: return XHamsterShortComments(emptyList(), null)
        val obj = first as? JsonObject ?: return XHamsterShortComments(emptyList(), null)
        val data = obj.a("responseData") ?: obj.a("data") ?: JsonArray(emptyList())
        val items = data.mapNotNull { el ->
            val c = el as? JsonObject ?: return@mapNotNull null
            val id = c.l("id") ?: return@mapNotNull null
            val text = plain(c.s("text") ?: c.s("textHtml") ?: return@mapNotNull null)
            if (text.isBlank()) return@mapNotNull null
            val author = c.o("author")
            XHamsterShortComment(
                id = id,
                author = author?.s("name") ?: c.s("name") ?: "User",
                avatarUrl = author?.s("thumbUrl") ?: author?.s("avatarUrl"),
                text = text,
                likes = c.l("likes") ?: c.o("rating")?.l("likes") ?: 0L,
                createdSeconds = c.l("created"),
                isReply = (c.l("replyToId") ?: 0L) > 0L,
            )
        }
        val next = obj.o("extras")?.s("nextCursor")
            ?: obj.o("extras")?.o("pagination")?.s("nextCursor")
        return XHamsterShortComments(items, next)
    }
}
