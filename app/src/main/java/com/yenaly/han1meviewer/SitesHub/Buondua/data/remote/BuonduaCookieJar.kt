package com.yenaly.han1meviewer.SitesHub.Buondua.data.remote

import com.yenaly.han1meviewer.SitesHub.Buondua.cloudflare.BuonduaCloudflareCookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class BuonduaCookieJar : CookieJar {

    companion object {
        private val cookieStore = mutableMapOf<String, MutableMap<String, Cookie>>()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val cookies = mutableListOf<Cookie>()
        cookieStore[host]?.values?.let { cookies.addAll(it) }
        cookies.addAll(BuonduaCloudflareCookieManager.getOkHttpCookies(host))
        return cookies.distinctBy { it.name to it.domain }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val now = System.currentTimeMillis()
        val valid = cookies.filter { it.expiresAt == Long.MAX_VALUE || it.expiresAt > now }
        if (valid.isEmpty()) return
        val bucket = cookieStore.getOrPut(host) { mutableMapOf() }
        valid.forEach { bucket[it.name] = it }
        valid.firstOrNull { it.name == "cf_clearance" }?.let { cf ->
            BuonduaCloudflareCookieManager.saveCloudflareCookie(host, "${cf.name}=${cf.value}")
        }
    }

    fun clearCookies() {
        cookieStore.clear()
        BuonduaCloudflareCookieManager.clearAllCloudflareCookies()
    }
}
