package com.yenaly.han1meviewer.SitesHub.XXXTube.common

import android.content.Context
import android.util.Log
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.yenaly.han1meviewer.SitesHub.XXXTube.cloudflare.XXXTubeCloudflareCookieManager
import com.yenaly.han1meviewer.SitesHub.XXXTube.cloudflare.XXXTubeCloudflareInterceptor
import com.yenaly.han1meviewer.SitesHub.XXXTube.data.remote.XXXTubeNetwork
import com.yenaly.han1meviewer.USER_AGENT
import okhttp3.Call
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import java.util.concurrent.TimeUnit

object XXXTubeImageLoader {

    private const val TAG = "XXXTubeImage"

    private fun isXXXTubeHost(host: String): Boolean =
        XXXTubeConstants.HOSTNAME.any { host == it || host.endsWith(".$it") }

    private val xxxTubeInterceptor = Interceptor { chain ->
        val builder = chain.request().newBuilder()
            .header("Referer", "${XXXTubeConstants.BASE_URL}/")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "image/webp,image/apng,image/jpeg,image/png,*/*;q=0.8")
        val clearance = XXXTubeConstants.HOSTNAME.firstNotNullOfOrNull {
            XXXTubeCloudflareCookieManager.getCloudflareCookie(it)
        }
        if (clearance != null) builder.header("Cookie", "cf_clearance=$clearance")
        val request = builder.build()
        val response = chain.proceed(request)
        if (!response.isSuccessful) {
            Log.w(TAG, "HTTP ${response.code} for ${request.url.host} (clearance cookie sent: ${clearance != null})")
        }
        response
    }

    private val xxxTubeClient: OkHttpClient by lazy {
        val builder = XXXTubeNetwork.sharedOkHttpClient.newBuilder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)

            .cookieJar(CookieJar.NO_COOKIES)

        builder.interceptors().removeAll { it is XXXTubeCloudflareInterceptor }
        builder.addInterceptor(xxxTubeInterceptor).build()
    }

    private val otherClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val callFactory = Call.Factory { request ->
        val client = if (isXXXTubeHost(request.url.host)) xxxTubeClient else otherClient
        client.newCall(request)
    }

    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader =
        instance ?: synchronized(this) {
            instance ?: build(context.applicationContext, "xxxtube_image_cache").also { instance = it }
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
