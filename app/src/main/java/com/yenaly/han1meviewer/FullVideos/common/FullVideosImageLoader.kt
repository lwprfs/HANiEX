package com.yenaly.han1meviewer.FullVideos.common

import android.content.Context
import android.util.Log
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.yenaly.han1meviewer.FullVideos.cloudflare.FullVideosCloudflareCookieManager
import com.yenaly.han1meviewer.FullVideos.cloudflare.FullVideosCloudflareInterceptor
import com.yenaly.han1meviewer.FullVideos.data.remote.FullVideosNetwork
import com.yenaly.han1meviewer.USER_AGENT
import okhttp3.Call
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import java.util.concurrent.TimeUnit

object FullVideosImageLoader {

    private const val TAG = "FullVideosImage"

    private fun isFullVideosHost(host: String): Boolean =
        FullVideosConstants.HOSTNAME.any { host == it || host.endsWith(".$it") }

    private val fullVideosInterceptor = Interceptor { chain ->
        val builder = chain.request().newBuilder()
            .header("Referer", "${FullVideosConstants.BASE_URL}/")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "image/webp,image/apng,image/jpeg,image/png,*/*;q=0.8")
        val clearance = FullVideosConstants.HOSTNAME.firstNotNullOfOrNull {
            FullVideosCloudflareCookieManager.getCloudflareCookie(it)
        }
        if (clearance != null) builder.header("Cookie", "cf_clearance=$clearance")
        val request = builder.build()
        val response = chain.proceed(request)
        if (!response.isSuccessful) {
            Log.w(TAG, "HTTP ${response.code} for ${request.url.host} (clearance cookie sent: ${clearance != null})")
        }
        response
    }

    private val fullVideosClient: OkHttpClient by lazy {
        val builder = FullVideosNetwork.sharedOkHttpClient.newBuilder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)

            .cookieJar(CookieJar.NO_COOKIES)

        builder.interceptors().removeAll { it is FullVideosCloudflareInterceptor }
        builder.addInterceptor(fullVideosInterceptor).build()
    }

    private val otherClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val callFactory = Call.Factory { request ->
        val client = if (isFullVideosHost(request.url.host)) fullVideosClient else otherClient
        client.newCall(request)
    }

    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader =
        instance ?: synchronized(this) {
            instance ?: build(context.applicationContext, "fullvideos_image_cache").also { instance = it }
        }

    private fun build(context: Context, cacheDirName: String): ImageLoader {
        val builder = ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { callFactory }))
            }
            .crossfade(true)
        builder.diskCache(
            DiskCache.Builder()
                .directory(context.cacheDir.resolve(cacheDirName).toOkioPath())
                .maxSizeBytes(256L * 1024 * 1024)
                .build()
        )
        return builder.build()
    }
}
