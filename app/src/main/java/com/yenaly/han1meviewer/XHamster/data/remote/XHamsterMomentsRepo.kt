package com.yenaly.han1meviewer.XHamster.data.remote

import com.yenaly.han1meviewer.XHamster.common.XHamsterConstants
import com.yenaly.han1meviewer.XHamster.common.XHamsterSession
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterCommentResult
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShort
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShortComments
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShortMeta
import com.yenaly.han1meviewer.XHamster.data.parser.XHamsterMomentsParser
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterEngagement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.UUID

object XHamsterMomentsRepo {

    enum class Feed { SHORTS, GIFS }

    data class FeedPage(
        val items: List<Pair<XHamsterShort, XHamsterShortMeta>>,
        val hasNext: Boolean,
    )

    private val JSON_TYPE = "application/json;charset=UTF-8".toMediaType()
    private val TEXT_TYPE = "text/plain;charset=UTF-8".toMediaType()

    private fun base() = XHamsterConstants.BASE_URL

    private fun builder(url: String, referer: String): Request.Builder =
        Request.Builder().url(url)
            .header("Accept", "*/*")
            .header("Origin", XHamsterConstants.ORIGIN)
            .header("Referer", referer)
            .header("X-Requested-With", "XMLHttpRequest")
            .header("x-csrf-token", "1")

    private fun plainBuilder(url: String, referer: String): Request.Builder =
        Request.Builder().url(url)
            .header("Accept", "*/*")
            .header("Referer", referer)

    private fun runApi(url: String, referer: String): String =
        try {
            run(plainBuilder(url, referer).get().build())
        } catch (e: IOException) {
            if (e.message?.startsWith("HTTP 4") == true) {
                run(builder(url, referer).get().build())
            } else throw e
        }

