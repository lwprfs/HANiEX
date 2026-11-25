package com.yenaly.han1meviewer.SitesHub.Coser.data.remote

import com.yenaly.han1meviewer.SitesHub.Coser.cloudflare.CoserCloudflareInterceptor
import com.yenaly.han1meviewer.SitesHub.Coser.common.CoserConstants
import com.yenaly.han1meviewer.logic.network.HDns
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.logic.network.interceptor.UrlLoggingInterceptor
import com.yenaly.han1meviewer.logic.network.interceptor.UserAgentInterceptor
import com.yenaly.han1meviewer.util.applicationContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object CoserNetwork {

    @Volatile private var _service: CoserService? = null
    @Volatile private var _client: OkHttpClient? = null
    @Volatile private var _cookieJar: CoserCookieJar? = null

    val baseUrl: String
        get() = CoserConstants.BASE_URL

    val service: CoserService
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
            .cookieJar(_cookieJar ?: CoserCookieJar().also { _cookieJar = it })
            .addInterceptor(CoserCloudflareInterceptor(applicationContext))
            .dns(HDns())
            .proxySelector(HProxySelector())
            .build()
        _client = client
        _service = Retrofit.Builder()
            .baseUrl("$baseUrl/")
            .client(client)
            .build()
            .create(CoserService::class.java)
    }

    private val streamReferers = ConcurrentHashMap<String, String>()

    fun rememberStreamReferer(slug: String, referer: String) {
        if (slug.isNotBlank() && referer.isNotBlank()) streamReferers[slug] = referer
    }

    fun streamRefererFor(slug: String): String =
        streamReferers[slug] ?: CoserConstants.STREAM_REFERER

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
