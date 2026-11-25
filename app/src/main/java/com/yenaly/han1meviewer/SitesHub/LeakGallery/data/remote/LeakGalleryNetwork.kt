package com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote

import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryConstants
import com.yenaly.han1meviewer.logic.network.HDns
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.logic.network.interceptor.UrlLoggingInterceptor
import com.yenaly.han1meviewer.logic.network.interceptor.UserAgentInterceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object LeakGalleryNetwork {

    @Volatile private var _client: OkHttpClient? = null
    @Volatile private var _cookieJar: LeakGalleryCookieJar? = null

    val baseUrl: String
        get() = LeakGalleryConstants.BASE_URL

    val sharedOkHttpClient: OkHttpClient
        get() {
            ensure()
            return _client!!
        }

    @Synchronized
    private fun ensure() {
        if (_client != null) return
        _client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(UserAgentInterceptor)
            .addInterceptor(UrlLoggingInterceptor())
            .cookieJar(_cookieJar ?: LeakGalleryCookieJar().also { _cookieJar = it })
            .dns(HDns())
            .proxySelector(HProxySelector())
            .build()
    }

    /** Videos and images are served from the CDN, which expects the site as Referer/Origin. */
    fun streamRefererFor(@Suppress("UNUSED_PARAMETER") slug: String): String =
        "${LeakGalleryConstants.BASE_URL}/"

    fun clearCookies() {
        _cookieJar?.clearCookies()
        _cookieJar = null
        LeakGalleryRepo.clearToken()
        rebuildNetwork()
    }

    fun rebuildNetwork() {
        _client = null
    }
}
