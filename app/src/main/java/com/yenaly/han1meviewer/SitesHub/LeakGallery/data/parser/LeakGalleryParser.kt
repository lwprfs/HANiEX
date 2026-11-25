package com.yenaly.han1meviewer.SitesHub.LeakGallery.data.parser

import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryConstants
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryCard
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryDetail
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryListPage
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryTerm
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup

object LeakGalleryParser {

    private val VIDEO_EXT = setOf("mp4", "webm", "mov", "m4v", "mkv")

    fun appToken(html: String): String? {
        runCatching {
            val data = Jsoup.parse(html).selectFirst("script#ng-state")?.data().orEmpty()
            if (data.isNotBlank()) {
                val token = JSONObject(data).optString("APP_TOKEN")
                if (token.isNotBlank()) return token
            }
        }
        return Regex(""""APP_TOKEN"\s*:\s*"([^"]+)"""").find(html)?.groupValues?.get(1)
    }

    private fun JSONObject.str(key: String): String =
        if (isNull(key)) "" else optString(key, "").trim()

    private fun JSONObject.flag(key: String, default: Boolean): Boolean {
        if (!has(key) || isNull(key)) return default
        return when (val v = opt(key)) {
            is Boolean -> v
            is Number -> v.toInt() != 0
            is String -> v.equals("true", true) || v == "1"
            else -> default
        }
    }

    private fun isVideoPath(path: String): Boolean =
        path.substringBefore('?').substringAfterLast('.', "").lowercase() in VIDEO_EXT

    private fun profileOf(obj: JSONObject): LeakGalleryTerm? {
        val username = obj.str("username")
        if (username.isEmpty()) return null
        return LeakGalleryTerm(
            slug = username,
            name = username,
            thumbUrl = LeakGalleryConstants.cdn(obj.str("profile_pic")),
        )
    }

    private fun mediaOf(obj: JSONObject): LeakGalleryCard? {
        val id = if (obj.isNull("id")) "" else obj.opt("id")?.toString().orEmpty().trim()
        if (id.isEmpty()) return null
        val file = obj.str("file_path")
        val thumbRaw = obj.str("thumbnail_path").ifBlank { obj.str("thumbnail") }
        if (file.isEmpty() && thumbRaw.isEmpty()) return null
        val isVideo = obj.flag("is_video", isVideoPath(file))
        val thumb = thumbRaw.ifBlank { if (isVideo) "" else file }
        val username = obj.optJSONObject("profile")?.str("username").orEmpty()
            .ifBlank { obj.str("username") }
        return LeakGalleryCard(
            slug = id,
            title = obj.str("caption_title").ifBlank { obj.str("title") }.ifBlank { "Media $id" },
            coverUrl = LeakGalleryConstants.cdn(thumb),
            username = username,
            isVideo = isVideo,
            durationSec = if (obj.isNull("duration")) 0 else obj.optInt("duration", 0),
        )
    }

    private fun walk(
        node: Any?,
        cards: MutableList<LeakGalleryCard>,
        terms: MutableList<LeakGalleryTerm>,
        wantMedia: Boolean,
        wantTerms: Boolean,
        depth: Int = 0,
    ) {
        if (depth > 8) return
        when (node) {
            is JSONArray -> for (i in 0 until node.length()) {
                walk(node.opt(i), cards, terms, wantMedia, wantTerms, depth + 1)
            }
            is JSONObject -> {
                mediaOf(node)?.let { if (wantMedia) cards += it; return }
                if (node.has("username") && (node.has("profile_pic") || node.has("banner_pic"))) {
                    if (wantTerms) profileOf(node)?.let { terms += it }
                    return
                }
                val keys = node.keys()
                while (keys.hasNext()) {
                    walk(node.opt(keys.next()), cards, terms, wantMedia, wantTerms, depth + 1)
                }
            }
        }
    }

    fun parseList(json: String, wantMedia: Boolean = true, wantTerms: Boolean = false): LeakGalleryListPage {
        val root: Any = runCatching { JSONObject(json) }.getOrNull()
            ?: runCatching { JSONArray(json) }.getOrNull()
            ?: return LeakGalleryListPage(emptyList(), emptyList())
        val cards = ArrayList<LeakGalleryCard>()
        val terms = ArrayList<LeakGalleryTerm>()
        walk(root, cards, terms, wantMedia, wantTerms)
        return LeakGalleryListPage(
            cards = cards.distinctBy { it.slug },
            terms = terms.distinctBy { it.slug },
        )
    }

    fun parseDetail(json: String, slug: String): LeakGalleryDetail? {
        val o = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val id = if (o.isNull("id")) slug else o.opt("id")?.toString().orEmpty().ifBlank { slug }
        val file = o.str("file_path")
        if (file.isEmpty()) return null
        val isVideo = o.flag("is_video", isVideoPath(file))
        val mediaUrl = LeakGalleryConstants.cdn(file)
        val thumb = LeakGalleryConstants.cdn(o.str("thumbnail_path"))
        val creator = o.optJSONObject("profile")?.let { profileOf(it) }

        val tags = ArrayList<String>()
        o.optJSONArray("tags")?.let { arr ->
            for (i in 0 until arr.length()) {
                val t = arr.opt(i)
                val s = when (t) {
                    is JSONObject -> t.str("slug")
                    is String -> t
                    else -> ""
                }
                if (s.isNotBlank()) tags += s
            }
        }

        val similar = ArrayList<LeakGalleryCard>()
        val similarCreators = ArrayList<LeakGalleryTerm>()
        walk(o.opt("suggested_media"), similar, ArrayList(), wantMedia = true, wantTerms = false)
        walk(o.opt("similar"), ArrayList(), similarCreators, wantMedia = false, wantTerms = true)

        return LeakGalleryDetail(
            slug = id,
            title = o.str("caption_title").ifBlank { o.str("title") }.ifBlank { "Media $id" },
            pageUrl = LeakGalleryConstants.webUrl(creator?.slug.orEmpty(), id),
            coverUrl = thumb.ifBlank { if (isVideo) creator?.thumbUrl.orEmpty() else mediaUrl },
            mediaUrl = mediaUrl,
            isVideo = isVideo,
            durationSec = if (o.isNull("duration")) 0 else o.optInt("duration", 0),
            creator = creator,
            tags = tags.distinct(),
            similar = similar.distinctBy { it.slug }.filter { it.slug != id },
            similarCreators = similarCreators.distinctBy { it.slug }
                .filter { it.slug != creator?.slug },
            caption = o.str("caption_description"),
            postedAt = o.str("added"),
            views = if (o.isNull("views_count")) 0L else o.optLong("views_count", 0L),
            likes = if (o.isNull("likes_count")) 0L else o.optLong("likes_count", 0L),
        )
    }
}
