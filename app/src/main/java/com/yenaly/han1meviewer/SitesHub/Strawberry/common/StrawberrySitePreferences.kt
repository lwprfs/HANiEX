package com.yenaly.han1meviewer.SitesHub.Strawberry.common

import androidx.core.content.edit
import com.yenaly.han1meviewer.Preferences

object StrawberrySitePreferences {

    private const val PREF_STRAWBERRY_HOST = "strawberry_host"
    private const val PREF_STRAWBERRY_MIRROR_INDEX = "strawberry_mirror_index"

    private const val PREF_STRAWBERRY_QUALITY = "strawberry_preferred_quality"

    var preferredQuality: String
        get() = Preferences.preferenceSp.getString(PREF_STRAWBERRY_QUALITY, null)
            ?.takeIf(StrawberryQuality::isKnownChoice) ?: StrawberryQuality.ORIGINAL
        set(value) {
            Preferences.preferenceSp.edit {
                putString(PREF_STRAWBERRY_QUALITY, value.takeIf(StrawberryQuality::isKnownChoice) ?: StrawberryQuality.ORIGINAL)
            }
        }

    val selectedMirrorIndex: Int
        get() = Preferences.preferenceSp.getInt(PREF_STRAWBERRY_MIRROR_INDEX, 0)
            .coerceIn(0, StrawberryConstants.STRAWBERRY_URL.lastIndex)

    val selectedBaseUrl: String
        get() = StrawberryConstants.STRAWBERRY_URL[selectedMirrorIndex]

    fun setMirrorIndex(index: Int) {
        val clamped = index.coerceIn(0, StrawberryConstants.STRAWBERRY_URL.lastIndex)
        Preferences.preferenceSp.edit {
            putInt(PREF_STRAWBERRY_MIRROR_INDEX, clamped)
            putString(PREF_STRAWBERRY_HOST, StrawberryConstants.STRAWBERRY_HOSTNAME[clamped])
        }
    }

    fun setHost(hostname: String) {
        val index = StrawberryConstants.STRAWBERRY_HOSTNAME.indexOfFirst {
            it.equals(hostname, ignoreCase = true)
        }
        if (index >= 0) {
            setMirrorIndex(index)
        }
    }

    fun currentHostname(): String = StrawberryConstants.STRAWBERRY_HOSTNAME[selectedMirrorIndex]
}
