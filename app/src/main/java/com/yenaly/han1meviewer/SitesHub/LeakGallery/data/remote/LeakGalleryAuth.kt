package com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGallerySession
import com.yenaly.han1meviewer.util.applicationContext
import org.json.JSONObject

/**
 * Stores the LeakGallery session. The site authenticates API calls with a
 * `Authorization: Bearer <jwt>` header (the JWT is returned by `auth/login`
 * and `auth/register`), so the session is just that token plus a cached profile.
 */
object LeakGalleryAuth {

    private const val PREFS = "leakgallery_auth"
    private const val K_TOKEN = "token"
    private const val K_USERNAME = "username"
    private const val K_EMAIL = "email"
    private const val K_AVATAR = "avatar"
    private const val K_PREMIUM = "premium"

    private val sp: SharedPreferences
        get() = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val token: String?
        get() = sp.getString(K_TOKEN, null)?.takeIf { it.isNotBlank() }

    val isLoggedIn: Boolean
        get() {
            val t = token ?: return false
            val exp = jwtExp(t)
            return exp <= 0L || exp > System.currentTimeMillis() / 1000
        }

    val username: String?
        get() = sp.getString(K_USERNAME, null)?.takeIf { it.isNotBlank() }

    val email: String?
        get() = sp.getString(K_EMAIL, null)?.takeIf { it.isNotBlank() }

    val avatarUrl: String?
        get() = sp.getString(K_AVATAR, null)?.takeIf { it.isNotBlank() }

    val isPremium: Boolean
        get() = sp.getBoolean(K_PREMIUM, false)

    fun saveToken(token: String) {
        val e = sp.edit().putString(K_TOKEN, token)
        jwtClaim(token, "username")?.let { e.putString(K_USERNAME, it) }
        e.apply()
        LeakGallerySession.bump()
    }

    fun cacheProfile(name: String?, email: String?, avatar: String?) {
        sp.edit()
            .putString(K_USERNAME, name)
            .putString(K_EMAIL, email)
            .putString(K_AVATAR, avatar)
            .apply()
        LeakGallerySession.bump()
    }

    fun cachePremium(active: Boolean) {
        if (sp.getBoolean(K_PREMIUM, false) != active) {
            sp.edit().putBoolean(K_PREMIUM, active).apply()
            LeakGallerySession.bump()
        }
    }

    fun clear() {
        sp.edit().clear().apply()
        LeakGallerySession.bump()
    }

    private fun payload(token: String): JSONObject? = runCatching {
        val raw = Base64.decode(
            token.split('.')[1],
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
        )
        JSONObject(String(raw))
    }.getOrNull()

    private fun jwtExp(token: String): Long = payload(token)?.optLong("exp", 0L) ?: 0L

    private fun jwtClaim(token: String, name: String): String? =
        payload(token)?.optString(name)?.takeIf { it.isNotBlank() }
}
