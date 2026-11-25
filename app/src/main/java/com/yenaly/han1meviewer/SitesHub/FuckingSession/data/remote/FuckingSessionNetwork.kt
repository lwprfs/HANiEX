package com.yenaly.han1meviewer.SitesHub.FuckingSession.data.remote

import com.yenaly.han1meviewer.SitesHub.FuckingSession.cloudflare.FuckingSessionCloudflareInterceptor
import com.yenaly.han1meviewer.SitesHub.FuckingSession.common.FuckingSessionConstants
import com.yenaly.han1meviewer.logic.network.HDns
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.logic.network.interceptor.UrlLoggingInterceptor
import com.yenaly.han1meviewer.logic.network.interceptor.UserAgentInterceptor
import com.yenaly.han1meviewer.util.applicationContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object FuckingSessionNetwork {

    @Volatile
    private var _service: FuckingSessionService? = null

    @Volatile
    private var _client: OkHttpClient? = null

    @Volatile
    private var _cookieJar: FuckingSessionCookieJar? = null

    val baseUrl: String
        get() = FuckingSessionConstants.BASE_URL

    val service: FuckingSessionService
        get() {
            ensure()
            return _service!!
        }

    val sharedOkHttpClient: OkHttpClient
        get() {
            ensure()
            return _client!!
        }

    val noRedirectClient: OkHttpClient
        get() = sharedOkHttpClient.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

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
            .cookieJar(getOrCreateCookieJar())
            .addInterceptor(FuckingSessionCloudflareInterceptor(applicationContext))
            .dns(HDns())
            .proxySelector(HProxySelector())
            .build()
        _client = client
        _service = Retrofit.Builder()
            .baseUrl(baseUrl + "/")
            .client(client)
            .build()
            .create(FuckingSessionService::class.java)
    }

    private fun getOrCreateCookieJar(): FuckingSessionCookieJar =
        _cookieJar ?: FuckingSessionCookieJar().also { _cookieJar = it }

    private val streamReferers = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun rememberStreamReferer(videoCode: String, referer: String) {
        if (videoCode.isNotBlank() && referer.isNotBlank()) streamReferers[videoCode] = referer
    }

    fun streamRefererFor(videoCode: String): String =
        streamReferers[videoCode] ?: "$baseUrl/"

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
