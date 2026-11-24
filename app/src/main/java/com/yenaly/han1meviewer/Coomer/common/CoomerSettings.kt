package com.yenaly.han1meviewer.Coomer.common

import com.yenaly.han1meviewer.Preferences

object CoomerSettings {

    private const val KEY_PHOTO_COLUMNS = "coomer_photo_columns"
    private const val KEY_VIEWER_TAP_NEXT = "coomer_viewer_tap_next"

    private val sp get() = Preferences.preferenceSp

    var photoColumns: Int
        get() = sp.getInt(KEY_PHOTO_COLUMNS, CoomerConstants.DEFAULT_PHOTO_COLUMNS)
            .takeIf { it in CoomerConstants.PHOTO_COLUMN_OPTIONS }
            ?: CoomerConstants.DEFAULT_PHOTO_COLUMNS
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
            .remove(KEY_PHOTO_COLUMNS)
            .remove(KEY_VIEWER_TAP_NEXT)
            .apply()
    }
}
