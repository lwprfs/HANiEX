package com.yenaly.han1meviewer.RedGifs.data.remote

import com.yenaly.han1meviewer.RedGifs.common.RedGifsConstants
import com.yenaly.han1meviewer.logic.network.HDns
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.logic.network.interceptor.UrlLoggingInterceptor
import com.yenaly.han1meviewer.logic.network.interceptor.UserAgentInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object RedGifsNetwork {

    @Volatile
    private var _client: OkHttpClient? = null

    @Volatile
    private var _streamClient: OkHttpClient? = null

    val baseUrl: String
        get() = RedGifsConstants.BASE_URL

    val client: OkHttpClient
        get() {
            ensure()
            return _client!!
        }

    val streamClient: OkHttpClient
        get() {
            ensure()
            return _streamClient!!
        }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
        if (original.header("Referer") == null) builder.header("Referer", RedGifsConstants.REFERER)
        if (original.header("Origin") == null) builder.header("Origin", RedGifsConstants.ORIGIN)
        if (original.header("Accept") == null) builder.header("Accept", "application/json")
        val token = RedGifsAuth.bearer()
        if (token != null) builder.header("Authorization", "Bearer $token")
        var response = chain.proceed(builder.build())
        if (response.code == 401 && token != null) {
            response.close()
            val fresh = RedGifsAuth.bearer(force = true) ?: token
            response = chain.proceed(builder.header("Authorization", "Bearer $fresh").build())
        }
        response
    }

    private val mediaHeaders = Interceptor { chain ->
        val request = chain.request()
        val builder = request.newBuilder()
        if (request.header("Referer") == null) builder.header("Referer", RedGifsConstants.REFERER)
        if (request.header("Origin") == null) builder.header("Origin", RedGifsConstants.ORIGIN)
        chain.proceed(builder.build())
    }

    @Synchronized
    private fun ensure() {
        if (_client != null && _streamClient != null) return
        _client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(UserAgentInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(UrlLoggingInterceptor())
            .dns(HDns())
            .proxySelector(HProxySelector())
            .build()
        _streamClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(UserAgentInterceptor)
            .addInterceptor(mediaHeaders)
            .dns(HDns())
            .proxySelector(HProxySelector())
            .build()
    }

    fun rebuildNetwork() {
        _client = null
        _streamClient = null
    }
}
