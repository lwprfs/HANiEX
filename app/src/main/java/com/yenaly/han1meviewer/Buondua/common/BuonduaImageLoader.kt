package com.yenaly.han1meviewer.Buondua.common

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.yenaly.han1meviewer.Buondua.cloudflare.BuonduaCloudflareInterceptor
import com.yenaly.han1meviewer.Buondua.data.remote.BuonduaNetwork
import com.yenaly.han1meviewer.USER_AGENT
import okhttp3.Call
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import java.util.concurrent.TimeUnit

/**
 * Album images are hot-link protected, so every request carries the site Referer
 * (the page itself sends `referrerpolicy="strict-origin"`).
 */
object BuonduaImageLoader {

    private val refererInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("Referer", BuonduaConstants.REFERER)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "image/webp,image/apng,image/jpeg,image/png,*/*;q=0.8")
            .build()
        chain.proceed(request)
    }

    private val siteClient: OkHttpClient by lazy {
        val builder = BuonduaNetwork.sharedOkHttpClient.newBuilder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .cookieJar(CookieJar.NO_COOKIES)
        builder.interceptors().removeAll { it is BuonduaCloudflareInterceptor }
        builder.addInterceptor(refererInterceptor).build()
    }

    private val callFactory = Call.Factory { request -> siteClient.newCall(request) }

    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader =
        instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

    private fun build(context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { callFactory })) }
            .crossfade(true)
            .diskCache(
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("buondua_image_cache").toOkioPath())
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            )
            .build()
}
