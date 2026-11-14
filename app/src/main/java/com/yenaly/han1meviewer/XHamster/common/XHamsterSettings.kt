package com.yenaly.han1meviewer.XHamster.common

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yenaly.han1meviewer.Preferences

object XHamsterSettings {

    private const val KEY_QUALITY = "xhamster_pref_quality"

    private val sp get() = Preferences.preferenceSp

    var preferredQuality: String
        get() = sp.getString(KEY_QUALITY, null)
            ?.takeIf { it in XHamsterConstants.QUALITIES } ?: XHamsterConstants.DEFAULT_QUALITY
        set(value) {
            sp.edit().putString(KEY_QUALITY, value).apply()
        }

    // Home / discover strip visibility. Keys are XHamster-specific so no other site is affected.
    const val KEY_SHOW_CHANNELS = "xhamster_show_channels"
    const val KEY_SHOW_TOP_CREATORS = "xhamster_show_top_creators"

    var showChannels: Boolean
        get() = sp.getBoolean(KEY_SHOW_CHANNELS, true)
        set(value) {
            sp.edit().putBoolean(KEY_SHOW_CHANNELS, value).apply()
        }

    var showTopCreators: Boolean
        get() = sp.getBoolean(KEY_SHOW_TOP_CREATORS, true)
        set(value) {
            sp.edit().putBoolean(KEY_SHOW_TOP_CREATORS, value).apply()
        }
}

/** Boolean preference that recomposes as soon as it changes (e.g. from the Customize page). */
@Composable
fun rememberXHamsterFlag(key: String, default: Boolean = true): Boolean {
    val sp = Preferences.preferenceSp
    var value by remember(key) { mutableStateOf(sp.getBoolean(key, default)) }
    DisposableEffect(key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, changed ->
            if (changed == key) value = prefs.getBoolean(key, default)
        }
        sp.registerOnSharedPreferenceChangeListener(listener)
        value = sp.getBoolean(key, default)
        onDispose { sp.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return value
}
