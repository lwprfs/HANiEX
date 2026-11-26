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
    @Volatile private var _streamClient: OkHttpClient? = null

    val baseUrl: String
        get() = LeakGalleryConstants.BASE_URL

    val sharedOkHttpClient: OkHttpClient
        get() {
            ensure()
            return _client!!
        }

    val streamClient: OkHttpClient
        get() {
            val base = sharedOkHttpClient
            return _streamClient ?: synchronized(this) {
                _streamClient ?: base.newBuilder()
                    .readTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        val b = chain.request().newBuilder()
                        if (chain.request().header("Referer") == null) {
                            b.header("Referer", "${LeakGalleryConstants.BASE_URL}/")
                        }
                        if (chain.request().header("Origin") == null) {
                            b.header("Origin", LeakGalleryConstants.BASE_URL)
                        }
                        chain.proceed(b.build())
                    }
                    .build().also { _streamClient = it }
            }
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

    fun streamRefererFor(@Suppress("UNUSED_PARAMETER") slug: String): String =
        "${LeakGalleryConstants.BASE_URL}/"

    fun clearCookies() {
        _cookieJar?.clearCookies()
        _cookieJar = null
        LeakGalleryRepo.clearToken()
        LeakGalleryAuth.clear()
        rebuildNetwork()
    }

    fun rebuildNetwork() {
        _client = null
        _streamClient = null
    }
}
