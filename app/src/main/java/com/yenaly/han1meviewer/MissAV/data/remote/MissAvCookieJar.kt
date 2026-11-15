package com.yenaly.han1meviewer.MissAV.data.remote
import android.util.Log
import com.yenaly.han1meviewer.Preferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import org.json.JSONArray
import org.json.JSONObject

import com.yenaly.han1meviewer.MissAV.cloudflare.MissAvCloudflareCookieManager

/**
 * In-memory cookie jar for MissAV that also persists the site's session cookies
 * (Laravel session, XSRF token and "remember me" cookie) so a login survives app restarts.
 * `cf_clearance` is handled separately by [MissAvCloudflareCookieManager].
 */
class MissAvCookieJar : CookieJar {

    companion object {
        private const val TAG = "MissAvCookieJar"
        private const val PREF_SESSION_COOKIES = "missav_session_cookies"
        private const val CF_CLEARANCE = "cf_clearance"
        private const val XSRF_COOKIE = "XSRF-TOKEN"

        private val lock = Any()
        private val cookieStore = mutableMapOf<String, MutableMap<String, Cookie>>()
        @Volatile
        private var restored = false

        private fun ensureRestored() {
            if (restored) return
            synchronized(lock) {
                if (restored) return
                restored = true
                runCatching {
                    val raw = Preferences.preferenceSp.getString(PREF_SESSION_COOKIES, null)
                        ?: return@runCatching
                    val array = JSONArray(raw)
                    val now = System.currentTimeMillis()
                    for (i in 0 until array.length()) {
                        val o = array.getJSONObject(i)
                        val expiresAt = o.optLong("expiresAt", Long.MAX_VALUE)
                        if (expiresAt != Long.MAX_VALUE && expiresAt <= now) continue
                        val host = o.getString("host")
                        val builder = Cookie.Builder()
                            .name(o.getString("name"))
                            .value(o.getString("value"))
                            .path(o.optString("path", "/"))
                        if (o.optBoolean("hostOnly", true)) builder.hostOnlyDomain(host) else builder.domain(host)
                        if (expiresAt != Long.MAX_VALUE) builder.expiresAt(expiresAt)
                        if (o.optBoolean("secure")) builder.secure()
                        if (o.optBoolean("httpOnly")) builder.httpOnly()
                        cookieStore.getOrPut(host) { mutableMapOf() }[o.getString("name")] = builder.build()
                    }
                }.onFailure { Log.w(TAG, "Failed to restore session cookies", it) }
            }
        }

        private fun persist() {
            runCatching {
                val array = JSONArray()
                val now = System.currentTimeMillis()
                cookieStore.forEach { (host, map) ->
                    map.values.forEach { c ->
                        if (c.name == CF_CLEARANCE) return@forEach
                        if (c.expiresAt != Long.MAX_VALUE && c.expiresAt <= now) return@forEach
                        array.put(
                            JSONObject()
                                .put("host", host)
                                .put("name", c.name)
                                .put("value", c.value)
                                .put("path", c.path)
                                .put("expiresAt", c.expiresAt)
                                .put("hostOnly", c.hostOnly)
                                .put("secure", c.secure)
                                .put("httpOnly", c.httpOnly)
                        )
                    }
                }
                Preferences.preferenceSp.edit()
                    .putString(PREF_SESSION_COOKIES, array.toString())
                    .apply()
            }.onFailure { Log.w(TAG, "Failed to persist session cookies", it) }
        }

        /** Current `XSRF-TOKEN` cookie value for [host] (URL-decoded), as axios would send it. */
        fun xsrfToken(host: String): String? {
            ensureRestored()
            val raw = synchronized(lock) { cookieStore[host]?.get(XSRF_COOKIE)?.value } ?: return null
            return runCatching { java.net.URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)
        }

        /** Whether any non-Cloudflare cookie is held for [host] (i.e. a session may exist). */
        fun hasSessionCookies(host: String): Boolean {
            ensureRestored()
            return synchronized(lock) {
                cookieStore[host]?.values?.any {
                    it.name != CF_CLEARANCE && it.name != XSRF_COOKIE
                } == true
            }
        }

        /** Drops every site cookie (login session) but keeps the Cloudflare clearance. */
        fun clearSessionCookies() {
            ensureRestored()
            synchronized(lock) {
                cookieStore.values.forEach { map ->
                    map.keys.removeAll { it != CF_CLEARANCE }
                }
            }
            Preferences.preferenceSp.edit().remove(PREF_SESSION_COOKIES).apply()
            Log.d(TAG, "Cleared MissAV session cookies")
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        ensureRestored()
        val host = url.host
        val now = System.currentTimeMillis()
        val cookies = mutableListOf<Cookie>()

        synchronized(lock) {
            cookieStore[host]?.values
                ?.filter { it.expiresAt == Long.MAX_VALUE || it.expiresAt > now }
                ?.let { cookies.addAll(it) }
        }

        val cfCookies = MissAvCloudflareCookieManager.getOkHttpCookies(host)
        cookies.addAll(cfCookies)

        Log.d(TAG, "loadForRequest: $host, cookies: ${cookies.size}")
        return cookies
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        ensureRestored()
        val host = url.host
        val now = System.currentTimeMillis()

        // A cookie the server expires (logout / session reset) must be removed, not ignored.
        val expired = cookies.filter { it.expiresAt != Long.MAX_VALUE && it.expiresAt <= now }
        val validCookies = cookies.filter {
            it.expiresAt == Long.MAX_VALUE || it.expiresAt > now
        }
        if (validCookies.isEmpty() && expired.isEmpty()) return

        synchronized(lock) {
            val hostMap = cookieStore.getOrPut(host) { mutableMapOf() }
            expired.forEach { hostMap.remove(it.name) }
            validCookies.forEach { cookie ->
                hostMap[cookie.name] = cookie
            }
            Log.d(TAG, "saveFromResponse: $host, merged ${validCookies.size}, total ${hostMap.size}")
        }
        persist()

        validCookies.firstOrNull { it.name == CF_CLEARANCE }?.let { cf ->
            MissAvCloudflareCookieManager.saveCloudflareCookie(
                host,
                "${cf.name}=${cf.value}"
            )
            Log.d(TAG, "Saved cf_clearance from response for $host")
        }
    }

    fun clearCookies() {
        synchronized(lock) { cookieStore.clear() }
        Preferences.preferenceSp.edit().remove(PREF_SESSION_COOKIES).apply()
        MissAvCloudflareCookieManager.clearAllCloudflareCookies()
        Log.d(TAG, "Cleared all MissAV cookies")
    }
}
