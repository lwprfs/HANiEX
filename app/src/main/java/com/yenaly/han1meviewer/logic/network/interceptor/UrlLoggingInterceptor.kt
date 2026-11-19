package com.yenaly.han1meviewer.logic.network.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class UrlLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        // Only log method + host to avoid leaking tokens in queries or browsing history.
        Log.i("NetworkRequest", "${request.method} ${request.url.host}")
        return chain.proceed(request)
    }
}