    private fun run(request: Request): String =
        XHamsterNetwork.sharedOkHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body.string()
        }

    val gifRanges = listOf("daily", "weekly", "monthly", "alltime")

    private fun gifListUrl(range: String, page: Int): String {
        val path = if (range == "newest") "/gifs/newest" else "/gifs/best/${range.ifBlank { "alltime" }}"
        return base() + path + if (page > 1) "/$page" else ""
    }

    fun newChainId(): Long = System.currentTimeMillis() / 1000

    suspend fun feed(
        kind: Feed,
        page: Int,
        timeRange: String = "alltime",
        chainId: Long = newChainId(),
        currentChain: List<Long> = emptyList(),
    ): Result<FeedPage> =
        withContext(Dispatchers.IO) {
            runCatching {
                val chain = chainId
                when (kind) {
                    Feed.SHORTS -> {
                        val seen = URLEncoder.encode(currentChain.joinToString(","), "UTF-8")

                        val url = "${base()}/api/v1/moments/?type=index&env=mobile&currentChain=$seen" +
                                "&page=$page&chainId=$chain&pageType=mainPage" +
                                if (page <= 1) "&isWithSettings=1&checkPrivileges=1" else ""
                        val body = runApi(url, "${base()}/shorts")
                        val parsed = XHamsterMomentsParser.parse(body)
                        FeedPage(parsed.items, parsed.hasNext && parsed.items.isNotEmpty())
                    }

                    Feed.GIFS -> {
                        val pageUrl = gifListUrl(timeRange, page)
                        val html = run(
                            Request.Builder().url(pageUrl)
                                .header("Accept", "text/html,application/xhtml+xml,*/*")
                                .header("Referer", "${base()}/gifs").build()
                        )
                        val list = XHamsterMomentsParser.parseGifList(html)
                        if (list.ids.isEmpty()) return@runCatching FeedPage(emptyList(), false)
                        val order = if (timeRange == "newest") "added" else "rated"
                        val range = if (timeRange == "newest") "" else "&time-range=${timeRange.ifBlank { "alltime" }}"
                        val ids = URLEncoder.encode(list.ids.joinToString(","), "UTF-8")
                        val url = "${base()}/api/v1/moments-by-id-list?type=gifs-best-ranking&env=mobile" +
                                "&currentChain=&chainId=$chain&isWithSettings=1&pageType=gifsBestPage" +
                                "&order=$order$range&checkPrivileges=1&ids=$ids&xplayerVideoId=${list.ids.first()}"
                        val body = runApi(url, pageUrl)
                        val parsed = XHamsterMomentsParser.parse(body)

                        val byId = parsed.items.associateBy { it.first.id }
                        val ordered = list.ids.mapNotNull { byId[it] }.ifEmpty { parsed.items }
                        FeedPage(ordered, list.hasNext && ordered.isNotEmpty())
                    }
                }
            }
        }

    suspend fun comments(videoId: Long, cursor: String?, referer: String): Result<XHamsterShortComments> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "${base()}/api/front/comment/list-by-cursor?entityModel=videoModel" +
                        "&entityId=$videoId&spamFolder=0&cursor=${URLEncoder.encode(cursor ?: "0", "UTF-8")}" +
                        "&count=10&checkPrivileges=1"
                XHamsterMomentsParser.parseComments(run(builder(url, referer).get().build()))
            }
        }

    private val xapiJson = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun postXApi(payload: String, referer: String): String =
        XHamsterNetwork.sharedOkHttpClient.newCall(
            builder("${base()}/x-api", referer).post(payload.toRequestBody(TEXT_TYPE)).build()
        ).execute().use { it.body.string() }

    private fun syncOk(body: String): Boolean {
        val extras = ((runCatching { xapiJson.parseToJsonElement(body) }.getOrNull() as? JsonArray)
            ?.firstOrNull() as? JsonObject)?.get("extras") as? JsonObject
        val result = (extras?.get("result") as? JsonPrimitive)?.booleanOrNull
        return result != false
    }

    suspend fun postComment(
        videoId: Long,
        text: String,
        replyToId: String?,
        replyToName: String?,
        watchId: String?,
        referer: String,
    ): XHamsterCommentResult {
        val userId = XHamsterAccountRepo.currentUserId()
        return XHamsterUserRepo.postComment(
            entityModel = "videoModel",
            entityId = videoId,
            text = text,
            replyToId = replyToId,
            replyToName = replyToName,
            engagement = XHamsterEngagement(
                entityId = videoId, userId = userId, watchId = watchId,
                likes = null, dislikes = null,
                ratingState = XHamsterEngagement.RATING_NONE,
                isFavorite = false, collectionIds = emptyList(),
            ),
            referer = referer,
        )
    }

    suspend fun likeComment(videoId: Long, commentId: String, referer: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireLogin()
                if (!syncOk(postXApi(commentSync(videoId, commentId, "changed", true), referer)))
                    throw IOException("Like rejected")
            }
        }

    suspend fun deleteComment(videoId: Long, commentId: String, referer: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireLogin()
                if (!syncOk(postXApi(commentSync(videoId, commentId, "removed", false), referer)))
                    throw IOException("Delete rejected")
            }
        }

    private fun commentSync(videoId: Long, commentId: String, state: String, like: Boolean): String =
        buildJsonArray {
            add(buildJsonObject {
                put("name", "commentModelSync")
                put("requestData", buildJsonObject {
                    put("model", buildJsonObject {
                        put("id", commentId)
                        put("${'$'}id", commentId)
                        put("modelName", "commentModel")
                        put("itemState", state)
                        if (like) put("likes", true)
                    })
                    put("entity", buildJsonObject {
                        put("entityModel", "videoModel")
                        put("entityID", videoId)
                    })
                })
            })
        }.toString()

    private fun requireLogin() {
        if (!XHamsterNetwork.hasAuthCookies()) throw IOException("LOGIN_REQUIRED")
    }

    suspend fun rate(videoId: Long, like: Boolean, pageUrl: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireLogin()
                val payload = buildJsonObject {
                    put("stats", buildJsonObject {
                        put("moment", buildJsonObject {
                            put("watched_time", 1)
                            put("referrer_block", "mono")
                        })
                    })
                    put("watchId", UUID.randomUUID().toString())
                    put("statsPrevUrl", pageUrl)
                    put("entityModel", "videoModel")
                    put("entityId", videoId)
                    put("isLike", like)
                }.toString()
                run(
                    builder("${base()}/api/front/rating/rate", pageUrl)
                        .post(payload.toRequestBody(TEXT_TYPE)).build()
                )
                Unit
            }
        }

    suspend fun undoRate(videoId: Long, pageUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireLogin()
            val payload = buildJsonObject { put("videoId", videoId) }.toString()
            run(
                builder("${base()}/api/front/rating/undo-video", pageUrl)
                    .post(payload.toRequestBody(JSON_TYPE)).build()
            )
            Unit
        }
    }

    suspend fun setFavorite(videoId: Long, favorite: Boolean, pageUrl: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireLogin()
                var collection = XHamsterSession.defaultCollectionId
                if (collection.isBlank()) {
                    XHamsterPrefsRepo.load()
                    collection = XHamsterSession.defaultCollectionId
                }
                if (collection.isBlank()) throw IOException("No favourites playlist found")
                val payload = buildJsonObject {
                    put("videoId", videoId)
                    put("collections", buildJsonArray { if (favorite) add(kotlinx.serialization.json.JsonPrimitive(collection)) })
                    put("stats", buildJsonObject {
                        put("isLinkingClicked", false)
                        put("favoriteSource", "video")
                    })
                }.toString()
                run(builder("${base()}/api/front/favorite/videos", pageUrl).post(payload.toRequestBody(TEXT_TYPE)).build())
                Unit
            }
        }

    fun reportUrl(pageUrl: String): String =
        "${base()}/info/contact?subject=abuse&content=video&source=" +
                URLEncoder.encode(pageUrl, "UTF-8")
}
