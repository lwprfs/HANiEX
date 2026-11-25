package com.yenaly.han1meviewer.SitesHub.RouVideo.data.remote

import android.util.Log
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.SitesHub.RouVideo.cloudflare.RouVideoCloudflareCookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

class RouVideoCookieJar : CookieJar {

    companion object {
        private const val TAG = "RouVideoCookieJar"

        const val SESSION_COOKIE = "rouv.session"

        private const val KEY_PERSISTED = "rouvideo_auth_cookies"

        private val cookieStore = mutableMapOf<String, MutableMap<String, Cookie>>()

        private val lock = Any()

        private var restored = false

        private fun isAuthCookie(name: String) = name.startsWith("rouv.")

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
                bucket.values.filter { isAuthCookie(it.name) }.map { "$host\t$it" }
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

        fun importHeader(host: String, header: String) {
            restoreIfNeeded()
            val cookies = ArrayList<Cookie>()
            header.split(';').forEach { part ->
                val eq = part.indexOf('=')
                if (eq <= 0) return@forEach
                val name = part.substring(0, eq).trim()
                val value = part.substring(eq + 1).trim()
                if (name.isEmpty()) return@forEach
                if (name == "cf_clearance") {
                    RouVideoCloudflareCookieManager.saveCloudflareCookie(host, "$name=$value")
                    return@forEach
                }
                val cookie = runCatching {
                    Cookie.Builder().name(name).value(value).hostOnlyDomain(host).path("/").build()
                }.getOrNull() ?: return@forEach
                cookies.add(cookie)
            }
            synchronized(lock) {
                val bucket = cookieStore.getOrPut(host) { mutableMapOf() }
                cookies.forEach { bucket[it.name] = it }
            }
            persist()
        }

        fun clearAuth() {
            restoreIfNeeded()
            synchronized(lock) {
                cookieStore.values.forEach { bucket ->
                    bucket.keys.removeAll { isAuthCookie(it) }
                }
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
        cookies.addAll(RouVideoCloudflareCookieManager.getOkHttpCookies(host))

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
                if (isAuthCookie(it.name)) authChanged = true
            }
            expired.forEach {
                if (bucket.remove(it.name) != null && isAuthCookie(it.name)) authChanged = true
            }
        }
        if (authChanged) persist()

        valid.firstOrNull { it.name == "cf_clearance" }?.let { cf ->
            RouVideoCloudflareCookieManager.saveCloudflareCookie(
                host,
                "${cf.name}=${cf.value}"
            )
            Log.d(TAG, "Saved cf_clearance from response for $host")
        }

        Log.d(TAG, "saveFromResponse: $host, merged=${valid.size}, removed=${expired.size}")
    }

    fun clearCookies() {
        synchronized(lock) { cookieStore.clear() }
        persist()
        RouVideoCloudflareCookieManager.clearAllCloudflareCookies()
        Log.d(TAG, "Cleared all RouVideo cookies")
    }
}
