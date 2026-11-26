package com.yenaly.han1meviewer.base

import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.annotation.StringRes
import com.yenaly.han1meviewer.R
import kotlin.math.roundToInt

enum class AppFontScale(val key: String, val factor: Float, @StringRes val displayNameRes: Int) {
    System("system", 1f, R.string.follow_system),
    Small("small", 0.85f, R.string.font_scale_small),
    Normal("normal", 1f, R.string.font_scale_normal),
    Large("large", 1.15f, R.string.font_scale_large),
    ExtraLarge("xlarge", 1.3f, R.string.font_scale_xlarge);

    companion object {

        const val MAX_SYSTEM_FONT_SCALE = 1.2f

        val DEFAULT = Normal

        fun fromKey(key: String?): AppFontScale = entries.firstOrNull { it.key == key } ?: DEFAULT

        fun resolve(systemFontScale: Float, pref: AppFontScale): Float {
            return if (pref == System) {
                systemFontScale.coerceAtMost(MAX_SYSTEM_FONT_SCALE)
            } else {
                pref.factor
            }
        }
    }
}

object AppDisplayDensity {

    const val MIN_LOGICAL_WIDTH_DP = 360f

    const val DEFAULT_ENABLED = true

    private const val FULLSCREEN_RATIO = 0.9f

    fun resolve(
        currentDensity: Float,
        windowWidthPx: Int,
        displayWidthPx: Int,
        enabled: Boolean,
    ): Float {
        if (!enabled || currentDensity <= 0f || windowWidthPx <= 0 || displayWidthPx <= 0) {
            return currentDensity
        }
        if (windowWidthPx < displayWidthPx * FULLSCREEN_RATIO) return currentDensity
        val logicalWidthDp = windowWidthPx / currentDensity
        if (logicalWidthDp >= MIN_LOGICAL_WIDTH_DP) return currentDensity
        return windowWidthPx / MIN_LOGICAL_WIDTH_DP
    }

    fun applyTo(
        config: Configuration,
        metrics: DisplayMetrics,
        displayWidthPx: Int,
        enabled: Boolean,
    ): Boolean {
        if (!enabled || metrics.density <= 0f) return false
        if (config.screenWidthDp == Configuration.SCREEN_WIDTH_DP_UNDEFINED) return false

        val systemDensity = metrics.density
        val targetDensity = resolve(
            currentDensity = systemDensity,
            windowWidthPx = (config.screenWidthDp * systemDensity).roundToInt(),
            displayWidthPx = displayWidthPx,
            enabled = true,
        )
        if (targetDensity == systemDensity) return false

        val scale = systemDensity / targetDensity
        config.densityDpi = (targetDensity * DisplayMetrics.DENSITY_DEFAULT).roundToInt()
        config.screenWidthDp = (config.screenWidthDp * scale).roundToInt()
        if (config.screenHeightDp != Configuration.SCREEN_HEIGHT_DP_UNDEFINED) {
            config.screenHeightDp = (config.screenHeightDp * scale).roundToInt()
        }
        if (config.smallestScreenWidthDp != Configuration.SMALLEST_SCREEN_WIDTH_DP_UNDEFINED) {
            config.smallestScreenWidthDp = (config.smallestScreenWidthDp * scale).roundToInt()
        }
        return true
    }
}

internal fun displayRealWidthPx(): Int = Resources.getSystem().displayMetrics.widthPixels
