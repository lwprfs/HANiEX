package com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote

import android.content.Context
import android.util.Log
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote.StrawberryNetwork.baseUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.net.SocketTimeoutException
import java.net.UnknownHostException

enum class StrawberryProbeFailure {
    NoCookies,
    RateLimited,
    ServerError,
    SiteDown,
    NotFound,
    SessionExpired,
    Network,
    Unknown,
}

data class StrawberryProbeResult(
    val ok: Boolean,
    val failure: StrawberryProbeFailure? = null,
    val userMessage: String? = null,
    val httpCode: Int? = null,
    val suggestSwitchSite: Boolean = false,
    val suggestWait: Boolean = false,
    val suggestRelogin: Boolean = false,
)

object StrawberryAuthProbe {

    private const val TAG = "StrawberryAuthProbe"

    suspend fun probe(context: Context): StrawberryProbeResult = withContext(Dispatchers.IO) {
        if (!StrawberryCookieManager.hasCookies) {
            return@withContext StrawberryProbeResult(
                ok = false,
                failure = StrawberryProbeFailure.NoCookies,
                userMessage = "No cookies saved. Sign in or paste cookies to continue.",
                suggestRelogin = true,
            )
        }
        try {
            val url = baseUrl.trimEnd('/') + "/user/settings.php"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "text/html")
                .build()
            StrawberryNetwork.sharedOkHttpClient.newCall(request).execute().use { resp ->
                val code = resp.code
                val body = resp.body.string().orEmpty()
                Log.d(TAG, "probe /user/settings.php: code=$code, htmlLen=${body.length}")

                when {
                    code == 429 -> StrawberryProbeResult(
                        ok = false,
                        failure = StrawberryProbeFailure.RateLimited,
                        userMessage = "Strawberry is rate limiting requests (HTTP 429). " +
                                "Wait a few minutes before trying again.",
                        httpCode = code,
                        suggestWait = true,
                    )

                    code == 401 -> {
                        StrawberryCookieManager.clearCookies()
                        StrawberryProbeResult(
                            ok = false,
                            failure = StrawberryProbeFailure.SessionExpired,
                            userMessage = "Your session was rejected (HTTP 401). " +
                                    "The cookies are no longer valid — sign in again.",
                            httpCode = code,
                            suggestRelogin = true,
                        )
                    }

                    code == 403 -> StrawberryProbeResult(
                        ok = false,
                        failure = StrawberryProbeFailure.SessionExpired,
                        userMessage = "Strawberry refused the request (HTTP 403). " +
                                "Try switching mirrors, or wait a moment and retry.",
                        httpCode = code,
                        suggestSwitchSite = true,
                        suggestWait = true,
                    )

                    code in 500..599 -> StrawberryProbeResult(
                        ok = false,
                        failure = StrawberryProbeFailure.ServerError,
                        userMessage = "Strawberry returned a server error (HTTP $code). " +
                                "Wait a bit or switch mirrors.",
                        httpCode = code,
                        suggestSwitchSite = true,
                        suggestWait = true,
                    )

                    code == 404 -> StrawberryProbeResult(
                        ok = false,
                        failure = StrawberryProbeFailure.SiteDown,
                        userMessage = "This mirror returned 404. Switch mirrors.",
                        httpCode = code,
                        suggestSwitchSite = true,
                    )

                    !resp.isSuccessful -> StrawberryProbeResult(
                        ok = false,
                        failure = StrawberryProbeFailure.Unknown,
                        userMessage = "Unexpected response (HTTP $code). Try again.",
                        httpCode = code,
                        suggestSwitchSite = true,
                    )

                    else -> {
                        val looksLikeLogin =
                            body.contains("login.php", ignoreCase = true) ||
                                    body.contains("<title>Log in", ignoreCase = true) ||
                                    body.contains("<title>Login", ignoreCase = true)
                        val hasAuthMarker =
                            body.contains("nav-username", ignoreCase = true) ||
                                    body.contains("action=\"/logout\"", ignoreCase = true) ||
                                    body.contains("/exit.php", ignoreCase = true) ||
                                    body.contains("/user/settings.php", ignoreCase = true)
                        if (!hasAuthMarker || (looksLikeLogin && !hasAuthMarker)) {
                            StrawberryCookieManager.clearCookies()
                            StrawberryProbeResult(
                                ok = false,
                                failure = StrawberryProbeFailure.SessionExpired,
                                userMessage = "Your cookies are not signed in on this mirror. " +
                                        "Sign in again or paste fresh cookies.",
                                httpCode = code,
                                suggestRelogin = true,
                            )
                        } else {
                            StrawberryProbeResult(ok = true)
                        }
                    }
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "probe timeout", e)
            StrawberryProbeResult(
                ok = false,
                failure = StrawberryProbeFailure.Network,
                userMessage = "The request timed out. Tor might be slow.",
                suggestWait = true,
            )
        } catch (e: UnknownHostException) {
            Log.e(TAG, "probe DNS failure", e)
            StrawberryProbeResult(
                ok = false,
                failure = StrawberryProbeFailure.Network,
                userMessage = "Cannot resolve the .onion address. Confirm Tor is running.",
                suggestSwitchSite = true,
            )
        } catch (e: Exception) {
            Log.e(TAG, "probe failed", e)
            StrawberryProbeResult(
                ok = false,
                failure = StrawberryProbeFailure.Unknown,
                userMessage = "Could not reach Strawberry: " +
                        (e.message ?: e.javaClass.simpleName),
                suggestSwitchSite = true,
            )
        }
    }
}
