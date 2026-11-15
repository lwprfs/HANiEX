package com.yenaly.han1meviewer.Cosplaytele.common

import com.yenaly.han1meviewer.Preferences

object CosplayteleSettings {

    private const val KEY_QUALITY = "cosplaytele_pref_quality"
    private const val KEY_PHOTO_COLUMNS = "cosplaytele_photo_columns"
    private const val KEY_VIEWER_TAP_NEXT = "cosplaytele_viewer_tap_next"

    private val sp get() = Preferences.preferenceSp

    var preferredQuality: String
        get() = sp.getString(KEY_QUALITY, null)
            ?.takeIf { it in CosplayteleConstants.QUALITIES } ?: CosplayteleConstants.DEFAULT_QUALITY
        set(value) {
            sp.edit().putString(KEY_QUALITY, value).apply()
        }

    var photoColumns: Int
        get() = sp.getInt(KEY_PHOTO_COLUMNS, CosplayteleConstants.DEFAULT_PHOTO_COLUMNS)
            .takeIf { it in CosplayteleConstants.PHOTO_COLUMN_OPTIONS }
            ?: CosplayteleConstants.DEFAULT_PHOTO_COLUMNS
        set(value) {
            sp.edit().putInt(KEY_PHOTO_COLUMNS, value).apply()
        }

    /** Tapping the left / right third of the photo viewer flips the page. */
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
