package com.yenaly.han1meviewer.Coomer.data.remote

import com.yenaly.han1meviewer.Coomer.common.CoomerConstants
import com.yenaly.han1meviewer.logic.network.HDns
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.logic.network.interceptor.UrlLoggingInterceptor
import com.yenaly.han1meviewer.logic.network.interceptor.UserAgentInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object CoomerNetwork {

    @Volatile private var _service: CoomerService? = null
    @Volatile private var _client: OkHttpClient? = null
    @Volatile private var _cookieJar: CoomerCookieJar? = null

    val baseUrl: String
        get() = CoomerConstants.BASE_URL

    val service: CoomerService
        get() {
            ensure()
            return _service!!
        }

    val sharedOkHttpClient: OkHttpClient
        get() {
            ensure()
            return _client!!
        }

    @Synchronized
    private fun ensure() {
        if (_service != null && _client != null) return
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(UserAgentInterceptor)
            .addInterceptor(UrlLoggingInterceptor())
            .cookieJar(_cookieJar ?: CoomerCookieJar().also { _cookieJar = it })
            .dns(HDns())
            .proxySelector(HProxySelector())
            .build()
        _client = client
        _service = Retrofit.Builder()
            .baseUrl("$baseUrl/")
            .client(client)
            .build()
            .create(CoomerService::class.java)
    }

    /** Media is served from `img*.coomerfans.com`; the site expects its own pages as Referer. */
    fun streamRefererFor(@Suppress("UNUSED_PARAMETER") slug: String): String =
        "${CoomerConstants.BASE_URL}/"

    fun clearCookies() {
        _cookieJar?.clearCookies()
        _cookieJar = null
        rebuildNetwork()
    }

    fun rebuildNetwork() {
        _service = null
        _client = null
    }
}
