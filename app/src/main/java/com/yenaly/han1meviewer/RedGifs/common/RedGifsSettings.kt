package com.yenaly.han1meviewer.RedGifs.common

import com.yenaly.han1meviewer.Preferences

object RedGifsSettings {

    private const val KEY_QUALITY = "redgifs_quality"
    private const val KEY_VERIFIED_ONLY = "redgifs_verified_only"

    const val QUALITY_HD = "hd"
    const val QUALITY_SD = "sd"

    private val sp get() = Preferences.preferenceSp

    var quality: String
        get() = sp.getString(KEY_QUALITY, null)?.takeIf { it == QUALITY_SD } ?: QUALITY_HD
        set(value) = sp.edit().putString(KEY_QUALITY, value).apply()

    /** Matches the web app's explore default (`verified=y`). */
    var verifiedOnly: Boolean
        get() = sp.getBoolean(KEY_VERIFIED_ONLY, true)
        set(value) = sp.edit().putBoolean(KEY_VERIFIED_ONLY, value).apply()

    fun resetToDefaults() {
        sp.edit().remove(KEY_QUALITY).remove(KEY_VERIFIED_ONLY).apply()
    }
}
