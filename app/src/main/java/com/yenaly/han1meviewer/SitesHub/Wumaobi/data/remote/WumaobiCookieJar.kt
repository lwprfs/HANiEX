package com.yenaly.han1meviewer.SitesHub.Wumaobi.data.remote

import android.util.Log
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.SitesHub.Wumaobi.cloudflare.WumaobiCloudflareCookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class WumaobiCookieJar : CookieJar {

    companion object {
        private const val TAG = "WumaobiCookieJar"

        const val SESSION_COOKIE = "auth_token"

        private const val KEY_PERSISTED = "wumaobi_auth_cookies"

        private val cookieStore = mutableMapOf<String, MutableMap<String, Cookie>>()

        private val lock = Any()

        private var restored = false

        private fun restoreIfNeeded() = synchronized(lock) {
            if (restored) return@synchronized
            restored = true
            val raw = runCatching { Preferences.preferenceSp.getString(KEY_PERSISTED, null) }
                .getOrNull().orEmpty()
            if (raw.isEmpty()) return@synchronized
            val now = System.currentTimeMillis()
            raw.split('\n').forEach { line ->
                val tab = line.indexOf('\t')
                if (tab <= 0) return@forEach
                val host = line.substring(0, tab)
                val header = line.substring(tab + 1)
                val url = runCatching { HttpUrl.Builder().scheme("https").host(host).build() }
                    .getOrNull() ?: return@forEach
                val cookie = Cookie.parse(url, header) ?: return@forEach
                if (cookie.expiresAt <= now) return@forEach
                cookieStore.getOrPut(host) { mutableMapOf() }[cookie.name] = cookie
            }
        }

        private fun persist() = synchronized(lock) {
            val lines = cookieStore.flatMap { (host, bucket) ->
                bucket.values.filter { it.name == SESSION_COOKIE }.map { "$host\t$it" }
            }
            runCatching {
                Preferences.preferenceSp.edit()
                    .putString(KEY_PERSISTED, lines.joinToString("\n"))
                    .apply()
            }
        }

        fun hasSession(host: String): Boolean {
            restoreIfNeeded()
            val now = System.currentTimeMillis()
            return synchronized(lock) {
                cookieStore[host]?.get(SESSION_COOKIE)?.let { it.expiresAt > now } == true
            }
        }

        fun importSession(host: String, token: String) {
            restoreIfNeeded()
            val cookie = runCatching {
                Cookie.Builder()
                    .name(SESSION_COOKIE)
                    .value(token)
                    .hostOnlyDomain(host)
                    .path("/")
                    .expiresAt(System.currentTimeMillis() + 30L * 24 * 3600 * 1000)
                    .build()
            }.getOrNull() ?: return
            synchronized(lock) {
                cookieStore.getOrPut(host) { mutableMapOf() }[SESSION_COOKIE] = cookie
            }
            persist()
        }

        fun clearAuth() {
            restoreIfNeeded()
            synchronized(lock) {
                cookieStore.values.forEach { it.remove(SESSION_COOKIE) }
            }
            persist()
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        restoreIfNeeded()
        val host = url.host
        val cookies = mutableListOf<Cookie>()

        synchronized(lock) {
            val now = System.currentTimeMillis()
            cookieStore[host]?.values?.filter { it.expiresAt > now }?.let { cookies.addAll(it) }
        }
        cookies.addAll(WumaobiCloudflareCookieManager.getOkHttpCookies(host))

        val deduped = cookies.distinctBy { it.name to it.domain }
        Log.d(TAG, "loadForRequest: $host, cookies=${deduped.size}")
        return deduped
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        restoreIfNeeded()
        val host = url.host
        val now = System.currentTimeMillis()

        val (valid, expired) = cookies.partition {
            it.expiresAt == Long.MAX_VALUE || it.expiresAt > now
        }
        if (valid.isEmpty() && expired.isEmpty()) return

        var authChanged = false
        synchronized(lock) {
            val bucket = cookieStore.getOrPut(host) { mutableMapOf() }
            valid.forEach {
                bucket[it.name] = it
                if (it.name == SESSION_COOKIE) authChanged = true
            }
            expired.forEach {
                if (bucket.remove(it.name) != null && it.name == SESSION_COOKIE) authChanged = true
            }
        }
        if (authChanged) persist()

        valid.firstOrNull { it.name == "cf_clearance" }?.let { cf ->
            WumaobiCloudflareCookieManager.saveCloudflareCookie(
                host,
                "${cf.name}=${cf.value}"
            )
            Log.d(TAG, "Saved cf_clearance from response for $host")
        }

        Log.d(TAG, "saveFromResponse: $host, merged=${valid.size}, removed=${expired.size}")
    }

    fun clearCookies() {
        WumaobiCloudflareCookieManager.clearAllCloudflareCookies()
        Log.d(TAG, "Cleared Wumaobi Cloudflare cookies")
    }
}
