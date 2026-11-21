package com.yenaly.han1meviewer.XXXTube.data.remote

import android.util.Log
import com.yenaly.han1meviewer.XXXTube.cloudflare.XXXTubeCloudflareCookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class XXXTubeCookieJar : CookieJar {

    companion object {
        private const val TAG = "XXXTubeCookieJar"
        private val cookieStore = mutableMapOf<String, MutableMap<String, Cookie>>()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val cookies = mutableListOf<Cookie>()

        cookieStore[host]?.values?.let { cookies.addAll(it) }
        cookies.addAll(XXXTubeCloudflareCookieManager.getOkHttpCookies(host))

        val deduped = cookies.distinctBy { it.name to it.domain }
        Log.d(TAG, "loadForRequest: $host, cookies=${deduped.size}")
        return deduped
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val now = System.currentTimeMillis()

        val valid = cookies.filter {
            it.expiresAt == Long.MAX_VALUE || it.expiresAt > now
        }
        if (valid.isEmpty()) return

        val bucket = cookieStore.getOrPut(host) { mutableMapOf() }
        valid.forEach { bucket[it.name] = it }

        valid.firstOrNull { it.name == "cf_clearance" }?.let { cf ->
            XXXTubeCloudflareCookieManager.saveCloudflareCookie(
                host,
                "${cf.name}=${cf.value}"
            )
            Log.d(TAG, "Saved cf_clearance from response for $host")
        }

        Log.d(TAG, "saveFromResponse: $host, merged=${valid.size}, total=${bucket.size}")
    }

    fun clearCookies() {
        cookieStore.clear()
        XXXTubeCloudflareCookieManager.clearAllCloudflareCookies()
        Log.d(TAG, "Cleared all XXXTube cookies")
    }
}
