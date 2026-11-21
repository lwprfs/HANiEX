package com.yenaly.han1meviewer.RedGifs.data.remote

import android.net.Uri
import com.yenaly.han1meviewer.RedGifs.common.RedGifsConstants
import com.yenaly.han1meviewer.RedGifs.common.RedGifsSettings
import com.yenaly.han1meviewer.RedGifs.data.model.RedGifsFeedPage
import com.yenaly.han1meviewer.RedGifs.data.model.RedGifsGif
import com.yenaly.han1meviewer.RedGifs.data.model.RedGifsListPage
import com.yenaly.han1meviewer.RedGifs.data.model.RedGifsMe
import com.yenaly.han1meviewer.RedGifs.data.model.RedGifsNiche
import com.yenaly.han1meviewer.RedGifs.data.model.RedGifsUser
import com.yenaly.han1meviewer.RedGifs.data.model.arr
import com.yenaly.han1meviewer.RedGifs.data.model.asArr
import com.yenaly.han1meviewer.RedGifs.data.model.asObj
import com.yenaly.han1meviewer.RedGifs.data.model.long
import com.yenaly.han1meviewer.RedGifs.data.model.obj
import com.yenaly.han1meviewer.RedGifs.data.model.parseFeedPage
import com.yenaly.han1meviewer.RedGifs.data.model.parseGif
import com.yenaly.han1meviewer.RedGifs.data.model.parseNiche
import com.yenaly.han1meviewer.RedGifs.data.model.parseUser
import com.yenaly.han1meviewer.RedGifs.data.model.str
import com.yenaly.han1meviewer.RedGifs.data.model.strList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.ConcurrentHashMap

class RedGifsHttpException(val code: Int, message: String) : Exception(message)

sealed interface RedGifsFeedSource {
    data object ForYou : RedGifsFeedSource
    data object Trending : RedGifsFeedSource
    data object Latest : RedGifsFeedSource
    data object Popular : RedGifsFeedSource
    data object Liked : RedGifsFeedSource
    data object Mine : RedGifsFeedSource
    data class Niche(val id: String) : RedGifsFeedSource
    data class Creator(val name: String) : RedGifsFeedSource
    data class Search(val query: String) : RedGifsFeedSource

    val kind: String
        get() = when (this) {
            ForYou -> "foryou"
            Trending -> "trending"
            Latest -> "latest"
            Popular -> "popular"
            Liked -> "liked"
            Mine -> "mine"
            is Niche -> "niche"
            is Creator -> "creator"
            is Search -> "search"
        }

    val arg: String
        get() = when (this) {
            is Niche -> id
            is Creator -> name
            is Search -> query
            else -> ""
        }

    val requiresLogin: Boolean
        get() = this == ForYou || this == Liked || this == Mine || this == Popular

    companion object {
        fun from(kind: String, arg: String): RedGifsFeedSource = when (kind) {
            "foryou" -> ForYou
            "latest" -> Latest
            "popular" -> Popular
            "liked" -> Liked
            "mine" -> Mine
            "niche" -> Niche(arg)
            "creator" -> Creator(arg)
            "search" -> Search(arg)
            else -> Trending
        }
    }
}

object RedGifsRepo {

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMedia = "application/json".toMediaType()

    private val gifCache = ConcurrentHashMap<String, RedGifsGif>()
    private val userCache = ConcurrentHashMap<String, RedGifsUser>()

    @Volatile
    private var likedIds: MutableSet<String>? = null

    @Volatile
    private var followNames: MutableSet<String>? = null

    fun clearCaches() {
        likedIds = null
        followNames = null
    }

    fun cachedGif(id: String): RedGifsGif? = gifCache[id]

    fun cachedUser(name: String): RedGifsUser? = userCache[name.lowercase()]

    private fun remember(page: RedGifsFeedPage) {
        if (gifCache.size > 4000) gifCache.clear()
        if (userCache.size > 2000) userCache.clear()
        page.gifs.forEach { gifCache[it.id] = it }
        page.users.forEach { (k, v) -> userCache[k] = v }
    }

    // ------------------------------------------------------------ http

    private fun enc(s: String): String = Uri.encode(s)

