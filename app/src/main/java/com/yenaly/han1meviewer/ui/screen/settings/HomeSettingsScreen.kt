package com.yenaly.han1meviewer.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.HorizontalCardCountConfig
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SearchGridColumnsConfig
import com.yenaly.han1meviewer.base.AppFontScale
import com.yenaly.han1meviewer.ui.component.ChoiceDialog
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsCategory
import com.yenaly.han1meviewer.ui.navigation.settings.SiteSettingsSection
import com.yenaly.han1meviewer.ui.screen.settings.dialog.HomeCategoryLayoutDialog
import com.yenaly.han1meviewer.ui.screen.settings.dialog.HorizontalCardCountDialog
import com.yenaly.han1meviewer.ui.screen.settings.dialog.SearchGridColumnsDialog
import com.yenaly.han1meviewer.ui.screen.settings.dialog.ThemeColorPresetDialog
import com.yenaly.han1meviewer.ui.screen.settings.model.HomeSettingsUiState
import com.yenaly.han1meviewer.ui.theme.ThemeColorPreset

@Composable
fun HomeSettingsScreen(
    category: SettingsCategory,
    state: HomeSettingsUiState,
    onVideoLanguageChange: (String) -> Unit,
    onVideoQualityChange: (String) -> Unit,
    onDarkModeChange: (String) -> Unit,
    onAllowPipModeChange: (Boolean) -> Unit,
    onAllowResumePlaybackChange: (Boolean) -> Unit,
    onShowPlayedIndicatorChange: (Boolean) -> Unit,
    onWatchedProgressThresholdChange: (Int) -> Unit,
    onSearchPaginationChange: (Boolean) -> Unit,
    onRelatedVideoAsTabChange: (Boolean) -> Unit,
    onSearchArtistIgnoreVideoTypeChange: (Boolean) -> Unit,
    onDisableMobileDataWarningChange: (Boolean) -> Unit,
    onShowExitConfirmChange: (Boolean) -> Unit,
    onDisablePredictiveBackChange: (Boolean) -> Unit,
    onDisableCommentsChange: (Boolean) -> Unit,
    onSearchGridColumnsConfigChange: (SearchGridColumnsConfig) -> Unit,
    onVideoGridColumnsConfigChange: (SearchGridColumnsConfig) -> Unit,
    onHorizontalCardCountConfigChange: (HorizontalCardCountConfig) -> Unit,
    onUseCIUpdateChannelChange: (Boolean) -> Unit,
    onUpdateInstallerChange: (String) -> Unit,
    onUseAnalyticsChange: (Boolean) -> Unit,
    onUseLockScreenChange: (Boolean) -> Unit,
    onThemeColorChange: (String) -> Unit,
    onPureBlackDarkModeChange: (Boolean) -> Unit,
    onHomeCategoryPreferencesChange: (List<String>, Set<String>) -> Unit,
    onOpenPlayerSettings: () -> Unit,
    onOpenUnifiedHistory: () -> Unit = {},
    onAppLanguageChange: (String) -> Unit,
    onAppFontScaleChange: (String) -> Unit,
    onIgnoreSystemDisplaySizeChange: (Boolean) -> Unit,
    onCheckUpdate: () -> Unit,
    onUpdatePopupIntervalDaysChange: (Int) -> Unit,
    onOpenFakeLauncherIcon: () -> Unit,
    onOpenOpenSourceLicense: () -> Unit,
    onOpenAboutPage: () -> Unit,
    onClearCache: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onSubmitBug: () -> Unit,
    onOpenForum: () -> Unit,
    hanimeSection: SiteSettingsSection? = null,
) {
    var activeDialog by rememberSaveable { mutableStateOf<HomeSettingsChoiceDialog?>(null) }
    var showSearchGridColumnsDialog by rememberSaveable { mutableStateOf(false) }
    var showVideoGridColumnsDialog by rememberSaveable { mutableStateOf(false) }
    var showHorizontalCardCountDialog by rememberSaveable { mutableStateOf(false) }
    var showHomeCategoryDialog by rememberSaveable { mutableStateOf(false) }

    ChoiceDialog(
        visible = activeDialog == HomeSettingsChoiceDialog.VideoLanguage,
        title = stringResource(R.string.video_language),
        options = listOf(
            stringResource(R.string.traditional_chinese) to "zht",
            stringResource(R.string.simplified_chinese) to "zhs",
        ),
        selectedValue = state.videoLanguage,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onVideoLanguageChange(it)
        },
    )

    ChoiceDialog(
        visible = activeDialog == HomeSettingsChoiceDialog.VideoQuality,
        title = stringResource(R.string.default_video_quilty),
        options = listOf("480P" to "480P", "720P" to "720P", "1080P" to "1080P"),
        selectedValue = state.defaultVideoQuality,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onVideoQualityChange(it)
        },
    )

    ChoiceDialog(
        visible = activeDialog == HomeSettingsChoiceDialog.UpdateInstaller,
        title = stringResource(R.string.update_installer),
        options = listOf(
            stringResource(R.string.update_installer_system) to "system",
            stringResource(R.string.update_installer_shizuku) to "shizuku",
        ),
        selectedValue = state.updateInstaller,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onUpdateInstallerChange(it)
        },
    )

    ChoiceDialog(
        visible = activeDialog == HomeSettingsChoiceDialog.DarkMode,
        title = stringResource(R.string.dark_theme),
        options = listOf(
            stringResource(R.string.follow_system) to "follow_system",
            stringResource(R.string.always_off) to "always_off",
            stringResource(R.string.always_on) to "always_on",
        ),
        selectedValue = state.darkMode,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onDarkModeChange(it)
        },
    )

    ChoiceDialog(
        visible = activeDialog == HomeSettingsChoiceDialog.AppLanguage,
        title = stringResource(R.string.app_lang),
        options = listOf(
            stringResource(R.string.follow_system) to "system",
            stringResource(R.string.simplified_chinese) to "zh-rCN",
            stringResource(R.string.traditional_chinese) to "zh",
            stringResource(R.string.japanese_lang) to "ja",
            stringResource(R.string.english_lang) to "en",
        ),
        selectedValue = state.appLanguage,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onAppLanguageChange(it)
        },
    )

    ChoiceDialog(
        visible = activeDialog == HomeSettingsChoiceDialog.AppFontScale,
        title = stringResource(R.string.app_font_scale),
        options = AppFontScale.entries.map { scale ->
            stringResource(scale.displayNameRes) to scale.key
        },
        selectedValue = state.appFontScaleKey,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onAppFontScaleChange(it)
        },
    )

    ThemeColorPresetDialog(
        visible = activeDialog == HomeSettingsChoiceDialog.ThemeColor,
        presets = ThemeColorPreset.entries
            .filter { state.dynamicColorEnabled || it != ThemeColorPreset.SYSTEM },
        selectedKey = state.themeColorKey,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onThemeColorChange(it)
        },
    )

    if (showVideoGridColumnsDialog) {
        SearchGridColumnsDialog(
            initialConfig = state.videoGridColumnsConfig,
            title = stringResource(R.string.site_video_grid_columns),
            onDismiss = { showVideoGridColumnsDialog = false },
            onConfirm = {
                showVideoGridColumnsDialog = false
                onVideoGridColumnsConfigChange(it)
            },
        )
    }

    if (showSearchGridColumnsDialog) {
        SearchGridColumnsDialog(
            initialConfig = state.searchGridColumnsConfig,
            onDismiss = { showSearchGridColumnsDialog = false },
            onConfirm = {
                showSearchGridColumnsDialog = false
                onSearchGridColumnsConfigChange(it)
            },
        )
    }

    if (showHorizontalCardCountDialog) {
        HorizontalCardCountDialog(
            initialConfig = state.horizontalCardCountConfig,
            onDismiss = { showHorizontalCardCountDialog = false },
            onConfirm = {
                showHorizontalCardCountDialog = false
                onHorizontalCardCountConfigChange(it)
            },
        )
    }

    if (showHomeCategoryDialog) {
        HomeCategoryLayoutDialog(
            state = state,
            onDismiss = { showHomeCategoryDialog = false },
            onConfirm = { order, hiddenKeys ->
                showHomeCategoryDialog = false
                onHomeCategoryPreferencesChange(order, hiddenKeys)
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (hanimeSection == SiteSettingsSection.LayoutCards) {
            hanimeLayoutSettings(
                state = state,
                onEditHomeCategories = { showHomeCategoryDialog = true },
            )
        } else if (hanimeSection != null) {
            hanimeSiteSettings(
                state = state,
                onDisableCommentsChange = onDisableCommentsChange,
                onOpenDialog = { activeDialog = it },
                onRelatedVideoAsTabChange = onRelatedVideoAsTabChange,
                onSearchArtistIgnoreVideoTypeChange = onSearchArtistIgnoreVideoTypeChange,
                onSearchPaginationChange = onSearchPaginationChange,
                onWatchedProgressThresholdChange = onWatchedProgressThresholdChange,
            )
        } else when (category) {
            SettingsCategory.Appearance -> {
                appearanceSettings(
                    state = state,
                    onEditHorizontalCards = { showHorizontalCardCountDialog = true },
                    onEditSearchGrid = { showSearchGridColumnsDialog = true },
                    onEditVideoGrid = { showVideoGridColumnsDialog = true },
                    onOpenDialog = { activeDialog = it },
                    onPureBlackDarkModeChange = onPureBlackDarkModeChange,
                    onShowExitConfirmChange = onShowExitConfirmChange,
                    onDisablePredictiveBackChange = onDisablePredictiveBackChange,
                    onIgnoreSystemDisplaySizeChange = onIgnoreSystemDisplaySizeChange,
                )
            }

            SettingsCategory.Playback -> {
                playbackSettings(
                    state = state,
                    onAllowPipModeChange = onAllowPipModeChange,
                    onAllowResumePlaybackChange = onAllowResumePlaybackChange,
                    onDisableMobileDataWarningChange = onDisableMobileDataWarningChange,
                    onOpenPlayerSettings = onOpenPlayerSettings,
                    onShowPlayedIndicatorChange = onShowPlayedIndicatorChange,
                )
            }

            SettingsCategory.Network,
            SettingsCategory.Download,
            SettingsCategory.Keyframes -> Unit

            SettingsCategory.Privacy -> {
                privacySettings(
                    state = state,
                    onOpenFakeLauncherIcon = onOpenFakeLauncherIcon,
                    onUseAnalyticsChange = onUseAnalyticsChange,
                    onUseLockScreenChange = onUseLockScreenChange,
                )
            }

            SettingsCategory.Data -> {
                dataSettings(
                    state = state,
                    onClearCache = onClearCache,
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup,
                    onOpenUnifiedHistory = onOpenUnifiedHistory,
                )
            }

            SettingsCategory.About -> {
                aboutSettings(
                    state = state,
                    onCheckUpdate = onCheckUpdate,
                    onOpenAboutPage = onOpenAboutPage,
                    onOpenDialog = { activeDialog = it },
                    onOpenForum = onOpenForum,
                    onOpenOpenSourceLicense = onOpenOpenSourceLicense,
                    onSubmitBug = onSubmitBug,
                    onUpdatePopupIntervalDaysChange = onUpdatePopupIntervalDaysChange,
                    onUseCIUpdateChannelChange = onUseCIUpdateChannelChange,
                )
            }
        }
    }
}
