package com.yenaly.han1meviewer.XHamster.data.remote

import com.yenaly.han1meviewer.XHamster.common.XHamsterConstants
import com.yenaly.han1meviewer.XHamster.common.XHamsterSession
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShort
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShortComments
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShortMeta
import com.yenaly.han1meviewer.XHamster.data.parser.XHamsterMomentsParser
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

/** Site-wide short-video / GIF feeds plus the per-short actions (comments, love, dislike). */
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

    private fun run(request: Request): String =
        XHamsterNetwork.sharedOkHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body.string()
        }

    suspend fun feed(kind: Feed, page: Int, timeRange: String = "weekly"): Result<FeedPage> =
        withContext(Dispatchers.IO) {
            runCatching {
                val chain = System.currentTimeMillis() / 1000
                val url = when (kind) {
                    Feed.SHORTS -> "${base()}/api/v1/moments/?type=index&env=mobile&currentChain=" +
                            "&page=$page&chainId=$chain&pageType=mainPage&checkPrivileges=1"

                    Feed.GIFS -> "${base()}/api/v1/moments-by-id-list?type=gifs-best-ranking&env=mobile" +
                            "&currentChain=&chainId=$chain&isWithSettings=1&pageType=gifsBestPage" +
                            "&order=rated&time-range=$timeRange&page=$page&checkPrivileges=1"
                }
                val referer = if (kind == Feed.GIFS) "${base()}/gifs" else "${base()}/shorts"
                val body = run(builder(url, referer).get().build())
                val parsed = XHamsterMomentsParser.parse(body)
                FeedPage(parsed.items, parsed.hasNext && parsed.items.isNotEmpty())
            }
        }

    suspend fun comments(videoId: Long, cursor: String?, referer: String): Result<XHamsterShortComments> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = buildJsonArray {
                    add(buildJsonObject {
                        put("name", "entityCommentCollectionByCursorFetch")
                        put("requestData", buildJsonObject {
                            if (cursor == null) put("cursor", JsonNull) else put("cursor", cursor)
                            put("count", 10)
                            put("entity", buildJsonObject {
                                put("entityModel", "videoModel")
                                put("entityID", videoId)
                            })
                        })
                    })
                }.toString()
                val url = "${base()}/x-api?r=${URLEncoder.encode(request, "UTF-8")}&_=${System.currentTimeMillis()}"
                XHamsterMomentsParser.parseComments(run(builder(url, referer).get().build()))
            }
        }

    private fun requireLogin() {
        if (!XHamsterNetwork.hasAuthCookies()) throw IOException("LOGIN_REQUIRED")
    }

    /** like = true → thumbs up, false → thumbs down. */
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

    /** "Love": add the short to / remove it from the user's default favourites playlist. */
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
                        put("moment", buildJsonObject {
                            put("watched_time", 1)
                            put("referrer_block", "mono")
                        })
                    })
                }.toString()
                run(builder(pageUrl, pageUrl).post(payload.toRequestBody(TEXT_TYPE)).build())
                Unit
            }
        }

    fun reportUrl(pageUrl: String): String =
        "${base()}/info/contact?subject=abuse&content=video&source=" +
                URLEncoder.encode(pageUrl, "UTF-8")
}
