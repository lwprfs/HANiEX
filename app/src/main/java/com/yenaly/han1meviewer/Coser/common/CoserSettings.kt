package com.yenaly.han1meviewer.Coser.common

import com.yenaly.han1meviewer.Preferences

object CoserSettings {

    private const val KEY_QUALITY = "coser_pref_quality"
    private const val KEY_PHOTO_COLUMNS = "coser_photo_columns"
    private const val KEY_VIEWER_TAP_NEXT = "coser_viewer_tap_next"

    private val sp get() = Preferences.preferenceSp

    var preferredQuality: String
        get() = sp.getString(KEY_QUALITY, null)
            ?.takeIf { it in CoserConstants.QUALITIES } ?: CoserConstants.DEFAULT_QUALITY
        set(value) {
            sp.edit().putString(KEY_QUALITY, value).apply()
        }

    var photoColumns: Int
        get() = sp.getInt(KEY_PHOTO_COLUMNS, CoserConstants.DEFAULT_PHOTO_COLUMNS)
            .takeIf { it in CoserConstants.PHOTO_COLUMN_OPTIONS }
            ?: CoserConstants.DEFAULT_PHOTO_COLUMNS
        set(value) {
            sp.edit().putInt(KEY_PHOTO_COLUMNS, value).apply()
        }

    var viewerTapToFlip: Boolean
        get() = sp.getBoolean(KEY_VIEWER_TAP_NEXT, true)
        set(value) {
            sp.edit().putBoolean(KEY_VIEWER_TAP_NEXT, value).apply()
        }

    fun resetToDefaults() {
        sp.edit()
            .remove(KEY_QUALITY)
            .remove(KEY_PHOTO_COLUMNS)
            .remove(KEY_VIEWER_TAP_NEXT)
            .apply()
    }
}
