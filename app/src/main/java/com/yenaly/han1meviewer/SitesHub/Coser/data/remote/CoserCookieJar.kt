package com.yenaly.han1meviewer.SitesHub.Coser.data.remote

import com.yenaly.han1meviewer.SitesHub.Coser.cloudflare.CoserCloudflareCookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class CoserCookieJar : CookieJar {

    companion object {
        private val cookieStore = mutableMapOf<String, MutableMap<String, Cookie>>()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val cookies = mutableListOf<Cookie>()
        synchronized(cookieStore) { cookieStore[host]?.values?.let { cookies.addAll(it) } }
        cookies.addAll(CoserCloudflareCookieManager.getOkHttpCookies(host))
        return cookies.distinctBy { it.name to it.domain }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val now = System.currentTimeMillis()
        val valid = cookies.filter { it.expiresAt == Long.MAX_VALUE || it.expiresAt > now }
        if (valid.isEmpty()) return
        synchronized(cookieStore) {
            val bucket = cookieStore.getOrPut(host) { mutableMapOf() }
            valid.forEach { bucket[it.name] = it }
        }
        valid.firstOrNull { it.name == "cf_clearance" }?.let { cf ->
            CoserCloudflareCookieManager.saveCloudflareCookie(host, "${cf.name}=${cf.value}")
        }
    }

    fun clearCookies() {
        synchronized(cookieStore) { cookieStore.clear() }
        CoserCloudflareCookieManager.clearAllCloudflareCookies()
    }
}
