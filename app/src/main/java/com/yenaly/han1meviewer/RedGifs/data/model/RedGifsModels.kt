package com.yenaly.han1meviewer.RedGifs.data.model

import com.yenaly.han1meviewer.RedGifs.common.RedGifsConstants
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

data class RedGifsGif(
    val id: String,
    val userName: String,
    val description: String,
    val tags: List<String>,
    val views: Long,
    val likes: Long,
    val duration: Double,
    val hasAudio: Boolean,
    val width: Int,
    val height: Int,
    val verified: Boolean,
    val hdUrl: String,
    val sdUrl: String,
    val thumbnailUrl: String,
    val posterUrl: String,
    val createDate: Long,
) {
    val pageUrl: String get() = RedGifsConstants.gifUrl(id)
}

data class RedGifsUser(
    val username: String,
    val displayName: String,
    val description: String,
    val followers: Long,
    val following: Long,
    val gifs: Long,
    val views: Long,
    val likes: Long,
    val verified: Boolean,
    val avatarUrl: String?,
    val url: String,
)

data class RedGifsNiche(
    val id: String,
    val name: String,
    val description: String,
    val gifs: Long,
    val subscribers: Long,
    val thumbnailUrl: String?,
)

data class RedGifsMe(
    val user: RedGifsUser,
    val email: String?,
    val preferences: List<String>,
    val blockedTags: List<String>,
)

data class RedGifsFeedPage(
    val gifs: List<RedGifsGif>,
    val users: Map<String, RedGifsUser>,
    val page: Int,
    val pages: Int,
) {
    val hasNext: Boolean get() = page < pages
}

data class RedGifsListPage<T>(
    val items: List<T>,
    val page: Int,
    val pages: Int,
) {
    val hasNext: Boolean get() = page < pages
}

internal fun JsonElement?.asObj(): JsonObject? = this as? JsonObject

internal fun JsonElement?.asArr(): JsonArray? = this as? JsonArray

private fun JsonObject.prim(key: String): JsonPrimitive? =
    (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }

internal fun JsonObject.str(key: String): String? = prim(key)?.contentOrNull

internal fun JsonObject.long(key: String): Long =
    prim(key)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() } ?: 0L

internal fun JsonObject.dbl(key: String): Double = prim(key)?.doubleOrNull ?: 0.0

internal fun JsonObject.bool(key: String): Boolean = prim(key)?.booleanOrNull ?: false

internal fun JsonObject.strList(key: String): List<String> =
    (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()

internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

internal fun JsonObject.arr(key: String): JsonArray? = this[key] as? JsonArray

internal fun parseGif(o: JsonObject): RedGifsGif? {
    val id = o.str("id") ?: return null
    if (o["cta"] != null && o["cta"] !is JsonNull) return null
    if (o.bool("promoted")) return null
    if (o["gallery"] != null && o["gallery"] !is JsonNull) return null
    if (o.long("type") == 2L) return null
    val urls = o.obj("urls") ?: return null
    val hd = urls.str("hd").orEmpty()
    val sd = urls.str("sd").orEmpty()
    if (hd.isBlank() && sd.isBlank()) return null
    return RedGifsGif(
        id = id,
        userName = o.str("userName").orEmpty(),
        description = o.str("description").orEmpty().trim(),
        tags = o.strList("tags"),
        views = o.long("views"),
        likes = o.long("likes"),
        duration = o.dbl("duration"),
        hasAudio = o.bool("hasAudio"),
        width = o.long("width").toInt(),
        height = o.long("height").toInt(),
        verified = o.bool("verified"),
        hdUrl = hd.ifBlank { sd },
        sdUrl = sd.ifBlank { hd },
        thumbnailUrl = urls.str("thumbnail").orEmpty().ifBlank { urls.str("poster").orEmpty() },
        posterUrl = urls.str("poster").orEmpty().ifBlank { urls.str("thumbnail").orEmpty() },
        createDate = o.long("createDate"),
    )
}

internal fun parseUser(o: JsonObject): RedGifsUser? {
    val username = o.str("username") ?: o.str("name") ?: return null
    return RedGifsUser(
        username = username,
        displayName = o.str("name").orEmpty().ifBlank { username },
        description = o.str("description").orEmpty().trim(),
        followers = o.long("followers"),
        following = o.long("following"),
        gifs = o.long("publishedGifs").takeIf { it > 0 } ?: o.long("gifs"),
        views = o.long("views"),
        likes = o.long("likes"),
        verified = o.bool("verified"),
        avatarUrl = o.str("profileImageUrl"),
        url = o.str("url") ?: RedGifsConstants.userUrl(username),
    )
}

internal fun parseNiche(o: JsonObject): RedGifsNiche? {
    val id = o.str("id") ?: return null
    return RedGifsNiche(
        id = id,
        name = o.str("name").orEmpty().ifBlank { id },
        description = o.str("description").orEmpty().trim(),
        gifs = o.long("gifs"),
        subscribers = o.long("subscribers"),
        thumbnailUrl = o.str("thumbnail") ?: o.str("cover"),
    )
}

internal fun parseFeedPage(root: JsonObject): RedGifsFeedPage {
    val gifs = root.arr("gifs")?.mapNotNull { it.asObj()?.let(::parseGif) }.orEmpty()
    val users = HashMap<String, RedGifsUser>()
    root.arr("users")?.forEach { e -> e.asObj()?.let(::parseUser)?.let { users[it.username.lowercase()] = it } }
    return RedGifsFeedPage(
        gifs = gifs,
        users = users,
        page = root.long("page").toInt().coerceAtLeast(1),
        pages = root.long("pages").toInt().coerceAtLeast(1),
    )
}
