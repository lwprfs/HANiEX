package com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote

import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryConstants
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryDetail
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryListPage
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryLoginResult
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryMe
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryNotification
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryPremium
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryProfileInfo
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryShort
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryShortsPage
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.parser.LeakGalleryParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LeakGalleryApiException(val code: Int, val body: String) : IOException("HTTP $code")

object LeakGalleryRepo {

    @Volatile private var token: String? = null

    private val detailCache = object : LinkedHashMap<String, LeakGalleryDetail>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LeakGalleryDetail>?) = size > 12
    }

    private val shortCache = object : LinkedHashMap<String, LeakGalleryShort>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LeakGalleryShort>?) = size > 300
    }

    private val profileCache = object : LinkedHashMap<String, LeakGalleryProfileInfo>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LeakGalleryProfileInfo>?) = size > 40
    }

    fun clearToken() {
        token = null
    }

    private fun pageHeaders(builder: Request.Builder) = builder
        .header("Referer", "${LeakGalleryConstants.BASE_URL}/")
        .header("Origin", LeakGalleryConstants.BASE_URL)

    private fun loadToken(force: Boolean): String {
        if (!force) token?.let { return it }
        val request = Request.Builder()
            .url(LeakGalleryConstants.BASE_URL + "/")
            .header("Accept", "text/html,application/xhtml+xml")
            .build()
        val html = LeakGalleryNetwork.sharedOkHttpClient.newCall(request).execute().use {
            if (!it.isSuccessful) throw IOException("HTTP ${it.code}")
            it.body.string()
        }
        val fresh = LeakGalleryParser.appToken(html) ?: throw IOException("API token not found")
        token = fresh
        return fresh
    }

    private fun buildRequest(url: String, appToken: String, body: String?, method: String?): Request {
        val builder = pageHeaders(Request.Builder().url(url))
            .header("Accept", "application/json, text/plain, */*")
            .header("x-app-token", appToken)
        LeakGalleryAuth.token?.let { builder.header("Authorization", "Bearer $it") }
        val jsonBody = (body ?: if (method == "POST") "{}" else null)
            ?.toRequestBody("application/json".toMediaType())
        when (method) {
            "DELETE" -> builder.delete(jsonBody)
            "POST" -> builder.post(jsonBody!!)
            else -> if (body != null) builder.post(jsonBody!!)
        }
        return builder.build()
    }

    private suspend fun api(url: String, body: String? = null, method: String? = null): String =
        withContext(Dispatchers.IO) {
            val client = LeakGalleryNetwork.sharedOkHttpClient
            var response = client.newCall(buildRequest(url, loadToken(false), body, method)).execute()

            if ((response.code == 401 || response.code == 403) && !url.contains("/auth/")) {
                response.close()
                response = client.newCall(buildRequest(url, loadToken(true), body, method)).execute()
            }
            response.use {
                val text = it.body.string()
                if (!it.isSuccessful) throw LeakGalleryApiException(it.code, text)
                text
            }
        }

    private fun endpoint(path: String) = LeakGalleryConstants.API_URL + "/" + path.trimStart('/')

    suspend fun page(path: String, page: Int): Result<LeakGalleryListPage> = runCatching {
        val json = api(LeakGalleryConstants.apiUrl(path, page))
        val base = path.substringBefore('?').trim('/')
        val creators = base.startsWith("popular/newest-profiles") || base.startsWith("popular/profile")
        val profileName = LeakGalleryConstants.usernameFromProfilePath(path)
        withContext(Dispatchers.Default) {
            if (profileName != null) {
                LeakGalleryParser.parseProfileInfo(json, profileName)?.let {
                    synchronized(profileCache) { profileCache[profileName.lowercase()] = it }
                }
            }
            LeakGalleryParser.parseList(json, wantMedia = !creators, wantTerms = creators)
        }
    }

    suspend fun search(query: String, creators: Boolean, page: Int): Result<LeakGalleryListPage> =
        runCatching {
            val json = if (creators) {
                api(
                    LeakGalleryConstants.API_URL + "/search",
                    JSONObject().put("search", query).toString(),
                )
            } else {
                api(
                    LeakGalleryConstants.API_URL + "/search/advanced",
                    JSONObject()
                        .put("titleSearch", query)
                        .put("sort", "MostViewed")
                        .put("type", "all")
                        .put("page", page)
                        .toString(),
                )
            }
            withContext(Dispatchers.Default) {
                val result = LeakGalleryParser.parseList(json, wantMedia = !creators, wantTerms = creators)

                if (creators && page > 1) LeakGalleryListPage(emptyList(), emptyList()) else result
            }
        }

    suspend fun detail(slug: String): Result<LeakGalleryDetail> = runCatching {
        val json = api("${LeakGalleryConstants.API_URL}/media/$slug")
        withContext(Dispatchers.Default) {
            LeakGalleryParser.parseDetail(json, slug) ?: throw IOException("Could not read media")
        }
    }.onSuccess { d -> synchronized(detailCache) { detailCache[slug] = d } }

    suspend fun detailCached(slug: String): Result<LeakGalleryDetail> {
        val hit = synchronized(detailCache) { detailCache[slug] }
        return if (hit != null) Result.success(hit) else detail(slug)
    }

    suspend fun shorts(
        mode: String,
        page: Int,
        filter: String = LeakGalleryConstants.SHORTS_FILTER_RECENT,
    ): Result<LeakGalleryShortsPage> = runCatching {
        val json = api(LeakGalleryConstants.shortsApiUrl(mode, page, filter))
        withContext(Dispatchers.Default) { LeakGalleryParser.parseShorts(json) }
    }.onSuccess { p -> rememberShorts(p.shorts) }

    suspend fun shortById(id: String): Result<LeakGalleryShort> = runCatching {
        synchronized(shortCache) { shortCache[id] }?.let { return@runCatching it }
        val json = api("${LeakGalleryConstants.API_URL}/media/$id")
        withContext(Dispatchers.Default) {
            LeakGalleryParser.parseShort(json) ?: throw IOException("Not a video")
        }
    }.onSuccess { rememberShorts(listOf(it)) }

    fun cachedShort(id: String): LeakGalleryShort? = synchronized(shortCache) { shortCache[id] }

    fun updateShort(id: String, change: (LeakGalleryShort) -> LeakGalleryShort) {
        synchronized(shortCache) { shortCache[id]?.let { shortCache[id] = change(it) } }
    }

    private fun rememberShorts(items: List<LeakGalleryShort>) = synchronized(shortCache) {
        items.forEach { shortCache[it.id] = it }
    }

    suspend fun markShortViewed(id: String) {
        runCatching { api(endpoint("tiktok/vue/$id"), "{}", "POST") }
    }

    suspend fun setLiked(id: String, liked: Boolean): Result<Long?> = runCatching {
        val state = if (liked) "liked" else "deleted"
        val json = api(endpoint("likes?id=$id&liked=$state"))
        LeakGalleryParser.parseLikes(json)?.first
    }

    suspend fun profileInfo(username: String): Result<LeakGalleryProfileInfo> = runCatching {
        synchronized(profileCache) { profileCache[username.lowercase()] }?.let { return@runCatching it }
        val path = LeakGalleryConstants.profilePath(username)
        val json = api(LeakGalleryConstants.apiUrl(path, 1))
        withContext(Dispatchers.Default) {
            LeakGalleryParser.parseProfileInfo(json, username)
                ?.also { synchronized(profileCache) { profileCache[username.lowercase()] = it } }
                ?: throw IOException("Profile not found")
        }
    }

    fun cachedProfile(username: String): LeakGalleryProfileInfo? =
        synchronized(profileCache) { profileCache[username.lowercase()] }

    fun updateProfile(username: String, change: (LeakGalleryProfileInfo) -> LeakGalleryProfileInfo) {
        synchronized(profileCache) {
            profileCache[username.lowercase()]?.let { profileCache[username.lowercase()] = change(it) }
        }
    }

    fun markFollowing(username: String, follow: Boolean) {
        synchronized(shortCache) {
            shortCache.entries.toList().forEach { (id, short) ->
                if (short.username.equals(username, ignoreCase = true)) {
                    shortCache[id] = short.copy(subscribed = follow)
                }
            }
        }
        updateProfile(username) { it.copy(subscribed = follow) }
    }

    suspend fun setFollowing(profileId: Long, follow: Boolean): Result<Unit> = runCatching {
        if (follow) {
            api(
                endpoint("profile-subscription"),
                JSONObject().put("profileId", profileId).toString(),
                "POST",
            )
        } else {
            api(endpoint("profile-subscription/$profileId"), null, "DELETE")
        }
    }

    suspend fun following(page: Int = 1): Result<LeakGalleryListPage> = runCatching {
        val json = api(endpoint("profile-subscription/$page"))
        withContext(Dispatchers.Default) {
            LeakGalleryParser.parseList(json, wantMedia = false, wantTerms = true)
        }
    }

    suspend fun likedMedia(page: Int): Result<LeakGalleryListPage> = runCatching {
        val json = api(endpoint("likes/me-media-like/$page"))
        withContext(Dispatchers.Default) { LeakGalleryParser.parseList(json) }
    }

    private fun failure(e: LeakGalleryApiException): LeakGalleryLoginResult.Failure {
        val code = LeakGalleryParser.errorCode(e.body).orEmpty()
        val kind = when {
            code == "BANNED" -> LeakGalleryLoginResult.Kind.BANNED
            code == "OAUTH_ACCOUNT" -> LeakGalleryLoginResult.Kind.SOCIAL_ACCOUNT
            code == "VALIDATION_ERROR" -> LeakGalleryLoginResult.Kind.CAPTCHA
            code == "CAPTCHA_SERVICE_UNAVAILABLE" || e.code == 503 -> LeakGalleryLoginResult.Kind.UNAVAILABLE
            e.code == 429 -> LeakGalleryLoginResult.Kind.TOO_MANY
            e.code == 409 -> LeakGalleryLoginResult.Kind.EXISTS
            e.code == 401 -> LeakGalleryLoginResult.Kind.INVALID
            else -> LeakGalleryLoginResult.Kind.OTHER
        }
        val message = if (kind == LeakGalleryLoginResult.Kind.SOCIAL_ACCOUNT) {
            LeakGalleryParser.errorMethod(e.body) ?: "Google"
        } else LeakGalleryParser.errorMessage(e.body)
        return LeakGalleryLoginResult.Failure(kind, message)
    }

    private fun session(json: String): LeakGalleryLoginResult {
        val t = LeakGalleryParser.parseToken(json)
            ?: return LeakGalleryLoginResult.Failure(LeakGalleryLoginResult.Kind.OTHER)
        return if (LeakGalleryParser.requires2fa(json)) {
            LeakGalleryLoginResult.TwoFactor(t)
        } else {
            LeakGalleryAuth.saveToken(t)
            LeakGalleryLoginResult.Success(t)
        }
    }

    private suspend fun authCall(path: String, body: JSONObject): LeakGalleryLoginResult = try {
        session(api(endpoint(path), body.toString(), "POST"))
    } catch (e: LeakGalleryApiException) {
        failure(e)
    } catch (e: IOException) {
        LeakGalleryLoginResult.Failure(LeakGalleryLoginResult.Kind.OTHER, e.message)
    }

    suspend fun login(username: String, password: String, captcha: String): LeakGalleryLoginResult =
        authCall(
            "auth/login",
            JSONObject().put("username", username).put("password", password).put("token", captcha),
        )

    suspend fun register(
        username: String,
        email: String,
        password: String,
        captcha: String,
    ): LeakGalleryLoginResult = authCall(
        "auth/register",
        JSONObject()
            .put("username", username)
            .put("password", password)
            .put("language", "")
            .put("email", email)
            .put("token", captcha),
    )

    suspend fun login2fa(tempToken: String, code: String): LeakGalleryLoginResult = try {
        val json = api(
            endpoint("auth/login/2fa"),
            JSONObject().put("tempToken", tempToken).put("token", code).toString(),
            "POST",
        )
        finishSecondFactor(json)
    } catch (e: LeakGalleryApiException) {
        failure(e).let {
            if (LeakGalleryParser.errorCode(e.body) == "INVALID_CODE") {
                LeakGalleryLoginResult.Failure(LeakGalleryLoginResult.Kind.INVALID)
            } else it
        }
    } catch (e: IOException) {
        LeakGalleryLoginResult.Failure(LeakGalleryLoginResult.Kind.OTHER, e.message)
    }

    suspend fun loginBackupCode(tempToken: String, backupCode: String): LeakGalleryLoginResult = try {
        val json = api(
            endpoint("auth/backup-code"),
            JSONObject().put("tempToken", tempToken).put("backupCode", backupCode).toString(),
            "POST",
        )
        finishSecondFactor(json)
    } catch (e: LeakGalleryApiException) {
        failure(e)
    } catch (e: IOException) {
        LeakGalleryLoginResult.Failure(LeakGalleryLoginResult.Kind.OTHER, e.message)
    }

    private fun finishSecondFactor(json: String): LeakGalleryLoginResult {
        val t = LeakGalleryParser.parseToken(json)
            ?: return LeakGalleryLoginResult.Failure(LeakGalleryLoginResult.Kind.OTHER)
        LeakGalleryAuth.saveToken(t)
        return LeakGalleryLoginResult.Success(t)
    }

    suspend fun forgotPassword(email: String): Result<Unit> = runCatching {
        api(endpoint("auth/forgot-password"), JSONObject().put("email", email).toString(), "POST")
    }

    suspend fun me(): Result<LeakGalleryMe> = runCatching {
        val json = try {
            api(endpoint("user/me"))
        } catch (e: LeakGalleryApiException) {
            if (e.code == 401) LeakGalleryAuth.clear()
            throw e
        }
        val me = withContext(Dispatchers.Default) { LeakGalleryParser.parseMe(json) }
            ?: throw IOException("Could not read account")
        LeakGalleryAuth.cacheProfile(me.username, me.email, me.avatarUrl.ifBlank { null })
        me
    }

    suspend fun premium(): Result<LeakGalleryPremium> = runCatching {
        val p = LeakGalleryParser.parsePremium(api(endpoint("premium/status")))
            ?: throw IOException("Could not read premium status")
        LeakGalleryAuth.cachePremium(p.active)
        p
    }

    suspend fun unreadNotifications(): Result<Int> = runCatching {
        LeakGalleryParser.parseCount(api(endpoint("notifications/unread-count")))
    }

    suspend fun notifications(page: Int = 1): Result<List<LeakGalleryNotification>> = runCatching {
        LeakGalleryParser.parseNotifications(api(endpoint("notifications/me/$page")))
    }

    suspend fun markAllNotificationsRead(): Result<Unit> = runCatching {
        api(endpoint("notifications/mark-all-read"), "{}", "POST")
    }

    suspend fun statistics(): Result<List<Pair<String, Long>>> = runCatching {
        LeakGalleryParser.parseStatistics(api(endpoint("user/statistics")))
    }

    suspend fun logout() {
        runCatching { api(endpoint("auth/logout")) }
        LeakGalleryNetwork.clearCookies()
    }
}
