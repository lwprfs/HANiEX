package com.yenaly.han1meviewer.SitesHub.RedGifs.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.webkit.CookieManager
import com.yenaly.han1meviewer.SitesHub.RedGifs.common.RedGifsConstants
import com.yenaly.han1meviewer.SitesHub.RedGifs.common.RedGifsSession
import com.yenaly.han1meviewer.logic.network.HDns
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.util.applicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object RedGifsAuth {

    private const val PREFS = "redgifs_auth"

    private const val K_ID = "id_token"
    private const val K_ID_EXP = "id_exp"
    private const val K_LEGACY_ACCESS = "access_token"
    private const val K_LEGACY_ACCESS_EXP = "access_exp"
    private const val K_REFRESH = "refresh_token"
    private const val K_GUEST = "guest_token"
    private const val K_GUEST_EXP = "guest_exp"
    private const val K_USERNAME = "username"
    private const val K_AVATAR = "avatar"
    private const val K_SESSION = "session_id"

    private val lock = Any()

    private val sp: SharedPreferences
        get() = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", RedGifsConstants.USER_AGENT)
                        .build()
                )
            }
            .dns(HDns())
            .proxySelector(HProxySelector())
            .build()
    }

    private sealed interface RefreshResult {
        data class Ok(val idToken: String) : RefreshResult
        data object Rejected : RefreshResult
        data object Failed : RefreshResult
    }

    val sessionId: String
        get() = sp.getString(K_SESSION, null) ?: run {
            val rnd = java.security.SecureRandom()
            val id = buildString { repeat(18) { append(if (it == 0) 1 + rnd.nextInt(9) else rnd.nextInt(10)) } }
            sp.edit().putString(K_SESSION, id).apply()
            id
        }

    val isLoggedIn: Boolean
        get() = !sp.getString(K_REFRESH, null).isNullOrBlank() ||
                !sp.getString(K_ID, null).isNullOrBlank()

    var username: String?
        get() = sp.getString(K_USERNAME, null)
        private set(value) {
            sp.edit().putString(K_USERNAME, value).apply()
        }

    var avatarUrl: String?
        get() = sp.getString(K_AVATAR, null)
        private set(value) {
            sp.edit().putString(K_AVATAR, value).apply()
        }

    fun cacheProfile(name: String?, avatar: String?) {
        username = name
        avatarUrl = avatar
    }

    private fun nowSec(): Long = System.currentTimeMillis() / 1000

    private fun jwtExp(token: String): Long = runCatching {
        val payload = token.split('.')[1]
        val raw = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        json.parseToJsonElement(String(raw)).jsonObject["exp"]?.let { (it as? JsonPrimitive)?.longOrNull } ?: 0L
    }.getOrDefault(0L)

    fun isRedGifsIdToken(token: String): Boolean = runCatching {
        val payload = token.split('.')[1]
        val raw = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val o = json.parseToJsonElement(String(raw)).jsonObject
        val iss = (o["iss"] as? JsonPrimitive)?.contentOrNull.orEmpty()
        iss.contains("auth2.redgifs.com") &&
                (o["at_hash"] != null || o["email"] != null || o["preferred_username"] != null)
    }.getOrDefault(false)

    private fun claim(token: String, name: String): String? = runCatching {
        val payload = token.split('.')[1]
        val raw = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        (json.parseToJsonElement(String(raw)).jsonObject[name] as? JsonPrimitive)?.contentOrNull
    }.getOrNull()

    private fun dropLegacyAccess() {
        if (sp.contains(K_LEGACY_ACCESS) || sp.contains(K_LEGACY_ACCESS_EXP)) {
            sp.edit().remove(K_LEGACY_ACCESS).remove(K_LEGACY_ACCESS_EXP).apply()
        }
    }

    private fun saveIdToken(idToken: String, refresh: String?) {
        val e = sp.edit()
            .putString(K_ID, idToken)
            .putLong(K_ID_EXP, jwtExp(idToken))
            .remove(K_LEGACY_ACCESS).remove(K_LEGACY_ACCESS_EXP)
        if (!refresh.isNullOrBlank()) e.putString(K_REFRESH, refresh)
        if (sp.getString(K_USERNAME, null).isNullOrBlank()) {
            claim(idToken, "preferred_username")?.takeIf { it.isNotBlank() }?.let { e.putString(K_USERNAME, it) }
        }
        e.apply()
    }

    fun bearer(force: Boolean = false): String? = synchronized(lock) {
        dropLegacyAccess()
        val idToken = sp.getString(K_ID, null)
        val exp = sp.getLong(K_ID_EXP, 0L)
        val refresh = sp.getString(K_REFRESH, null)
        if (!idToken.isNullOrBlank() || !refresh.isNullOrBlank()) {
            if (!force && !idToken.isNullOrBlank() && exp - 60 > nowSec()) return@synchronized idToken
            if (!refresh.isNullOrBlank()) {
                when (val r = requestToken(refresh)) {
                    is RefreshResult.Ok -> return@synchronized r.idToken
                    RefreshResult.Rejected -> clearUserTokens(bump = true)
                    RefreshResult.Failed -> {
                        if (!force && !idToken.isNullOrBlank() && exp > nowSec()) return@synchronized idToken
                    }
                }
            } else if (force || exp <= nowSec()) {
                clearUserTokens(bump = true)
            } else {
                return@synchronized idToken
            }
        }
        guestToken(force)
    }

    fun guestBearer(force: Boolean = false): String? = synchronized(lock) { guestToken(force) }

    private fun guestToken(force: Boolean): String? {
        val cached = sp.getString(K_GUEST, null)
        val exp = sp.getLong(K_GUEST_EXP, 0L)
        if (!force && !cached.isNullOrBlank() && exp - 60 > nowSec()) return cached
        val req = Request.Builder()
            .url(
                "${RedGifsConstants.API_BASE}/v2/auth/temporary".toHttpUrl().newBuilder()
                    .addQueryParameter("session_id", sessionId)
                    .addQueryParameter("referrer", "https://redgifs.com/")
                    .addQueryParameter("path", "/")
                    .build()
            )
            .header("Origin", RedGifsConstants.ORIGIN)
            .header("Referer", RedGifsConstants.REFERER)
            .header("Accept", "application/json")
            .build()
        return try {
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val token = json.parseToJsonElement(resp.body.string().orEmpty())
                    .jsonObject["token"]?.let { (it as? JsonPrimitive)?.contentOrNull }
                if (token.isNullOrBlank()) return null
                sp.edit().putString(K_GUEST, token).putLong(K_GUEST_EXP, jwtExp(token)).apply()
                token
            }
        } catch (_: IOException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun requestToken(refresh: String): RefreshResult {
        val body = FormBody.Builder()
            .add("client_id", RedGifsConstants.CLIENT_ID)
            .add("grant_type", "refresh_token")
            .add("refresh_token", refresh)
            .build()
        val req = Request.Builder()
            .url("${RedGifsConstants.AUTH_BASE}/oauth2/token")
            .post(body)
            .header("Origin", RedGifsConstants.ORIGIN)
            .header("Referer", RedGifsConstants.REFERER)
            .header("Accept", "application/json")
            .header("Cookie", "refresh_token=$refresh")
            .build()
        return try {
            http.newCall(req).execute().use { resp ->
                val text = resp.body.string().orEmpty()
                if (resp.isSuccessful) {
                    val o: JsonObject = json.parseToJsonElement(text).jsonObject
                    val idToken = (o["id_token"] as? JsonPrimitive)?.contentOrNull
                    if (idToken.isNullOrBlank()) return RefreshResult.Failed
                    val newRefresh = (o["refresh_token"] as? JsonPrimitive)?.contentOrNull
                    saveIdToken(idToken, newRefresh)
                    RefreshResult.Ok(idToken)
                } else if (resp.code in 400..403) {
                    RefreshResult.Rejected
                } else {
                    RefreshResult.Failed
                }
            }
        } catch (_: IOException) {
            RefreshResult.Failed
        } catch (_: Exception) {
            RefreshResult.Failed
        }
    }

    fun webAuthCookies(): String = runCatching {
        CookieManager.getInstance().getCookie(RedGifsConstants.AUTH_BASE).orEmpty()
    }.getOrDefault("")

    fun refreshTokenFromWebView(): String? {
        val cookies = webAuthCookies()
        return cookies.split(';')
            .map { it.trim() }
            .firstOrNull { it.startsWith("refresh_token=") }
            ?.substringAfter('=')
            ?.takeIf { it.isNotBlank() }
    }

    suspend fun completeLoginWithRefresh(refresh: String): Boolean = withContext(Dispatchers.IO) {
        val result = synchronized(lock) { requestToken(refresh) }
        if (result is RefreshResult.Ok) {
            onLoggedIn()
            true
        } else {
            false
        }
    }

    suspend fun completeLoginWithIdToken(idToken: String): Boolean = withContext(Dispatchers.IO) {
        if (!isRedGifsIdToken(idToken) || jwtExp(idToken) <= nowSec()) return@withContext false
        synchronized(lock) { saveIdToken(idToken, null) }
        onLoggedIn()
        true
    }

    private fun onLoggedIn() {
        RedGifsRepo.clearCaches()
        RedGifsSession.bump()
    }

    private fun clearUserTokens(bump: Boolean) {
        sp.edit()
            .remove(K_ID).remove(K_ID_EXP).remove(K_REFRESH)
            .remove(K_LEGACY_ACCESS).remove(K_LEGACY_ACCESS_EXP)
            .remove(K_USERNAME).remove(K_AVATAR)
            .apply()
        RedGifsRepo.clearCaches()
        if (bump) RedGifsSession.bump()
    }

    fun logout() {
        synchronized(lock) { clearUserTokens(bump = false) }
        clearWebSession()
        RedGifsSession.bump()
    }

    fun clearWebSession() {
        runCatching {
            val cm = CookieManager.getInstance()
            val urls = listOf(
                RedGifsConstants.AUTH_BASE,
                RedGifsConstants.BASE_URL,
                "https://redgifs.com",
                "https://www.redgifs.com",
                RedGifsConstants.API_BASE,
            ).distinct()
            val expired = "Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/"
            urls.forEach { url ->
                val host = runCatching { url.toHttpUrl().host }.getOrNull() ?: return@forEach
                val parent = host.split('.').takeLast(2).joinToString(".")
                cm.getCookie(url).orEmpty().split(';')
                    .map { it.substringBefore('=').trim() }
                    .filter { it.isNotEmpty() }
                    .forEach { name ->
                        cm.setCookie(url, "$name=; $expired")
                        cm.setCookie(url, "$name=; $expired; Domain=$host")
                        cm.setCookie(url, "$name=; $expired; Domain=.$host")
                        cm.setCookie(url, "$name=; $expired; Domain=$parent")
                        cm.setCookie(url, "$name=; $expired; Domain=.$parent")
                    }
            }
            cm.flush()
        }
    }

    suspend fun completeLoginWithTokens(idToken: String, refresh: String?): Boolean = withContext(Dispatchers.IO) {
        if (idToken.isBlank()) return@withContext false
        synchronized(lock) { saveIdToken(idToken, refresh) }
        onLoggedIn()
        true
    }

    suspend fun importPasted(raw: String): Boolean {
        val text = raw.trim()
        if (text.isEmpty()) return false
        val refresh = Regex("""refresh_token["']?\s*[=:]\s*["']?([A-Za-z0-9._~+/\-]{20,})""").find(text)?.groupValues?.get(1)
            ?: text.takeIf { it.length in 20..400 && it.none { c -> c.isWhitespace() || c == ';' || c == '=' || c == '"' } && it.count { c -> c == '.' } != 2 }
        if (refresh != null && completeLoginWithRefresh(refresh)) return true
        val idToken = Regex("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+").findAll(text)
            .map { it.value }.firstOrNull { isRedGifsIdToken(it) }
        return idToken != null && completeLoginWithIdToken(idToken)
    }
}
