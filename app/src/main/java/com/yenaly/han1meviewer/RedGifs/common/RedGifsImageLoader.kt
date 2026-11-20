package com.yenaly.han1meviewer.RedGifs.common

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.yenaly.han1meviewer.RedGifs.data.remote.RedGifsNetwork
import okhttp3.Call
import okio.Path.Companion.toOkioPath

object RedGifsImageLoader {

    private val callFactory = Call.Factory { request ->
        RedGifsNetwork.streamClient.newCall(request)
    }

    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader =
        instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

    private fun build(context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { callFactory }))
            }
            .diskCache(
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("redgifs_image_cache").toOkioPath())
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            )
            .crossfade(true)
            .build()
}
