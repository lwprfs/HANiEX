package com.yenaly.han1meviewer.SitesHub.XHamster.common

import com.yenaly.han1meviewer.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object XHamsterSession {

    private val _epoch = MutableStateFlow(0)
    val epoch: StateFlow<Int> get() = _epoch

    fun bump() {
        _epoch.value = _epoch.value + 1
    }

    private const val KEY_ORIENTATION = "xhamster_site_orientation"
    private const val KEY_GEO = "xhamster_site_geo"
    private const val KEY_LANG = "xhamster_site_lang"
    private const val KEY_LANG_LABEL = "xhamster_site_lang_label"
    private const val KEY_COLLECTION = "xhamster_default_collection"

    private val sp get() = Preferences.preferenceSp

    var orientation: String
        get() = sp.getString(KEY_ORIENTATION, null) ?: "straight"
        set(value) = sp.edit().putString(KEY_ORIENTATION, value).apply()

    var geo: String
        get() = sp.getString(KEY_GEO, null).orEmpty()
        set(value) = sp.edit().putString(KEY_GEO, value).apply()

    var lang: String
        get() = sp.getString(KEY_LANG, null) ?: "en"
        set(value) = sp.edit().putString(KEY_LANG, value).apply()

    var langLabel: String
        get() = sp.getString(KEY_LANG_LABEL, null) ?: "English"
        set(value) = sp.edit().putString(KEY_LANG_LABEL, value).apply()

    var defaultCollectionId: String
        get() = sp.getString(KEY_COLLECTION, null).orEmpty()
        set(value) = sp.edit().putString(KEY_COLLECTION, value).apply()
}
