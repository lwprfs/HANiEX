package com.yenaly.han1meviewer.SitesHub.LeakGallery.common

import com.yenaly.han1meviewer.Preferences

object LeakGallerySettings {

    private const val KEY_PHOTO_COLUMNS = "leakgallery_photo_columns"
    private const val KEY_VIEWER_TAP_NEXT = "leakgallery_viewer_tap_next"
    private const val KEY_DEFAULT_SECTION = "leakgallery_default_section"

    private val sp get() = Preferences.preferenceSp

    var photoColumns: Int
        get() = sp.getInt(KEY_PHOTO_COLUMNS, LeakGalleryConstants.DEFAULT_PHOTO_COLUMNS)
            .takeIf { it in LeakGalleryConstants.PHOTO_COLUMN_OPTIONS }
            ?: LeakGalleryConstants.DEFAULT_PHOTO_COLUMNS
        set(value) {
            sp.edit().putInt(KEY_PHOTO_COLUMNS, value).apply()
        }

    var viewerTapToFlip: Boolean
        get() = sp.getBoolean(KEY_VIEWER_TAP_NEXT, true)
        set(value) {
            sp.edit().putBoolean(KEY_VIEWER_TAP_NEXT, value).apply()
        }

    /** Key of the home chip that is selected when the site opens. */
    var defaultSection: String
        get() = sp.getString(KEY_DEFAULT_SECTION, null)
            ?.takeIf { key -> LeakGalleryConstants.HOME_SECTIONS.any { it.key == key } }
            ?: LeakGalleryConstants.SECTION_TRENDING.key
        set(value) {
            sp.edit().putString(KEY_DEFAULT_SECTION, value).apply()
        }

    fun resetToDefaults() {
        sp.edit()
            .remove(KEY_PHOTO_COLUMNS)
            .remove(KEY_VIEWER_TAP_NEXT)
            .remove(KEY_DEFAULT_SECTION)
            .apply()
    }
}