    private suspend fun getJson(path: String, query: List<Pair<String, String>> = emptyList()): JsonElement =
        withContext(Dispatchers.IO) {
            val url = (RedGifsConstants.API_BASE + path).toHttpUrl().newBuilder().apply {
                query.forEach { (k, v) -> addQueryParameter(k, v) }
            }.build()
            RedGifsNetwork.client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw RedGifsHttpException(resp.code, errorText(resp.code, text))
                json.parseToJsonElement(text)
            }
        }

    private suspend fun send(method: String, path: String, body: String?) {
        withContext(Dispatchers.IO) {
            val reqBody = (body ?: if (method == "DELETE") null else "{}")?.toRequestBody(jsonMedia)
            val req = Request.Builder()
                .url(RedGifsConstants.API_BASE + path)
                .method(method, reqBody)
                .build()
            RedGifsNetwork.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw RedGifsHttpException(resp.code, errorText(resp.code, resp.body?.string().orEmpty()))
                }
            }
        }
    }

    /** "HTTP 401: <server message>" so failures can be reported precisely. */
    private fun errorText(code: Int, body: String): String {
        val msg = runCatching {
            json.parseToJsonElement(body).asObj()?.obj("error")?.str("description")
        }.getOrNull() ?: body.take(120).replace('\n', ' ')
        return if (msg.isBlank()) "HTTP $code" else "HTTP $code: $msg"
    }

    // ------------------------------------------------------------ feeds

    suspend fun feed(source: RedGifsFeedSource, page: Int): Result<RedGifsFeedPage> = runCatching {
        val p = page.toString()
        val verified = if (RedGifsSettings.verifiedOnly) listOf("verified" to "y") else emptyList()
        val (path, query) = when (source) {
            RedGifsFeedSource.ForYou ->
                "/v2/feeds/for-you" to listOf("page" to p, "count" to "50", "source" to "all")
            RedGifsFeedSource.Trending ->
                "/v2/gifs/search" to listOf("type" to "g", "order" to "trending", "count" to "30", "page" to p) + verified
            RedGifsFeedSource.Latest ->
                "/v2/gifs/search" to listOf("type" to "g", "order" to "new", "count" to "30", "page" to p) + verified
            RedGifsFeedSource.Popular ->
                "/v2/feeds/trending/popular" to listOf("page" to p, "count" to "50")
            RedGifsFeedSource.Liked ->
                "/v2/feeds/liked" to listOf("page" to p, "count" to "30", "type" to "g")
            RedGifsFeedSource.Mine ->
                "/v2/me/content" to listOf("order" to "new", "count" to "50", "type" to "g", "page" to p)
            is RedGifsFeedSource.Niche ->
                "/v2/niches/${enc(source.id)}/gifs" to listOf("count" to "30", "page" to p, "order" to "hot")
            is RedGifsFeedSource.Creator ->
                "/v2/users/${enc(source.name)}/search" to listOf("order" to "new", "count" to "40", "type" to "g", "page" to p)
            is RedGifsFeedSource.Search ->
                "/v2/gifs/search" to listOf(
                    "search_text" to source.query, "type" to "g", "order" to "trending",
                    "count" to "30", "page" to p,
                )
        }
        val root = getJson(path, query).asObj() ?: throw RedGifsHttpException(0, "bad response")
        parseFeedPage(root).also(::remember)
    }

    suspend fun gif(id: String): Result<RedGifsGif> = runCatching {
        gifCache[id]?.let { return@runCatching it }
        val root = getJson("/v2/gifs/${enc(id)}").asObj()
        val gif = root?.obj("gif")?.let(::parseGif) ?: throw RedGifsHttpException(404, "not found")
        root.obj("user")?.let(::parseUser)?.let { userCache[it.username.lowercase()] = it }
        gifCache[gif.id] = gif
        gif
    }

    // ------------------------------------------------------------ users / niches

    suspend fun user(name: String): Result<RedGifsUser> = runCatching {
        val o = getJson("/v1/users/${enc(name)}").asObj()
        val user = o?.let(::parseUser) ?: throw RedGifsHttpException(404, "not found")
        userCache[user.username.lowercase()] = user
        user
    }

    suspend fun creators(order: String, page: Int, query: String? = null): Result<RedGifsListPage<RedGifsUser>> =
        runCatching {
            val q = mutableListOf("order" to order, "page" to page.toString(), "count" to "30")
            if (query.isNullOrBlank()) {
                if (RedGifsSettings.verifiedOnly) q += "verified" to "y"
            } else {
                q += "search_text" to query
            }
            val root = getJson("/v2/creators/search/previews", q).asObj() ?: throw RedGifsHttpException(0, "bad response")
            val users = root.arr("users")?.mapNotNull { it.asObj()?.let(::parseUser) }.orEmpty()
            users.forEach { userCache[it.username.lowercase()] = it }
            RedGifsListPage(users, root.long("page").toInt().coerceAtLeast(1), root.long("pages").toInt().coerceAtLeast(1))
        }

    suspend fun niches(order: String, page: Int, category: String?, query: String? = null): Result<RedGifsListPage<RedGifsNiche>> =
        runCatching {
            val q = mutableListOf("order" to order, "page" to page.toString(), "count" to "30")
            if (!category.isNullOrBlank()) q += "category" to category
            if (!query.isNullOrBlank()) q += "search_text" to query
            val root = getJson("/v2/niches/search/previews", q).asObj() ?: throw RedGifsHttpException(0, "bad response")
            val niches = root.arr("previews")?.mapNotNull { it.asObj()?.obj("niche")?.let(::parseNiche) }.orEmpty()
            RedGifsListPage(niches, root.long("page").toInt().coerceAtLeast(1), root.long("pages").toInt().coerceAtLeast(1))
        }

    suspend fun nicheCategories(): Result<List<String>> = runCatching {
        getJson("/v2/niches/categories").asObj()?.strList("categories").orEmpty()
    }

    suspend fun followedNiches(): Result<List<RedGifsNiche>> = runCatching {
        getJson("/v2/niches/following").asObj()?.arr("niches")?.mapNotNull { it.asObj()?.let(::parseNiche) }.orEmpty()
    }

    suspend fun myFollowing(page: Int): Result<RedGifsListPage<RedGifsUser>> = userList("/v2/me/following", page)

    suspend fun myFollowers(page: Int): Result<RedGifsListPage<RedGifsUser>> = userList("/v2/me/followers", page)

    private suspend fun userList(path: String, page: Int): Result<RedGifsListPage<RedGifsUser>> = runCatching {
        val root = getJson(path, listOf("count" to "100", "page" to page.toString())).asObj()
            ?: throw RedGifsHttpException(0, "bad response")
        val users = root.arr("items")?.mapNotNull { it.asObj()?.let(::parseUser) }.orEmpty()
        users.forEach { userCache[it.username.lowercase()] = it }
        RedGifsListPage(users, root.long("page").toInt().coerceAtLeast(1), root.long("pages").toInt().coerceAtLeast(1))
    }

    // ------------------------------------------------------------ me

    suspend fun me(): Result<RedGifsMe> = runCatching {
        val o: JsonObject = getJson("/v1/me").asObj() ?: throw RedGifsHttpException(0, "bad response")
        val user = parseUser(o) ?: throw RedGifsHttpException(0, "bad response")
        RedGifsAuth.cacheProfile(user.username, user.avatarUrl)
        RedGifsMe(
            user = user,
            email = o.str("email"),
            preferences = o.strList("preferences"),
            blockedTags = o.strList("blockedTags"),
        )
    }

    suspend fun updatePreferences(preferences: List<String>, blockedTags: List<String>): Result<Unit> = runCatching {
        fun jsonArr(values: List<String>) = values.joinToString(",", "[", "]") { "\"" + it.replace("\\", "\\\\").replace("\"", "\\\"") + "\"" }
        val body = """{"operations":[{"op":"add","path":"/preferences","value":${jsonArr(preferences)}},""" +
                """{"op":"add","path":"/blocked_tags","value":${jsonArr(blockedTags)}}]}"""
        send("PATCH", "/v1/me", body)
    }

    // ------------------------------------------------------------ likes / follows

    /** `GET /v2/likes`: tolerant of both a list of ids and a list of objects. */
    suspend fun likedIds(): Set<String> {
        likedIds?.let { return it }
        if (!RedGifsAuth.isLoggedIn) return emptySet()
        val set = runCatching {
            getJson("/v2/likes").asArr()?.mapNotNull { e ->
                when (e) {
                    is JsonPrimitive -> e.contentOrNull
                    is JsonObject -> e.str("id") ?: e.str("gifId") ?: e.str("gif_id")
                    else -> null
                }
            }.orEmpty().toMutableSet()
        }.getOrNull() ?: return emptySet()
        likedIds = set
        return set
    }

    suspend fun followingNames(): Set<String> {
        followNames?.let { return it }
        if (!RedGifsAuth.isLoggedIn) return emptySet()
        val set = runCatching {
            getJson("/v1/me/follows").asArr()?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.lowercase() }
                .orEmpty().toMutableSet()
        }.getOrNull() ?: return emptySet()
        followNames = set
        return set
    }

    fun isFollowingCached(name: String): Boolean = followNames?.contains(name.lowercase()) == true

    suspend fun like(id: String): Result<Unit> = runCatching {
        send("PUT", "/v2/gifs/${enc(id)}/like", """{"context":null,"source":"explore-gifs","source_id":null,"position":0}""")
        likedIds?.add(id)
        Unit
    }

    /** Not present in the HAR; mirrors the like endpoint with DELETE. */
    suspend fun unlike(id: String): Result<Unit> = runCatching {
        send("DELETE", "/v2/gifs/${enc(id)}/like", null)
        likedIds?.remove(id)
        Unit
    }

    suspend fun setFollowing(name: String, follow: Boolean): Result<Unit> = runCatching {
        if (follow) {
            send("PUT", "/v1/me/follows/${enc(name)}", """{"source":"explore-gifs","source_id":null,"position":0}""")
            followNames?.add(name.lowercase())
        } else {
            send("DELETE", "/v1/me/follows/${enc(name)}", null)
            followNames?.remove(name.lowercase())
        }
        Unit
    }
}
