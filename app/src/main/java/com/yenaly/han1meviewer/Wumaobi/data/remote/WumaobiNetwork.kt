package com.yenaly.han1meviewer.Wumaobi.data.remote

import com.yenaly.han1meviewer.Wumaobi.cloudflare.WumaobiCloudflareInterceptor
import com.yenaly.han1meviewer.Wumaobi.common.WumaobiConstants
import com.yenaly.han1meviewer.logic.network.HDns
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.logic.network.interceptor.UrlLoggingInterceptor
import com.yenaly.han1meviewer.logic.network.interceptor.UserAgentInterceptor
import com.yenaly.han1meviewer.util.applicationContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object WumaobiNetwork {

    const val HEADER_SORT = "X-Wmb-Sort"
    const val HEADER_CATEGORY_SORT = "X-Wmb-Category-Sort"

    /**
     * The site picks list ordering from the `sort` / `category_sort` cookies. They are applied
     * per request here (as a network interceptor, after OkHttp has attached the jar cookies) so
     * two screens with different sorts never race on shared cookie state.
     */
    private val sortCookieInterceptor = Interceptor { chain ->
        val request = chain.request()
        val sort = request.header(HEADER_SORT)
        val categorySort = request.header(HEADER_CATEGORY_SORT)
        if (sort == null && categorySort == null) return@Interceptor chain.proceed(request)
        val cookies = request.header("Cookie").orEmpty()
            .split(';')
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("sort=") && !it.startsWith("category_sort=") }
            .toMutableList()
        if (sort != null) cookies += "sort=$sort"
        if (categorySort != null) cookies += "category_sort=$categorySort"
        chain.proceed(
            request.newBuilder()
                .removeHeader(HEADER_SORT)
                .removeHeader(HEADER_CATEGORY_SORT)
                .header("Cookie", cookies.joinToString("; "))
                .build()
        )
    }

    @Volatile
    private var _service: WumaobiService? = null

    @Volatile
    private var _client: OkHttpClient? = null

    @Volatile
    private var _cookieJar: WumaobiCookieJar? = null

    val baseUrl: String
        get() = WumaobiConstants.BASE_URL

    val service: WumaobiService
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
            .addInterceptor(WumaobiCloudflareInterceptor(applicationContext))
            .addNetworkInterceptor(sortCookieInterceptor)
            .dns(HDns())
            .proxySelector(HProxySelector())
            .build()
        _client = client
        _service = Retrofit.Builder()
            .baseUrl(baseUrl + "/")
            .client(client)
            .build()
            .create(WumaobiService::class.java)
    }

    private fun getOrCreateCookieJar(): WumaobiCookieJar =
        _cookieJar ?: WumaobiCookieJar().also { _cookieJar = it }

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
        runCatching { WumaobiAuth.reload() }
    }
}
