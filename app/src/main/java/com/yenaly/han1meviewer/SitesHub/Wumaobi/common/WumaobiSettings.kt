package com.yenaly.han1meviewer.SitesHub.Wumaobi.common

import com.yenaly.han1meviewer.Preferences

object WumaobiSettings {

    private const val KEY_QUALITY = "wumaobi_pref_quality"

    private const val KEY_SECTION_ORDER = "wumaobi_section_order"
    private const val KEY_SECTION_HIDDEN = "wumaobi_sections_hidden"

    private val sp get() = Preferences.preferenceSp

    private fun readList(key: String): List<String> =
        sp.getString(key, null).orEmpty()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    var sectionOrder: List<String>
        get() {
            val all = WumaobiConstants.SECTIONS.map { it.key }
            val saved = readList(KEY_SECTION_ORDER).filter { it in all }.distinct()
            return saved + all.filter { it !in saved }
        }
        set(value) {
            sp.edit().putString(KEY_SECTION_ORDER, value.joinToString(",")).apply()
        }

    var hiddenSections: Set<String>
        get() = readList(KEY_SECTION_HIDDEN).toSet()
        set(value) {
            sp.edit().putString(KEY_SECTION_HIDDEN, value.joinToString(",")).apply()
        }

    fun visibleSections(): List<WumaobiConstants.Section> {
        val byKey = WumaobiConstants.SECTIONS.associateBy { it.key }
        val ordered = sectionOrder.mapNotNull { byKey[it] }
        val hidden = hiddenSections
        return ordered.filter { it.key !in hidden }.ifEmpty { ordered }
    }

    var preferredQuality: String
        get() = sp.getString(KEY_QUALITY, null)
            ?.takeIf { it in WumaobiConstants.QUALITIES } ?: WumaobiConstants.DEFAULT_QUALITY
        set(value) {
            sp.edit().putString(KEY_QUALITY, value).apply()
        }

    fun resetToDefaults() {
        sp.edit().remove(KEY_QUALITY).remove(KEY_SECTION_ORDER).remove(KEY_SECTION_HIDDEN).apply()
    }
}
