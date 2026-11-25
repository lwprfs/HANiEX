package com.yenaly.han1meviewer.ui.navigation.settings

import android.content.Context
import androidx.core.net.toUri
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.SitesHub.Hanime1.logic.Parser
import com.yenaly.han1meviewer.logic.network.MirrorLatency
import com.yenaly.han1meviewer.logic.network.ServiceCreator
import com.yenaly.han1meviewer.logic.state.WebsiteState
import okhttp3.Request

internal fun normalizeCustomMirrorSite(url: String): String? {
    val trimmed = url.trim().trimEnd('/')
    val uri = runCatching { trimmed.toUri() }.getOrNull() ?: return null
    val host = uri.host
    if (host.isNullOrBlank()) return null
    val schemeOk = uri.scheme == "https" || (uri.scheme == "http" && host.endsWith(".onion"))
    if (!schemeOk) return null
    if (!uri.query.isNullOrBlank() || !uri.fragment.isNullOrBlank()) return null
    return url.trim()
}

internal fun testCustomMirrorSite(
    context: Context,
    site: SiteType,
    homeUrl: String,
    appendPath: Boolean,
): String {
    if (site != SiteType.HANIME && site != SiteType.JAVCHU) {
        return testGenericMirrorSite(context, homeUrl, appendPath)
    }
    return runCatching {
        val request = Request.Builder().url(homeUrl).get().build()
        ServiceCreator.hClient.newCall(request).execute().use { response ->
            val finalUrl = response.request.url.toString()
            val body = response.body.string()
            if (!response.isSuccessful) {
                return context.getString(
                    R.string.custom_mirror_site_test_failed_http,
                    response.code,
                    finalUrl,
                )
            }

            val apiBaseUrl = buildCustomMirrorApiBaseUrl(homeUrl, appendPath)
            val watchTestResult = testCustomMirrorWatchUrl(context, apiBaseUrl)
            when (val parseResult = Parser.homePageVer2(body)) {
                is WebsiteState.Success -> if (watchTestResult == null) {
                    context.getString(
                        R.string.custom_mirror_site_test_success,
                        finalUrl,
                        apiBaseUrl,
                    )
                } else {
                    context.getString(
                        R.string.custom_mirror_site_test_partial_success,
                        finalUrl,
                        apiBaseUrl,
                        watchTestResult,
                    )
                }

                is WebsiteState.Error -> context.getString(
                    R.string.custom_mirror_site_test_parse_failed,
                    finalUrl,
                    parseResult.throwable.message ?: parseResult.throwable::class.java.simpleName,
                )

                WebsiteState.Loading -> context.getString(
                    R.string.custom_mirror_site_test_parse_failed,
                    finalUrl,
                    context.getString(R.string.loading),
                )
            }
        }
    }.getOrElse { throwable ->
        context.getString(
            R.string.custom_mirror_site_test_failed,
            throwable.message ?: throwable::class.java.simpleName,
        )
    }
}

internal fun testGenericMirrorSite(context: Context, homeUrl: String, appendPath: Boolean): String {
    return runCatching {
        val request = Request.Builder().url(homeUrl).get().build()
        MirrorLatency.httpClient.newCall(request).execute().use { response ->
            val finalUrl = response.request.url.toString()
            val code = response.code
            if (code >= 500 || (code in 400..499 && code != 403)) {
                context.getString(R.string.custom_mirror_site_test_failed_http, code, finalUrl)
            } else {
                context.getString(
                    R.string.custom_mirror_site_test_success,
                    finalUrl,
                    buildCustomMirrorApiBaseUrl(homeUrl, appendPath),
                )
            }
        }
    }.getOrElse { throwable ->
        context.getString(
            R.string.custom_mirror_site_test_failed,
            throwable.message ?: throwable::class.java.simpleName,
        )
    }
}

internal fun testCustomMirrorWatchUrl(context: Context, apiBaseUrl: String): String? {
    return runCatching {
        val url = apiBaseUrl + "search"
        val request = Request.Builder().url(url).get().build()
        ServiceCreator.hClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                null
            } else {
                context.getString(
                    R.string.custom_mirror_site_watch_test_failed_http,
                    response.code,
                    response.request.url.toString(),
                )
            }
        }
    }.getOrElse { throwable ->
        context.getString(
            R.string.custom_mirror_site_watch_test_failed,
            throwable.message ?: throwable::class.java.simpleName,
        )
    }
}

internal fun buildCustomMirrorApiBaseUrl(homeUrl: String, appendPath: Boolean): String {
    val url = if (appendPath) homeUrl else {
        val uri = homeUrl.toUri()
        "${uri.scheme}://${uri.encodedAuthority}"
    }
    return if (url.endsWith('/')) url else "$url/"
}
