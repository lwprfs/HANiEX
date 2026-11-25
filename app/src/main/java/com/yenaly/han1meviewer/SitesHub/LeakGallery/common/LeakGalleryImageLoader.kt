package com.yenaly.han1meviewer.SitesHub.LeakGallery.common

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote.LeakGalleryNetwork
import com.yenaly.han1meviewer.USER_AGENT
import okhttp3.Call
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import java.util.concurrent.TimeUnit

object LeakGalleryImageLoader {

    private fun isSiteHost(host: String): Boolean =
        host == "cdn.leakgallery.com" || LeakGalleryConstants.HOSTNAME.any { host == it || host.endsWith(".$it") }

    private val siteInterceptor = Interceptor { chain ->
        val builder = chain.request().newBuilder()
        if (chain.request().header("Referer") == null) {
            builder.header("Referer", "${LeakGalleryConstants.BASE_URL}/")
        }
        if (chain.request().header("Origin") == null) {
            builder.header("Origin", LeakGalleryConstants.BASE_URL)
        }
        builder.header("User-Agent", USER_AGENT)
            .header("Accept", "image/webp,image/apng,image/jpeg,image/png,*/*;q=0.8")
        chain.proceed(builder.build())
    }

    private val siteClient: OkHttpClient by lazy {
        LeakGalleryNetwork.sharedOkHttpClient.newBuilder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .cookieJar(CookieJar.NO_COOKIES)
            .addInterceptor(siteInterceptor)
            .build()
    }

    private val otherClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val callFactory = Call.Factory { request ->
        val client = if (isSiteHost(request.url.host)) siteClient else otherClient
        client.newCall(request)
    }

    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader =
        instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

    fun downloadClient(): OkHttpClient = siteClient

    fun downloadHeaders(referer: String? = null): Map<String, String> =
        mapOf(
            "Referer" to (referer ?: "${LeakGalleryConstants.BASE_URL}/"),
            "Origin" to LeakGalleryConstants.BASE_URL,
        )

    private fun build(context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { callFactory })) }
            .crossfade(true)
            .diskCache(
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("leakgallery_image_cache").toOkioPath())
                    .maxSizeBytes(512L * 1024 * 1024)
                    .build()
            )
            .build()
}
