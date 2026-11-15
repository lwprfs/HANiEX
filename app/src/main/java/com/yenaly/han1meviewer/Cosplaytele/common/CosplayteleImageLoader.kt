package com.yenaly.han1meviewer.Cosplaytele.common

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.yenaly.han1meviewer.Cosplaytele.cloudflare.CosplayteleCloudflareCookieManager
import com.yenaly.han1meviewer.Cosplaytele.cloudflare.CosplayteleCloudflareInterceptor
import com.yenaly.han1meviewer.Cosplaytele.data.remote.CosplayteleNetwork
import com.yenaly.han1meviewer.USER_AGENT
import okhttp3.Call
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import java.util.concurrent.TimeUnit

/**
 * Photos are served from the site's own `wp-content/uploads` and need the post / site Referer.
 * Every image request of this site goes through [get].
 */
object CosplayteleImageLoader {

    private fun isSiteHost(host: String): Boolean =
        CosplayteleConstants.HOSTNAME.any { host == it || host.endsWith(".$it") }

    private val siteInterceptor = Interceptor { chain ->
        val builder = chain.request().newBuilder()
        if (chain.request().header("Referer") == null) {
            builder.header("Referer", "${CosplayteleConstants.BASE_URL}/")
        }
        builder.header("User-Agent", USER_AGENT)
            .header("Accept", "image/webp,image/apng,image/jpeg,image/png,*/*;q=0.8")
        val clearance = CosplayteleConstants.HOSTNAME.firstNotNullOfOrNull {
            CosplayteleCloudflareCookieManager.getCloudflareCookie(it)
        }
        if (clearance != null) builder.header("Cookie", "cf_clearance=$clearance")
        chain.proceed(builder.build())
    }

    private val siteClient: OkHttpClient by lazy {
        val builder = CosplayteleNetwork.sharedOkHttpClient.newBuilder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .cookieJar(CookieJar.NO_COOKIES)
        builder.interceptors().removeAll { it is CosplayteleCloudflareInterceptor }
        builder.addInterceptor(siteInterceptor).build()
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

    /** Client used for saving photos to disk (adds the headers the CDN expects). */
    fun downloadClient(): OkHttpClient = siteClient

    fun downloadHeaders(referer: String? = null): Map<String, String> =
        mapOf("Referer" to (referer ?: "${CosplayteleConstants.BASE_URL}/"))

    private fun build(context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { callFactory })) }
            .crossfade(true)
            .diskCache(
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("cosplaytele_image_cache").toOkioPath())
                    .maxSizeBytes(512L * 1024 * 1024)
                    .build()
            )
            .build()
}
