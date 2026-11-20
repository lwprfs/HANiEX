package com.yenaly.han1meviewer.RedGifs.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.webkit.CookieManager
import com.yenaly.han1meviewer.RedGifs.common.RedGifsConstants
import com.yenaly.han1meviewer.RedGifs.common.RedGifsSession
import com.yenaly.han1meviewer.logic.network.HDns
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.logic.network.interceptor.UserAgentInterceptor
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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object RedGifsAuth {

    private const val PREFS = "redgifs_auth"
    private const val K_ACCESS = "access_token"
    private const val K_ACCESS_EXP = "access_exp"
    private const val K_REFRESH = "refresh_token"
    private const val K_GUEST = "guest_token"
    private const val K_GUEST_EXP = "guest_exp"
    private const val K_USERNAME = "username"
    private const val K_AVATAR = "avatar"

    private val lock = Any()

    private val sp: SharedPreferences
        get() = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(UserAgentInterceptor)
            .dns(HDns())
            .proxySelector(HProxySelector())
            .build()
    }

    private sealed interface RefreshResult {
        data class Ok(val access: String) : RefreshResult
        data object Rejected : RefreshResult
        data object Failed : RefreshResult
    }

    val isLoggedIn: Boolean
        get() = !sp.getString(K_REFRESH, null).isNullOrBlank() ||
                !sp.getString(K_ACCESS, null).isNullOrBlank()

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

    fun isRedGifsAccessToken(token: String): Boolean = runCatching {
        val payload = token.split('.')[1]
        val raw = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val o = json.parseToJsonElement(String(raw)).jsonObject
        val iss = (o["iss"] as? JsonPrimitive)?.contentOrNull.orEmpty()
        iss.contains("auth2.redgifs.com") && o["at_hash"] == null
    }.getOrDefault(false)

    fun bearer(force: Boolean = false): String? = synchronized(lock) {
        val access = sp.getString(K_ACCESS, null)
        val exp = sp.getLong(K_ACCESS_EXP, 0L)
        val refresh = sp.getString(K_REFRESH, null)
        if (!access.isNullOrBlank() || !refresh.isNullOrBlank()) {
            if (!force && !access.isNullOrBlank() && exp - 60 > nowSec()) return@synchronized access
            if (!refresh.isNullOrBlank()) {
                when (val r = requestToken(refresh)) {
                    is RefreshResult.Ok -> return@synchronized r.access
                    RefreshResult.Rejected -> clearUserTokens(bump = true)
                    RefreshResult.Failed -> {
                        if (!access.isNullOrBlank() && exp > nowSec()) return@synchronized access
                    }
                }
            } else if (exp <= nowSec()) {
                clearUserTokens(bump = true)
            } else {
                return@synchronized access
            }
        }
        guestToken(force)
    }

    private fun guestToken(force: Boolean): String? {
        val cached = sp.getString(K_GUEST, null)
        val exp = sp.getLong(K_GUEST_EXP, 0L)
        if (!force && !cached.isNullOrBlank() && exp - 60 > nowSec()) return cached
        val req = Request.Builder()
            .url("${RedGifsConstants.API_BASE}/v2/auth/temporary")
            .header("Origin", RedGifsConstants.ORIGIN)
            .header("Referer", RedGifsConstants.REFERER)
            .header("Accept", "application/json")
            .build()
        return try {
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val token = json.parseToJsonElement(resp.body?.string().orEmpty())
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
                val text = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val o: JsonObject = json.parseToJsonElement(text).jsonObject
                    val access = (o["access_token"] as? JsonPrimitive)?.contentOrNull
                    if (access.isNullOrBlank()) return RefreshResult.Failed
                    val newRefresh = (o["refresh_token"] as? JsonPrimitive)?.contentOrNull
                    val editor = sp.edit()
                        .putString(K_ACCESS, access)
                        .putLong(K_ACCESS_EXP, jwtExp(access))
                    if (!newRefresh.isNullOrBlank()) editor.putString(K_REFRESH, newRefresh)
                    editor.apply()
                    RefreshResult.Ok(access)
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

    fun refreshTokenFromWebView(): String? {
        val cookies = CookieManager.getInstance().getCookie(RedGifsConstants.AUTH_BASE).orEmpty()
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

    suspend fun completeLoginWithAccess(access: String): Boolean = withContext(Dispatchers.IO) {
        if (!isRedGifsAccessToken(access) || jwtExp(access) <= nowSec()) return@withContext false
        synchronized(lock) {
            sp.edit()
                .putString(K_ACCESS, access)
                .putLong(K_ACCESS_EXP, jwtExp(access))
                .apply()
        }
        onLoggedIn()
        true
    }

    private fun onLoggedIn() {
        RedGifsRepo.clearCaches()
        RedGifsSession.bump()
    }

    private fun clearUserTokens(bump: Boolean) {
        sp.edit()
            .remove(K_ACCESS).remove(K_ACCESS_EXP).remove(K_REFRESH)
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

    private fun clearWebSession() {
        runCatching {
            val cm = CookieManager.getInstance()
            listOf(RedGifsConstants.AUTH_BASE, RedGifsConstants.BASE_URL).forEach { url ->
                cm.getCookie(url).orEmpty().split(';')
                    .map { it.substringBefore('=').trim() }
                    .filter { it.isNotEmpty() }
                    .forEach { name ->
                        cm.setCookie(url, "$name=; Max-Age=0; Path=/")
                    }
            }
            cm.flush()
        }
    }
}
