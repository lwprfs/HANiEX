package com.yenaly.han1meviewer.ui.navigation

import com.yenaly.han1meviewer.Preferences

val Preferences.disablePredictiveBack: Boolean
    get() = preferenceSp.getBoolean(
        com.yenaly.han1meviewer.ui.navigation.settings.SettingsPreferenceKeys.DISABLE_PREDICTIVE_BACK,
        false,
    )
