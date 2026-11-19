package com.yenaly.han1meviewer.ui.navigation

import com.yenaly.han1meviewer.Preferences

/**
 * Whether the predictive-back preview animation is disabled.
 *
 * Kept as an extension in this package so [SanitizedPredictiveBackOwner] compiles without
 * touching Preferences.kt. Defaults to false (predictive back stays on). If a real
 * `disablePredictiveBack` member is later added to Preferences, the member wins automatically.
 */
val Preferences.disablePredictiveBack: Boolean
    get() = preferenceSp.getBoolean("disablePredictiveBack", false)
