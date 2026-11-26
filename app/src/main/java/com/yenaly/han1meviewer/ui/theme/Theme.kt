package com.yenaly.han1meviewer.ui.theme

import android.os.Build
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Density
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.base.AppDisplayDensity
import com.yenaly.han1meviewer.base.AppFontScale
import com.yenaly.han1meviewer.base.displayRealWidthPx
import com.yenaly.han1meviewer.util.ThemeUtils

@Composable
fun HanimeTheme(
    darkTheme: Boolean = ThemeUtils.isDarkTheme(LocalContext.current),
    content: @Composable () -> Unit,
) {
    val presetColorScheme = when (val preset = ThemeColorPreset.fromKey(Preferences.themeColor)) {
        ThemeColorPreset.SYSTEM -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicColorScheme(darkTheme)
            } else {
                ThemeColorPreset.DEFAULT.colorScheme(darkTheme)
            }
        }

        else -> preset.colorScheme(darkTheme)
    }
    val colorScheme =
        if (darkTheme && Preferences.pureBlackDarkMode) presetColorScheme.toPureBlackColorScheme()
        else presetColorScheme

    val systemDensity = LocalDensity.current
    val fontScale = AppFontScale.resolve(systemDensity.fontScale, Preferences.appFontScale)
    val compatEnabled = Preferences.ignoreSystemDisplaySize && !LocalInspectionMode.current

    BoxWithConstraints {
        val resolvedDensity = if (compatEnabled) {

            val displayWidthPx = remember(LocalConfiguration.current) {
                displayRealWidthPx()
            }
            AppDisplayDensity.resolve(
                currentDensity = systemDensity.density,
                windowWidthPx = constraints.maxWidth,
                displayWidthPx = displayWidthPx,
                enabled = true,
            )
        } else {
            systemDensity.density
        }
        val appDensity = remember(resolvedDensity, fontScale) {
            Density(resolvedDensity, fontScale)
        }
        AppMaterialTheme(colorScheme = colorScheme, density = appDensity, content = content)
    }
}

@Composable
private fun AppMaterialTheme(
    colorScheme: ColorScheme,
    density: Density,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDensity provides density) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

private fun ColorScheme.toPureBlackColorScheme(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF232323),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF111111),
    surfaceContainerHigh = Color(0xFF1B1B1B),
    surfaceContainerHighest = Color(0xFF242424),
)
