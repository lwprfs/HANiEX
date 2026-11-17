package com.yenaly.han1meviewer.FullVideos.common

import com.yenaly.han1meviewer.Preferences

object FullVideosSettings {

    private const val KEY_QUALITY = "fullvideos_pref_quality"

    private val sp get() = Preferences.preferenceSp

    var preferredQuality: String
        get() = sp.getString(KEY_QUALITY, null)
            ?.takeIf { it in FullVideosConstants.QUALITIES } ?: FullVideosConstants.DEFAULT_QUALITY
        set(value) {
            sp.edit().putString(KEY_QUALITY, value).apply()
        }

    fun resetToDefaults() {
        sp.edit().remove(KEY_QUALITY).apply()
    }
}
