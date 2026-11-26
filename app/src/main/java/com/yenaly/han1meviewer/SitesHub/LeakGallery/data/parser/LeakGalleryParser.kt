package com.yenaly.han1meviewer.SitesHub.LeakGallery.data.parser

import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryConstants
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryCard
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryDetail
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryListPage
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryMe
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryNotification
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryPremium
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryProfileInfo
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryShort
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryShortsPage
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
            id = obj.longOr("id"),
            subscribed = obj.flag("isSubscribe", obj.flag("is_subscribed", obj.flag("subscribed", false))),
        )
    }

    private fun JSONObject.longOr(key: String, default: Long = 0L): Long {
        if (!has(key) || isNull(key)) return default
        return when (val v = opt(key)) {
            is Number -> v.toLong()
            is String -> v.trim().toLongOrNull() ?: default
            else -> default
        }
    }

    private fun parseRoot(json: String): Any? =
        runCatching { JSONObject(json) }.getOrNull() ?: runCatching { JSONArray(json) }.getOrNull()

    /** Finds every object in [node] (depth-first) for which [pick] returns non-null. */
    private fun <T : Any> collect(node: Any?, out: MutableList<T>, depth: Int = 0, pick: (JSONObject) -> T?) {
        if (depth > 8) return
        when (node) {
            is JSONArray -> for (i in 0 until node.length()) collect(node.opt(i), out, depth + 1, pick)
            is JSONObject -> {
                val hit = pick(node)
                if (hit != null) {
                    out += hit
                    return
                }
                val keys = node.keys()
                while (keys.hasNext()) collect(node.opt(keys.next()), out, depth + 1, pick)
            }
        }
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
            liked = o.flag("liked", false),
        )
    }

    // ---- account ----------------------------------------------------------------------

    /** `auth/login`, `auth/register`, `auth/login/2fa` answer with a token somewhere in the body. */
    fun parseToken(json: String): String? {
        val o = runCatching { JSONObject(json) }.getOrNull() ?: return null
        o.str("token").takeIf { it.isNotBlank() }?.let { return it }
        return o.optJSONObject("data")?.str("token")?.takeIf { it.isNotBlank() }
    }

    fun requires2fa(json: String): Boolean =
        runCatching { JSONObject(json).flag("requires2FA", false) }.getOrDefault(false)

    fun errorCode(json: String): String? {
        val o = runCatching { JSONObject(json) }.getOrNull() ?: return null
        return o.str("error").ifBlank { o.str("code") }.takeIf { it.isNotBlank() }
    }

    fun errorMessage(json: String): String? {
        val o = runCatching { JSONObject(json) }.getOrNull() ?: return null
        return o.str("message").ifBlank { o.str("error") }.takeIf { it.isNotBlank() }
    }

    fun errorMethod(json: String): String? =
        runCatching { JSONObject(json).str("method") }.getOrNull()?.takeIf { it.isNotBlank() }

    fun parseMe(json: String): LeakGalleryMe? {
        val o = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val name = o.str("username")
        if (name.isEmpty()) return null
        return LeakGalleryMe(
            id = o.longOr("id"),
            username = name,
            email = o.str("email"),
            avatarUrl = LeakGalleryConstants.cdn(o.str("profile_pic")),
            createdAt = o.str("created_at"),
            language = o.str("language"),
            ageVerified = o.flag("verified_age", false),
            manager = o.flag("manager", false),
            hasUploads = o.flag("has_uploads", false),
            banned = o.flag("banned", false),
        )
    }

    fun parsePremium(json: String): LeakGalleryPremium? {
        val o = runCatching { JSONObject(json) }.getOrNull() ?: return null
        return LeakGalleryPremium(
            active = o.flag("active", false),
            until = o.str("until"),
            daysRemaining = if (o.isNull("days_remaining")) null else o.optInt("days_remaining"),
        )
    }

    fun parseCount(json: String): Int =
        runCatching { JSONObject(json).optInt("count", 0) }.getOrDefault(0)

    /** Numeric fields of `user/statistics` as (key, value), in the order the API sent them. */
    fun parseStatistics(json: String): List<Pair<String, Long>> {
        val o = runCatching { JSONObject(json) }.getOrNull() ?: return emptyList()
        val out = ArrayList<Pair<String, Long>>()
        val keys = o.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = o.opt(k)
            if (v is Number) out += k to v.toLong()
        }
        return out
    }

    fun parseLikes(json: String): Pair<Long, Boolean?>? {
        val o = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val likes = if (o.has("likes")) o.longOr("likes") else o.longOr("likes_count", -1L)
        if (likes < 0) return null
        val liked = if (o.has("liked")) o.flag("liked", false) else null
        return likes to liked
    }

    // ---- profiles ---------------------------------------------------------------------

    /** The profile block (id, avatar, banner, follow state) inside a `profile/<name>` response. */
    fun parseProfileInfo(json: String, username: String): LeakGalleryProfileInfo? {
        val root = parseRoot(json) ?: return null
        val found = ArrayList<LeakGalleryProfileInfo>()
        collect(root, found) { obj ->
            val name = obj.str("username")
            val id = obj.longOr("id")
            if (name.equals(username, ignoreCase = true) && id > 0L &&
                !obj.has("file_path") && !obj.has("thumbnail_path")
            ) {
                LeakGalleryProfileInfo(
                    id = id,
                    username = name,
                    avatarUrl = LeakGalleryConstants.cdn(obj.str("profile_pic")),
                    bannerUrl = LeakGalleryConstants.cdn(obj.str("banner_pic")),
                    subscribed = obj.flag("isSubscribe", obj.flag("is_subscribed", obj.flag("subscribed", false))),
                    subscribers = listOf("subscribers", "subscribers_count", "followers", "subscription_count")
                        .firstOrNull { obj.has(it) && !obj.isNull(it) }?.let { obj.longOr(it) },
                    mediaCount = listOf("media_count", "medias_count", "total_media", "total")
                        .firstOrNull { obj.has(it) && !obj.isNull(it) }?.let { obj.longOr(it) },
                )
            } else null
        }
        return found.firstOrNull()
    }

    // ---- shorts -----------------------------------------------------------------------

    private fun shortOf(obj: JSONObject): LeakGalleryShort? {
        val id = if (obj.isNull("id")) "" else obj.opt("id")?.toString().orEmpty().trim()
        if (id.isEmpty()) return null
        val file = obj.str("file_path")
        if (file.isEmpty()) return null
        if (!obj.flag("is_video", isVideoPath(file))) return null
        val profile = obj.optJSONObject("profile")
        val username = profile?.str("username").orEmpty().ifBlank { obj.str("username") }
        val thumb = obj.str("thumbnail_path").ifBlank { obj.str("thumbnail") }
        return LeakGalleryShort(
            id = id,
            title = obj.str("caption_title").ifBlank { obj.str("title") }
                .ifBlank { if (username.isNotBlank()) "@$username" else "Media $id" },
            videoUrl = LeakGalleryConstants.cdn(file),
            coverUrl = LeakGalleryConstants.cdn(thumb),
            username = username,
            profileId = profile?.longOr("id") ?: 0L,
            avatarUrl = LeakGalleryConstants.cdn(profile?.str("profile_pic").orEmpty()),
            likes = obj.longOr("likes_count"),
            liked = obj.flag("liked", false),
            views = obj.longOr("views_count"),
            subscribed = profile?.flag("isSubscribe", false) ?: false,
            durationSec = if (obj.isNull("duration")) 0 else obj.optInt("duration", 0),
        )
    }

    fun parseShorts(json: String): LeakGalleryShortsPage {
        val root = parseRoot(json) ?: return LeakGalleryShortsPage(emptyList())
        val out = ArrayList<LeakGalleryShort>()
        collect(root, out) { shortOf(it) }
        return LeakGalleryShortsPage(out.distinctBy { it.id })
    }

    fun parseShort(json: String): LeakGalleryShort? =
        runCatching { JSONObject(json) }.getOrNull()?.let { shortOf(it) }

    // ---- notifications ----------------------------------------------------------------

    fun parseNotifications(json: String): List<LeakGalleryNotification> {
        val root = parseRoot(json) ?: return emptyList()
        val out = ArrayList<LeakGalleryNotification>()
        collect(root, out) { obj ->
            val type = obj.str("type")
            if (type.isEmpty() || !obj.has("id")) return@collect null
            val text = listOf("message", "body", "text", "title", "target")
                .map { obj.str(it) }.firstOrNull { it.isNotBlank() } ?: type.replace('_', ' ')
            LeakGalleryNotification(
                id = obj.opt("id")?.toString().orEmpty(),
                type = type,
                text = text,
                createdAt = obj.str("created_at").ifBlank { obj.str("createdAt") },
                read = obj.flag("read", obj.flag("is_read", true)),
            )
        }
        return out.distinctBy { it.id }
    }
}
