package com.yenaly.han1meviewer.SitesHub.Coomer.data.remote

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class CoomerCookieJar : CookieJar {

    companion object {
        private val cookieStore = mutableMapOf<String, MutableMap<String, Cookie>>()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        return synchronized(cookieStore) {
            cookieStore[url.host]?.values?.filter { it.expiresAt > now }.orEmpty()
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val now = System.currentTimeMillis()
        val valid = cookies.filter { it.expiresAt > now }
        if (valid.isEmpty()) return
        synchronized(cookieStore) {
            val bucket = cookieStore.getOrPut(url.host) { mutableMapOf() }
            valid.forEach { bucket[it.name] = it }
        }
    }

    fun clearCookies() {
        synchronized(cookieStore) { cookieStore.clear() }
    }
}
