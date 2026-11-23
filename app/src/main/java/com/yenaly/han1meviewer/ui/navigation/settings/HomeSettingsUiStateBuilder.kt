package com.yenaly.han1meviewer.ui.navigation.settings

import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.yenaly.han1meviewer.BuildConfig
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.shizuku.UpdateInstaller
import com.yenaly.han1meviewer.ui.screen.home.homepage.defaultHomeCategoryPreferenceItems
import com.yenaly.han1meviewer.ui.screen.home.homepage.hiddenHomeCategoryKeys
import com.yenaly.han1meviewer.ui.screen.home.homepage.homeCategoryOrder
import com.yenaly.han1meviewer.ui.screen.settings.model.HomeSettingsUiState
import com.yenaly.han1meviewer.ui.theme.ThemeColorPreset

internal fun buildHomeSettingsUiState(
    context: Context,
    launcherItems: List<LauncherItem>,
    updateSummary: String,
    cacheSummary: String,
): HomeSettingsUiState {
    val currentAlias = Preferences.fakeLauncherIcon
    val currentItem = launcherItems.find { it.alias == currentAlias } ?: launcherItems.first()
    val videoLanguageLabel = when (Preferences.videoLanguage) {
        "zht" -> context.getString(R.string.traditional_chinese)
        "zhs" -> context.getString(R.string.simplified_chinese)
        else -> Preferences.videoLanguage
    }
    val darkModeLabel = when (Preferences.useDarkMode) {
        "follow_system" -> context.getString(R.string.follow_system)
        "always_off" -> context.getString(R.string.always_off)
        "always_on" -> context.getString(R.string.always_on)
        else -> Preferences.useDarkMode
    }
    val updateInstallerLabel = when (Preferences.updateInstaller) {
        UpdateInstaller.SHIZUKU -> context.getString(R.string.update_installer_shizuku)
        else -> context.getString(R.string.update_installer_system)
    }
    val appLanguageValue =
        Preferences.preferenceSp.getString(SettingsPreferenceKeys.APP_LANGUAGE, "system") ?: "system"
    val appLanguageLabel = when (appLanguageValue) {
        "system" -> context.getString(R.string.follow_system)
        "zh-rCN" -> context.getString(R.string.simplified_chinese)
        "zh" -> context.getString(R.string.traditional_chinese)
        "ja" -> context.getString(R.string.japanese_lang)
        "en" -> context.getString(R.string.english_lang)
        else -> appLanguageValue
    }
    val searchGridColumnsConfig = Preferences.searchGridColumnsConfig
    val videoGridColumnsConfig = Preferences.videoGridColumnsConfig
    val horizontalCardCountConfig = Preferences.horizontalCardCountConfig
    return HomeSettingsUiState(
        videoLanguage = Preferences.videoLanguage,
        videoLanguageLabel = videoLanguageLabel,
        defaultVideoQuality = Preferences.videoQuality,
        darkMode = Preferences.useDarkMode,
        darkModeLabel = darkModeLabel,
        appLanguage = appLanguageValue,
        appLanguageLabel = appLanguageLabel,
        allowPipMode = Preferences.preferenceSp.getBoolean(SettingsPreferenceKeys.ALLOW_PIP_MODE, false),
        allowResumePlayback = Preferences.allowResumePlayback,
        showPlayedIndicator = Preferences.showPlayedIndicator,
        watchedProgressThreshold = Preferences.watchedProgressThreshold,
        searchPagination = Preferences.searchPagination,
        relatedVideoAsTab = Preferences.relatedVideoAsTab,
        searchArtistIgnoreVideoType = Preferences.searchArtistIgnoreVideoType,
        disableMobileDataWarning = Preferences.disableMobileDataWarning,
        showExitConfirm = Preferences.showExitConfirm,
        disablePredictiveBack = Preferences.disablePredictiveBack,
        disableComments = Preferences.preferenceSp.getBoolean(SettingsPreferenceKeys.DISABLE_COMMENTS, false),
        useCIUpdateChannel = Preferences.useCIUpdateChannel,
        updateInstaller = Preferences.updateInstaller,
        updateInstallerLabel = updateInstallerLabel,
        useAnalytics = Preferences.isAnalyticsEnabled,
        useLockScreen = Preferences.preferenceSp.getBoolean(SettingsPreferenceKeys.USE_LOCK_SCREEN, false),
        fakeLauncherIconName = currentItem.name,
        updateSummary = updateSummary,
        cacheSummary = cacheSummary,
        versionSummary = context.getString(
            R.string.current_version,
            "v${BuildConfig.VERSION_NAME}"
        ),
        updatePopupIntervalSummary = toIntervalDaysPrettyString(
            context,
            Preferences.updatePopupIntervalDays
        ),
        updatePopupIntervalDays = Preferences.updatePopupIntervalDays,
        dynamicColorEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        themeColorKey = Preferences.themeColor ?: ThemeColorPreset.DEFAULT.key,
        themeColorName = context.getString(ThemeColorPreset.fromKey(Preferences.themeColor).displayNameRes),
        pureBlackDarkMode = Preferences.pureBlackDarkMode,
        searchGridColumnsSummary = listOf(
            searchGridColumnsConfig.compactColumns,
            searchGridColumnsConfig.mediumColumns,
            searchGridColumnsConfig.expandedColumns,
            searchGridColumnsConfig.largeColumns,
        ).joinToString(" / "),
        searchGridColumnsConfig = searchGridColumnsConfig,
        videoGridColumnsSummary = listOf(
            videoGridColumnsConfig.compactColumns,
            videoGridColumnsConfig.mediumColumns,
            videoGridColumnsConfig.expandedColumns,
            videoGridColumnsConfig.largeColumns,
        ).joinToString(" / "),
        videoGridColumnsConfig = videoGridColumnsConfig,
        horizontalCardCountSummary = listOf(
            horizontalCardCountConfig.narrowCount,
            horizontalCardCountConfig.compactCount,
            horizontalCardCountConfig.mediumCount,
            horizontalCardCountConfig.expandedCount,
        ).joinToString(" / "),
        horizontalCardCountConfig = horizontalCardCountConfig,
        homeCategoryItems = defaultHomeCategoryPreferenceItems,
        homeCategoryOrder = homeCategoryOrder,
        hiddenHomeCategoryKeys = hiddenHomeCategoryKeys,
        useAvHomeCategoryTitles = Preferences.isAvSite,
    )
}
