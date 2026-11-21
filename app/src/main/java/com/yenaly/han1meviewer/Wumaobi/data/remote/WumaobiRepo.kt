package com.yenaly.han1meviewer.Wumaobi.data.remote

import com.yenaly.han1meviewer.Wumaobi.common.WumaobiConstants
import com.yenaly.han1meviewer.Wumaobi.data.model.WumaobiDetail
import com.yenaly.han1meviewer.Wumaobi.data.model.WumaobiEntityPage
import com.yenaly.han1meviewer.Wumaobi.data.model.WumaobiFavFolder
import com.yenaly.han1meviewer.Wumaobi.data.model.WumaobiListPage
import com.yenaly.han1meviewer.Wumaobi.data.parser.WumaobiParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import java.io.IOException

class WumaobiNotSignedInException : IOException("Not signed in")

object WumaobiRepo {

    private const val MAX_RATE_RETRIES = 3
    private const val RATE_RETRY_DELAY_MS = 3_500L

    private suspend fun fetchHtml(
        url: String,
        sort: String? = null,
        categorySort: String? = null,
    ): String = withContext(Dispatchers.IO) {
        var attempt = 0
        var result: String? = null
        while (result == null) {
            val response = WumaobiNetwork.service.get(url, sort, categorySort)
            if (response.code() == 429) {
                response.errorBody()?.close()
                if (attempt < MAX_RATE_RETRIES) {
                    attempt++
                    delay(RATE_RETRY_DELAY_MS)
                    continue
                }
                throw IOException("HTTP 429 (too many requests, try again in a few seconds)")
            }
            if (!response.isSuccessful) throw IOException("HTTP ${response.code()}")
            result = response.body()?.string().orEmpty()
        }
        result.orEmpty()
    }

    private suspend fun postForm(url: String, fields: Map<String, String>): JSONObject =
        withContext(Dispatchers.IO) {
            val base = WumaobiConstants.BASE_URL
            val response = WumaobiNetwork.service.post(
                url = url,
                referer = "$base/",
                origin = base,
                fields = fields,
            )
            val text = (if (response.isSuccessful) response.body() else response.errorBody())
                ?.string().orEmpty()
            runCatching { JSONObject(text) }.getOrElse {
                throw IOException("HTTP ${response.code()}")
            }
        }

    private fun JSONObject.requireOk(): JSONObject {
        if (optInt("code", -1) != 1) throw IOException(optString("msg").ifBlank { "Request failed" })
        return this
    }

    private fun host(): String =
        WumaobiConstants.BASE_URL.toHttpUrlOrNull()?.host ?: "wumaobi.com"

    suspend fun listing(
        path: String,
        page: Int,
        sort: String? = null,
    ): Result<WumaobiListPage> = runCatching {
        val html = fetchHtml(WumaobiConstants.pagedUrl(path, page), sort = sort)
        withContext(Dispatchers.Default) { WumaobiParser.parseListPage(html) }
    }

    suspend fun search(query: String, page: Int, sort: String? = null): Result<WumaobiListPage> =
        runCatching {
            val html = fetchHtml(WumaobiConstants.searchUrl(query, page), sort = sort)
            withContext(Dispatchers.Default) { WumaobiParser.parseListPage(html) }
        }

    suspend fun entities(
        kind: WumaobiConstants.EntityKind,
        sort: String,
        page: Int,
    ): Result<WumaobiEntityPage> = runCatching {
        val html = fetchHtml(
            WumaobiConstants.entityListUrl(kind, page),
            categorySort = sort.ifBlank { null },
        )
        withContext(Dispatchers.Default) {
            when (kind) {
                WumaobiConstants.EntityKind.TAGS -> WumaobiParser.parseCategoryPane(html, "tags")
                WumaobiConstants.EntityKind.SERIES -> WumaobiParser.parseCategoryPane(html, "series")
                WumaobiConstants.EntityKind.ACTRESS -> WumaobiParser.parseActresses(html)
            }
        }
    }

    suspend fun detail(code: String): Result<WumaobiDetail> = runCatching {
        val html = fetchHtml(WumaobiConstants.videoUrl(code))
        withContext(Dispatchers.Default) {
            WumaobiParser.parseDetail(html, code) ?: throw IOException("Could not read video page")
        }
    }

    suspend fun login(userName: String, password: String): Result<Unit> = runCatching {
        val json = postForm(
            "${WumaobiConstants.BASE_URL}/user/login",
            mapOf("user_name" to userName, "user_pwd" to password),
        ).requireOk()
        finishAuth(json)
    }

    suspend fun register(
        userName: String,
        password: String,
        mail: String,
    ): Result<Unit> = runCatching {
        val json = postForm(
            "${WumaobiConstants.BASE_URL}/user/reg",
            mapOf(
                "user_name" to userName,
                "user_pwd" to password,
                "user_pwd2" to password,
                "mail" to mail,
            ),
        ).requireOk()
        finishAuth(json)
    }

    private fun finishAuth(json: JSONObject) {
        val host = host()
        if (!WumaobiCookieJar.hasSession(host)) {
            val token = json.optString("token")
            if (token.isNotBlank()) WumaobiCookieJar.importSession(host, token)
        }
    }

    suspend fun logoutRemote() {
        runCatching { fetchHtml("${WumaobiConstants.BASE_URL}/user/logout") }
    }

    suspend fun favFolders(): Result<List<WumaobiFavFolder>> = runCatching {
        val html = fetchHtml("${WumaobiConstants.BASE_URL}/user/fav")
        if (html.contains("id=login-fm") || html.contains("id=\"login-fm\"")) {
            throw WumaobiNotSignedInException()
        }
        withContext(Dispatchers.Default) { WumaobiParser.parseFavFolders(html) }
    }

    suspend fun favVideos(name: String, page: Int): Result<WumaobiListPage> = runCatching {
        val html = fetchHtml(WumaobiConstants.favFolderUrl(name, page))
        if (html.contains("id=login-fm") || html.contains("id=\"login-fm\"")) {
            throw WumaobiNotSignedInException()
        }
        withContext(Dispatchers.Default) { WumaobiParser.parseListPage(html) }
    }

    suspend fun editFav(videoId: String, collectionIds: List<String>): Result<Unit> = runCatching {
        postForm(
            "${WumaobiConstants.BASE_URL}/user/editFav",
            mapOf("collectionIDs" to collectionIds.joinToString(","), "VidID" to videoId),
        ).requireOk()
        Unit
    }

    suspend fun createFavFolder(name: String): Result<Unit> = runCatching {
        postForm(
            "${WumaobiConstants.BASE_URL}/user/createFavCollection",
            mapOf("favCollectionName" to name),
        ).requireOk()
        Unit
    }
}
