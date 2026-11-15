package com.yenaly.han1meviewer.util

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.yenaly.han1meviewer.Preferences

object ThemeUtils {

    fun isDarkTheme(context: Context): Boolean = when (Preferences.useDarkMode) {
        "always_on" -> true
        "always_off" -> false
        else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }

    fun applyDarkModeFromPreferences(context: Context) {
        AppCompatDelegate.setDefaultNightMode(
            when (Preferences.useDarkMode) {
                "always_on" -> AppCompatDelegate.MODE_NIGHT_YES
                "always_off" -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}
