package com.yenaly.han1meviewer.base

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.preference.PreferenceManager
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsPreferenceKeys
import java.util.Locale

object AppLocale {

    val preferredLanguage: Locale
        get() = LocaleList.getDefault()[0]

    fun wrap(context: Context): Context {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val lang = prefs.getString("app_language", "system") ?: "system"

        val newLocale = when (lang) {
            "zh-rCN" -> Locale.SIMPLIFIED_CHINESE
            "zh" -> Locale.TRADITIONAL_CHINESE
            "en" -> Locale.ENGLISH
            "ja" -> Locale.JAPANESE
            else -> Resources.getSystem().configuration.locales[0]
        }

        Locale.setDefault(newLocale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(newLocale)

        val fontScalePref = AppFontScale.fromKey(
            prefs.getString(SettingsPreferenceKeys.APP_FONT_SCALE, AppFontScale.DEFAULT.key)
        )
        config.fontScale = AppFontScale.resolve(config.fontScale, fontScalePref)

        AppDisplayDensity.applyTo(
            config = config,
            metrics = context.resources.displayMetrics,
            displayWidthPx = displayRealWidthPx(),
            enabled = prefs.getBoolean(
                SettingsPreferenceKeys.IGNORE_SYSTEM_DISPLAY_SIZE,
                AppDisplayDensity.DEFAULT_ENABLED,
            ),
        )

        return context.createConfigurationContext(config)
    }
}
