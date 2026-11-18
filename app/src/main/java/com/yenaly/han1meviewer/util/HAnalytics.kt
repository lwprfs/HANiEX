package com.yenaly.han1meviewer.util

import android.os.Bundle
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.crashlytics.setCustomKeys
import com.yenaly.han1meviewer.BuildConfig
import com.yenaly.han1meviewer.FirebaseConstants
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.exception.CloudFlareBlockedException
import com.yenaly.han1meviewer.logic.exception.HanimeNotFoundException
import com.yenaly.han1meviewer.logic.exception.IPBlockedException
import com.yenaly.han1meviewer.logic.exception.LoginStateExpiredException
import com.yenaly.han1meviewer.logic.exception.ParseException
import com.yenaly.han1meviewer.logic.network.HProxySelector
import javax.net.ssl.SSLHandshakeException

object HAnalytics {

    private const val TAG = "HAnalytics"

    private const val MAX_PARAM_VALUE_LENGTH = 100

    fun logEvent(name: String, vararg params: Pair<String, Any?>) {
        try {
            val bundle = Bundle()
            for ((key, value) in params) {
                when (value) {
                    null -> Unit
                    is String -> bundle.putString(key, value.take(MAX_PARAM_VALUE_LENGTH))
                    is Int -> bundle.putInt(key, value)
                    is Long -> bundle.putLong(key, value)
                    is Boolean -> bundle.putBoolean(key, value)
                    is Double -> bundle.putDouble(key, value)
                    is Float -> bundle.putFloat(key, value)
                    else -> bundle.putString(key, value.toString().take(MAX_PARAM_VALUE_LENGTH))
                }
            }
            Firebase.analytics.logEvent(name, bundle)
        } catch (t: Throwable) {

            logInternalFailure("logEvent($name)", t)
        }
    }

    fun logScreenView(screenName: String, screenClassName: String) {
        try {
            Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                param(FirebaseAnalytics.Param.SCREEN_NAME, screenName.take(MAX_PARAM_VALUE_LENGTH))
                param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClassName.take(MAX_PARAM_VALUE_LENGTH))
            }
        } catch (t: Throwable) {
            logInternalFailure("logScreenView($screenName)", t)
        }
    }

    fun logError(
        eventName: String,
        source: String,
        throwable: Throwable?,
        vararg params: Pair<String, Any?>,
    ) {
        try {
            val extra = mutableListOf<Pair<String, Any?>>(
                FirebaseConstants.PARAM_SOURCE to source,
                FirebaseConstants.PARAM_EXCEPTION_CLASS to throwable?.javaClass?.simpleName,
            )
            throwable?.message?.takeIf { it.isNotBlank() }?.let {
                extra.add(FirebaseConstants.PARAM_EXCEPTION_MSG to it)
            }
            extractHttpCode(throwable)?.let {
                extra.add(FirebaseConstants.PARAM_HTTP_CODE to it)
            }
            extra.addAll(params)
            logEvent(eventName, *extra.toTypedArray())

            if (throwable != null && isBugSignal(throwable)) {
                recordNonFatal(source, throwable)
            }
        } catch (t: Throwable) {

            logInternalFailure("logError($source)", t)
        }
    }

    fun logNetworkError(source: String, throwable: Throwable) {
        logError(FirebaseConstants.EVENT_NETWORK_ERROR, source, throwable)
    }

    private fun recordNonFatal(source: String, throwable: Throwable) {
        try {
            Firebase.crashlytics.log("HAnalytics: non-fatal at source=$source")
            Firebase.crashlytics.recordException(throwable)
        } catch (t: Throwable) {
            logInternalFailure("recordNonFatal($source)", t)
        }
    }

    private fun isBugSignal(throwable: Throwable): Boolean {
        if (BuildConfig.DEBUG) return false
        return throwable !is IPBlockedException &&
                throwable !is CloudFlareBlockedException &&
                throwable !is HanimeNotFoundException &&
                throwable !is LoginStateExpiredException
    }

    private fun extractHttpCode(throwable: Throwable?): Int? {
        val message = throwable?.message ?: return null
        val match = Regex("^\\s*(\\d{3})\\b").find(message) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    fun updateNetworkContextKeys() {
        try {
            Firebase.crashlytics.setCustomKeys {
                key(FirebaseConstants.CURRENT_SITE, Preferences.baseUrl.take(MAX_PARAM_VALUE_LENGTH))
                key(FirebaseConstants.USE_CUSTOM_MIRROR, Preferences.useCustomMirrorSite)
                key(FirebaseConstants.PROXY_TYPE, proxyTypeName(Preferences.proxyType))
                key(FirebaseConstants.DOH_ENABLED, Preferences.useDoH)
                key(FirebaseConstants.DOH_PRESET, Preferences.dohPreset)
                key(FirebaseConstants.USE_BUILTIN_HOSTS, Preferences.useBuiltInHosts)
                key(FirebaseConstants.USE_BACKUP_CDN, Preferences.useBackupMediaCdn)
            }
        } catch (t: Throwable) {
            logInternalFailure("updateNetworkContextKeys", t)
        }
    }

    private fun logInternalFailure(where: String, t: Throwable) {
        try {
            Log.w(TAG, "$where failed.", t)
        } catch (_: Throwable) {

        }
    }

    private fun proxyTypeName(type: Int): String = when (type) {
        HProxySelector.TYPE_DIRECT -> "direct"
        HProxySelector.TYPE_SYSTEM -> "system"
        HProxySelector.TYPE_HTTP -> "http"
        HProxySelector.TYPE_SOCKS -> "socks"
        else -> "unknown"
    }

}
