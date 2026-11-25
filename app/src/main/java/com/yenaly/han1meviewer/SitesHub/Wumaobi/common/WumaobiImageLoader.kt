package com.yenaly.han1meviewer.SitesHub.Wumaobi.common

import android.content.Context
import android.util.Log
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.yenaly.han1meviewer.SitesHub.Wumaobi.cloudflare.WumaobiCloudflareCookieManager
import com.yenaly.han1meviewer.SitesHub.Wumaobi.cloudflare.WumaobiCloudflareInterceptor
import com.yenaly.han1meviewer.SitesHub.Wumaobi.data.remote.WumaobiNetwork
import com.yenaly.han1meviewer.USER_AGENT
import okhttp3.Call
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import java.util.concurrent.TimeUnit

object WumaobiImageLoader {

    private const val TAG = "WumaobiImage"

    private fun isWumaobiHost(host: String): Boolean =
        WumaobiConstants.HOSTNAME.any { host == it || host.endsWith(".$it") }

    private val wumaobiInterceptor = Interceptor { chain ->
        val builder = chain.request().newBuilder()
            .header("Referer", "${WumaobiConstants.BASE_URL}/")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "image/webp,image/apng,image/jpeg,image/png,*/*;q=0.8")
        val clearance = WumaobiConstants.HOSTNAME.firstNotNullOfOrNull {
            WumaobiCloudflareCookieManager.getCloudflareCookie(it)
        }
        if (clearance != null) builder.header("Cookie", "cf_clearance=$clearance")
        val request = builder.build()
        val response = chain.proceed(request)
        if (!response.isSuccessful) {
            Log.w(TAG, "HTTP ${response.code} for ${request.url.host} (clearance cookie sent: ${clearance != null})")
        }
        response
    }

    private val wumaobiClient: OkHttpClient by lazy {
        val builder = WumaobiNetwork.sharedOkHttpClient.newBuilder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)

            .cookieJar(CookieJar.NO_COOKIES)

        builder.interceptors().removeAll { it is WumaobiCloudflareInterceptor }
        builder.addInterceptor(wumaobiInterceptor).build()
    }

    private val otherClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val callFactory = Call.Factory { request ->
        val client = if (isWumaobiHost(request.url.host)) wumaobiClient else otherClient
        client.newCall(request)
    }

    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader =
        instance ?: synchronized(this) {
            instance ?: build(context.applicationContext, "wumaobi_image_cache").also { instance = it }
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
