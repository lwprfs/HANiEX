package com.yenaly.han1meviewer.RouVideo.data.remote

import com.yenaly.han1meviewer.RouVideo.common.RouVideoConstants
import com.yenaly.han1meviewer.RouVideo.common.RouVideoConstants.FavoriteOrder
import com.yenaly.han1meviewer.RouVideo.data.model.RouVideoFollowsPage
import com.yenaly.han1meviewer.RouVideo.data.model.RouVideoListPage
import com.yenaly.han1meviewer.RouVideo.data.model.RouVideoSessionCheck
import com.yenaly.han1meviewer.RouVideo.data.model.RouVideoUser
import com.yenaly.han1meviewer.RouVideo.data.model.RouVideoViewerState
import com.yenaly.han1meviewer.RouVideo.data.parser.RouVideoAccountParser
import com.yenaly.han1meviewer.RouVideo.data.parser.RouVideoLiteral
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class RouVideoNotSignedInException : IOException("Not signed in")

object RouVideoAccountRepo {

    private val JSON_TYPE = "application/json".toMediaType()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private class Reply(val code: Int, val body: String)

    private suspend fun fetchHtml(url: String): String = withContext(Dispatchers.IO) {
        val response = RouVideoNetwork.service.get(url)
        if (response.code() == 401 || response.code() == 403) throw RouVideoNotSignedInException()
        if (!response.isSuccessful) throw IOException("HTTP ${response.code()}")
        response.body()?.string().orEmpty()
    }

    private suspend fun send(
        url: String,
        method: String,
        body: String? = null,
    ): Reply = withContext(Dispatchers.IO) {
        val payload = (body ?: "").toRequestBody(JSON_TYPE)
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "*/*")
            .header("Origin", RouVideoConstants.ORIGIN)
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post(payload)
            "PUT" -> builder.put(payload)
            else -> error("Unsupported method $method")
        }
        RouVideoNetwork.sharedOkHttpClient.newCall(builder.build()).execute().use {
            Reply(it.code, it.body.string())
        }
    }

    private fun Reply.requireOk() {
        if (code == 401 || code == 403) throw RouVideoNotSignedInException()
        if (code !in 200..299) throw IOException("HTTP $code")
    }

    private fun parseObject(text: String): JsonObject? =
        runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()

    suspend fun checkSession(): RouVideoSessionCheck {
        val user: RouVideoUser? = try {
            val html = fetchHtml(RouVideoConstants.homeUrl())
            RouVideoAccountParser.parseSessionUser(html)
                ?: RouVideoAccountParser.parseUserIdFromLinks(html)?.let { RouVideoUser(id = it) }
        } catch (e: RouVideoNotSignedInException) {
            return RouVideoSessionCheck.LoggedOut
        } catch (_: Exception) {
            null
        }
        if (user != null) return RouVideoSessionCheck.LoggedIn(user)

        return try {
            val reply = send(RouVideoConstants.followIdsApiUrl(), "GET")
            when {
                reply.code == 401 || reply.code == 403 -> RouVideoSessionCheck.LoggedOut
                reply.code in 200..299 -> RouVideoSessionCheck.LoggedIn(null)
                else -> RouVideoSessionCheck.Unknown(IOException("HTTP ${reply.code}"))
            }
        } catch (e: Exception) {
            RouVideoSessionCheck.Unknown(e)
        }
    }

    suspend fun signOutRemote() {
        runCatching { send(RouVideoConstants.signOutUrl(), "GET") }
    }

    suspend fun updateProfile(user: RouVideoUser, newName: String): Result<Unit> = runCatching {
        val body = buildJsonObject {
            put("profile", buildJsonObject {
                put("id", user.id)
                put("name", newName)
                put("email", user.email)
            })
        }.toString()
        val reply = send(RouVideoConstants.profileApiUrl(user.id), "PUT", body)
        if (reply.code == 401 || reply.code == 403) throw RouVideoNotSignedInException()
        if (reply.code != 200) {
            val message = parseObject(reply.body)?.get("error")?.let { it as? JsonPrimitive }
                ?.contentOrNull
            throw IOException(message ?: "HTTP ${reply.code}")
        }
    }

    private fun requireSignedIn(html: String) {
        val signedOut = RouVideoLiteral.allLoaderObjects(html).any {
            (it["signedOut"] as? JsonPrimitive)?.booleanOrNull == true ||
                    (it["anon"] as? JsonPrimitive)?.booleanOrNull == true
        }
        if (signedOut) throw RouVideoNotSignedInException()
    }

    suspend fun favorites(userId: String, order: FavoriteOrder, page: Int): Result<RouVideoListPage> =
        runCatching {
            val html = fetchHtml(RouVideoConstants.favoritesUrl(userId, order, page))
            requireSignedIn(html)
            withContext(Dispatchers.Default) { RouVideoAccountParser.parseFavorites(html) }
        }

    suspend fun history(userId: String, page: Int): Result<RouVideoListPage> = runCatching {
        val html = fetchHtml(RouVideoConstants.historyPageUrl(userId, page))
        requireSignedIn(html)
        withContext(Dispatchers.Default) { RouVideoAccountParser.parseHistory(html) }
    }

    suspend fun follows(onlyNew: Boolean, page: Int): Result<RouVideoFollowsPage> = runCatching {
        val html = fetchHtml(RouVideoConstants.followsUrl(onlyNew, page))
        requireSignedIn(html)
        withContext(Dispatchers.Default) { RouVideoAccountParser.parseFollows(html) }
    }

    suspend fun viewerState(videoId: String): Result<RouVideoViewerState> = runCatching {
        val reply = send(RouVideoConstants.viewerStateUrl(videoId), "GET")
        reply.requireOk()
        val obj = parseObject(reply.body)
        RouVideoViewerState(
            favorite = (obj?.get("favorite") as? JsonPrimitive)?.booleanOrNull == true,
            positionSec = (obj?.get("position") as? JsonPrimitive)?.doubleOrNull ?: 0.0,
        )
    }

    suspend fun toggleFavorite(videoId: String): Result<Unit> = runCatching {
        send(RouVideoConstants.favoriteApiUrl(videoId), "PUT").requireOk()
    }

    suspend fun saveProgress(
        videoId: String,
        positionSec: Double,
        ended: Boolean,
        first: Boolean,
    ): Result<Unit> = runCatching {
        val body = buildJsonObject {
            put("position", positionSec)
            put("ended", ended)
            put("first", first)
        }.toString()
        send(RouVideoConstants.progressApiUrl(videoId), "POST", body).requireOk()
    }

    suspend fun followedSeriesIds(): Result<Set<String>> = runCatching {
        val reply = send(RouVideoConstants.followIdsApiUrl(), "GET")
        reply.requireOk()
        (parseObject(reply.body)?.get("ids") as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            ?.toSet()
            ?: emptySet()
    }

    suspend fun setFollow(
        seriesId: String,
        followed: Boolean,
        seenEpisode: Int? = null,
    ): Result<Unit> = runCatching {
        val body = buildJsonObject {
            put("followed", followed)
            if (seenEpisode != null) put("seenEpisode", seenEpisode)
        }.toString()
        send(RouVideoConstants.seriesFollowApiUrl(seriesId), "PUT", body).requireOk()
    }

    suspend fun markSeen(seriesId: String? = null): Result<Unit> = runCatching {
        val body = buildJsonObject {
            if (seriesId != null) put("seriesId", seriesId) else put("all", true)
        }.toString()
        send(RouVideoConstants.followSeenApiUrl(), "POST", body).requireOk()
    }
}
