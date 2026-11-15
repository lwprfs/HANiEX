package com.yenaly.han1meviewer.Buondua.data.remote

import com.yenaly.han1meviewer.Buondua.cloudflare.BuonduaCloudflareInterceptor
import com.yenaly.han1meviewer.Buondua.common.BuonduaConstants
import com.yenaly.han1meviewer.logic.network.HDns
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.logic.network.interceptor.UrlLoggingInterceptor
import com.yenaly.han1meviewer.logic.network.interceptor.UserAgentInterceptor
import com.yenaly.han1meviewer.util.applicationContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object BuonduaNetwork {

    @Volatile private var _service: BuonduaService? = null
    @Volatile private var _client: OkHttpClient? = null
    @Volatile private var _streamClient: OkHttpClient? = null
    @Volatile private var _cookieJar: BuonduaCookieJar? = null

    val baseUrl: String get() = BuonduaConstants.BASE_URL

    val service: BuonduaService
        get() { ensure(); return _service!! }

    val sharedOkHttpClient: OkHttpClient
        get() { ensure(); return _client!! }

    /**
     * Client for media files (photos, mp4). No Cloudflare interceptor: CDN hosts are not
     * behind the challenge and the interceptor must not try to solve it on a media host.
     */
    val streamClient: OkHttpClient
        get() { ensure(); return _streamClient!! }

    /** Headers every media request (image, probe, inline playback, download) must carry. */
    fun mediaHeaders(): Map<String, String> = mapOf(
        "Referer" to BuonduaConstants.REFERER,
        "Origin" to BuonduaConstants.BASE_URL,
        "Accept" to "*/*",
    )

    @Synchronized
    private fun ensure() {
        if (_service != null && _client != null && _streamClient != null) return
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(UserAgentInterceptor)
            .addInterceptor(UrlLoggingInterceptor())
            .cookieJar(_cookieJar ?: BuonduaCookieJar().also { _cookieJar = it })
            .addInterceptor(BuonduaCloudflareInterceptor(applicationContext))
            .dns(HDns())
            .proxySelector(HProxySelector())
            .build()
        _client = client
        _streamClient = client.newBuilder()
            .readTimeout(60, TimeUnit.SECONDS)
            .apply { interceptors().removeAll { it is BuonduaCloudflareInterceptor } }
            .build()
        _service = Retrofit.Builder()
            .baseUrl(baseUrl + "/")
            .client(client)
            .build()
            .create(BuonduaService::class.java)
    }

    fun clearCookies() {
        _cookieJar?.clearCookies()
        _cookieJar = null
        rebuildNetwork()
    }

    fun rebuildNetwork() {
        _service = null
        _client = null
        _streamClient = null
    }
}
