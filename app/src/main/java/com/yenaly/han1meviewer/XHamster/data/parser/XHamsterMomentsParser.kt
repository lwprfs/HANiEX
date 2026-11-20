package com.yenaly.han1meviewer.XHamster.data.parser

import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShort
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShortComment
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShortComments
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShortMeta
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShortTag
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterSubtitleTrack
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

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

    data class GifList(val ids: List<Long>, val hasNext: Boolean)

    private fun findArray(el: JsonElement?, key: String): JsonArray? = when (el) {
        is JsonObject -> (el[key] as? JsonArray) ?: el.values.firstNotNullOfOrNull { findArray(it, key) }
        is JsonArray -> el.firstNotNullOfOrNull { findArray(it, key) }
        else -> null
    }

    private fun findObject(el: JsonElement?, key: String): JsonObject? = when (el) {
        is JsonObject -> (el[key] as? JsonObject) ?: el.values.firstNotNullOfOrNull { findObject(it, key) }
        is JsonArray -> el.firstNotNullOfOrNull { findObject(it, key) }
        else -> null
    }

    fun parseGifList(html: String): GifList {
        val root = XHamsterParser.extractInitials(html) ?: return GifList(emptyList(), false)
        val ids = findArray(root, "videoThumbProps").orEmpty()
            .mapNotNull { (it as? JsonObject)?.l("id") }
            .distinct()
        val pg = findObject(root, "paginationProps")
        val cur = pg?.l("currentPageNumber") ?: 1L
        val last = pg?.l("lastPageNumber") ?: 1L
        return GifList(ids, cur < last)
    }

    fun parse(json: String): Page {
        val root = runCatching { Json.parseToJsonElement(json) }.getOrNull() as? JsonObject
            ?: return Page(emptyList(), false)
        val out = ArrayList<Pair<XHamsterShort, XHamsterShortMeta>>()
        root.a("items").orEmpty().forEach { el ->
            val o = el as? JsonObject ?: return@forEach
            val id = o.l("id") ?: return@forEach
            val sources = o.o("sources")
            val hls = sources?.o("hls")?.o("h264")?.s("url")
            val stdList = sources?.o("standard")?.a("h264").orEmpty().mapNotNull { it as? JsonObject }
            val autoUrl = stdList.firstOrNull { it.s("quality") == "auto" }?.s("url")
            val progressive = stdList
                .filter { it.s("quality") != "auto" }
                .maxByOrNull { it.s("quality")?.filter(Char::isDigit)?.toIntOrNull() ?: 0 }
                ?.s("url") ?: autoUrl
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
                tags = parseTags(o),
            )
            out += short to meta
        }
        return Page(out, root.b("hasNextPage") ?: false)
    }

    private val TAG_PATH = Regex("^/(tags|categories)/([^/?#]+)")
    private val ENTITY_PATH = Regex("^/(pornstars|creators|channels)/([^/?#]+)")

    private fun parseTags(o: JsonObject): List<XHamsterShortTag> {
        val seen = HashSet<String>()
        val out = ArrayList<XHamsterShortTag>()
        o.a("tags").orEmpty().forEach { el ->
            val t = el as? JsonObject ?: return@forEach
            if (t.b("isUploader") == true || t.b("isUser") == true) return@forEach
            val name = t.s("name")?.trim()?.takeIf { it.isNotEmpty() } ?: return@forEach
            val url = t.s("url") ?: return@forEach
            val raw = url.substringAfter("xhamster.com", url).substringBefore('?').substringBefore('#')
            val kind = when {
                t.b("isTag") == true -> XHamsterShortTag.Kind.TAG
                t.b("isCategory") == true -> XHamsterShortTag.Kind.CATEGORY
                t.b("isPornstar") == true || t.b("isCeleb") == true || t.b("isCreator") == true ->
                    XHamsterShortTag.Kind.PERSON
                t.b("isChannel") == true || t.b("isBrand") == true -> XHamsterShortTag.Kind.CHANNEL
                else -> return@forEach
            }
            val path = when (kind) {
                XHamsterShortTag.Kind.TAG, XHamsterShortTag.Kind.CATEGORY ->
                    TAG_PATH.find(raw)?.let { "/${it.groupValues[1]}/${it.groupValues[2]}/shorts" }
                else -> ENTITY_PATH.find(raw)?.let { "/${it.groupValues[1]}/${it.groupValues[2]}" }
            } ?: return@forEach
            if (seen.add(path)) out += XHamsterShortTag(name, path, kind)
        }
        return out.take(24)
    }

    private fun plain(html: String): String =
        androidx.core.text.HtmlCompat.fromHtml(html, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY)
            .toString().replace('\u00A0', ' ').trim()

    fun parseComments(json: String): XHamsterShortComments {
        val root = runCatching { Json.parseToJsonElement(json) }.getOrNull() as? JsonObject
            ?: return XHamsterShortComments(emptyList(), null)
        val items = root.a("items").orEmpty().mapNotNull { el ->
            val c = el as? JsonObject ?: return@mapNotNull null
            val id = c.s("id") ?: return@mapNotNull null
            val text = plain(c.s("text") ?: c.s("textHtml") ?: return@mapNotNull null)
            if (text.isBlank()) return@mapNotNull null
            if (c.b("isSpam") == true) return@mapNotNull null
            val author = c.o("author")
            val replyTo = c.s("replyToId")
            XHamsterShortComment(
                id = id,
                author = author?.s("name") ?: c.s("name") ?: "User",
                avatarUrl = author?.s("thumbUrl") ?: author?.s("avatarUrl"),
                text = text,
                likes = c.l("likes") ?: c.o("rating")?.l("likes") ?: 0L,
                createdSeconds = c.l("created"),
                isReply = replyTo != null,
                replyToId = replyTo,
                replyToName = c.s("replyToUserName"),
                authorPath = author?.s("pageUrl")?.substringAfter("xhamster.com", "")?.ifBlank { null },
                editable = c.b("isEditable") == true,
            )
        }
        val next = root.s("nextCursor")
        val denied = root["denyWriteComment"]?.let { it !is kotlinx.serialization.json.JsonNull } == true
        return XHamsterShortComments(
            items = items,
            nextCursor = next,
            total = root.l("total") ?: items.size.toLong(),
            writeDenied = denied,
        )
    }
}
