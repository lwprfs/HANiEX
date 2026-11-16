package com.yenaly.han1meviewer.ui.screen.settings

import androidx.compose.foundation.layout.padding
import com.yenaly.han1meviewer.ui.component.lazy.AnimatedLazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.DefaultSiteSwitchOptions
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SettingSectionHeader
import com.yenaly.han1meviewer.ui.component.SettingSliderItem
import com.yenaly.han1meviewer.ui.component.SettingSwitchItem
import com.yenaly.han1meviewer.ui.screen.settings.model.HomeSettingsUiState

internal enum class HomeSettingsChoiceDialog {
    VideoLanguage,
    VideoQuality,
    DarkMode,
    AppLanguage,
    ThemeColor,
    UpdateInstaller,
}

internal fun AnimatedLazyListScope.appearanceSettings(
    state: HomeSettingsUiState,
    onEditHomeCategories: () -> Unit,
    onEditHorizontalCards: () -> Unit,
    onEditSearchGrid: () -> Unit,
    onEditVideoGrid: () -> Unit,
    onOpenDialog: (HomeSettingsChoiceDialog) -> Unit,
    onOpenTranslationSettings: () -> Unit,
    onPureBlackDarkModeChange: (Boolean) -> Unit,
    onRelatedVideoAsTabChange: (Boolean) -> Unit,
    onSearchArtistIgnoreVideoTypeChange: (Boolean) -> Unit,
    onSearchPaginationChange: (Boolean) -> Unit,
    onShowExitConfirmChange: (Boolean) -> Unit,
) {
        item { SettingsGroupTitle(stringResource(R.string.theme)) }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.dark_theme),
                valueText = state.darkModeLabel,
                iconRes = R.drawable.ic_baseline_moon_24,
                onClick = { onOpenDialog(HomeSettingsChoiceDialog.DarkMode) },
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.theme_color),
                valueText = state.themeColorName,
                iconRes = R.drawable.ic_baseline_theme_24,
                onClick = { onOpenDialog(HomeSettingsChoiceDialog.ThemeColor) },
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.pure_black_dark_mode),
                summary = stringResource(R.string.pure_black_dark_mode_summary),
                checked = state.pureBlackDarkMode,
                iconRes = R.drawable.ic_baseline_contrast_24,
                onCheckedChange = onPureBlackDarkModeChange,
            )
        }

        item { SettingsGroupTitle(stringResource(R.string.settings_group_interface)) }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.home_category_layout),
                summary = stringResource(
                    R.string.home_category_layout_summary,
                    state.homeCategoryItems.size - state.hiddenHomeCategoryKeys.size,
                    state.homeCategoryItems.size,
                ),
                iconRes = R.drawable.baseline_sort_24,
                onClick = { onEditHomeCategories() },
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.site_video_grid_columns),
                summary = stringResource(R.string.hub_video_grid_columns_desc),
                valueText = state.videoGridColumnsSummary,
                iconRes = R.drawable.baseline_grid_24,
                onClick = { onEditVideoGrid() },
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.search_grid_columns_title),
                summary = stringResource(R.string.search_grid_columns_summary),
                valueText = state.searchGridColumnsSummary,
                iconRes = R.drawable.baseline_grid_24,
                onClick = { onEditSearchGrid() },
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.horizontal_card_count_title),
                summary = stringResource(R.string.horizontal_card_count_summary),
                valueText = state.horizontalCardCountSummary,
                iconRes = R.drawable.baseline_row_24,
                onClick = { onEditHorizontalCards() },
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.search_pagination),
                summary = stringResource(R.string.search_pagination_summary),
                checked = state.searchPagination,
                iconRes = R.drawable.baseline_paging_24,
                onCheckedChange = onSearchPaginationChange,
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.search_artist_ignore_video_type),
                summary = stringResource(R.string.search_artist_ignore_video_type_summary),
                checked = state.searchArtistIgnoreVideoType,
                iconRes = R.drawable.baseline_prohibit_24,
                onCheckedChange = onSearchArtistIgnoreVideoTypeChange,
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.related_video_as_tab),
                summary = stringResource(R.string.related_video_as_tab_summary),
                checked = state.relatedVideoAsTab,
                iconRes = R.drawable.ic_baseline_list_24,
                onCheckedChange = onRelatedVideoAsTabChange,
            )
        }

        item { SettingsGroupTitle(stringResource(R.string.settings_group_app)) }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.app_lang),
                summary = stringResource(R.string.app_lang_sum),
                valueText = state.appLanguageLabel,
                iconRes = R.drawable.ic_setting_lang,
                onClick = { onOpenDialog(HomeSettingsChoiceDialog.AppLanguage) },
            )
        }
        if (com.yenaly.han1meviewer.util.translation.AppTranslation.available) {
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.rouvideo_tr_title),
                    summary = stringResource(R.string.rouvideo_tr_privacy),
                    iconRes = R.drawable.ic_translate_24,
                    onClick = onOpenTranslationSettings,
                )
            }
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.show_exit_confirm),
                summary = stringResource(R.string.show_exit_confirm_summary),
                checked = state.showExitConfirm,
                iconRes = R.drawable.ic_baseline_close_24,
                onCheckedChange = onShowExitConfirmChange,
            )
        }

        item { SiteVisibilitySettings() }
        val siteEntries = SiteSettingsRegistry.activeEntries
        if (siteEntries.isNotEmpty()) {
            item { SettingsGroupTitle(stringResource(R.string.current_site_settings)) }
            siteEntries.forEach { entry ->
                item(key = "site_${entry.key}") {
                    SettingNavigationItem(
                        title = entry.title,
                        summary = entry.subtitle,
                        iconRes = R.drawable.ic_settings_list_24,
                        onClick = entry.onClick,
                    )
                }
            }
        }
}

internal fun AnimatedLazyListScope.playbackSettings(
    state: HomeSettingsUiState,
    onAllowPipModeChange: (Boolean) -> Unit,
    onAllowResumePlaybackChange: (Boolean) -> Unit,
    onDisableCommentsChange: (Boolean) -> Unit,
    onDisableMobileDataWarningChange: (Boolean) -> Unit,
    onOpenDialog: (HomeSettingsChoiceDialog) -> Unit,
    onOpenPlayerSettings: () -> Unit,
    onShowPlayedIndicatorChange: (Boolean) -> Unit,
    onWatchedProgressThresholdChange: (Int) -> Unit,
) {
        item { SettingsGroupTitle(stringResource(R.string.video)) }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.video_language),
                valueText = state.videoLanguageLabel,
                iconRes = R.drawable.baseline_simp_to_trad_24,
                onClick = { onOpenDialog(HomeSettingsChoiceDialog.VideoLanguage) },
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.default_video_quilty),
                valueText = state.defaultVideoQuality,
                iconRes = R.drawable.ic_video_quilty,
                onClick = { onOpenDialog(HomeSettingsChoiceDialog.VideoQuality) },
            )
        }
        item { SettingsGroupTitle(stringResource(R.string.settings_group_playback_behavior)) }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.allow_pip_title),
                summary = stringResource(R.string.allow_pip_disc),
                checked = state.allowPipMode,
                iconRes = R.drawable.ic_pip_mode,
                onCheckedChange = onAllowPipModeChange,
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.resume_playback_title),
                summary = stringResource(R.string.resume_playback_summary),
                checked = state.allowResumePlayback,
                iconRes = R.drawable.ic_baseline_skip_24,
                onCheckedChange = onAllowResumePlaybackChange,
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.show_played_indicator),
                summary = stringResource(R.string.show_played_indicator_summary),
                checked = state.showPlayedIndicator,
                iconRes = R.drawable.ic_baseline_history_24,
                onCheckedChange = onShowPlayedIndicatorChange,
            )
        }
        item {
            SettingSliderItem(
                title = stringResource(R.string.watched_progress_threshold),
                summary = stringResource(
                    R.string.watched_progress_threshold_summary,
                    state.watchedProgressThreshold,
                ),
                value = state.watchedProgressThreshold,
                valueLabel = "${state.watchedProgressThreshold}%",
                valueRange = 0..100,
                step = 10,
                iconRes = R.drawable.baseline_flag_24,
                onValueChange = onWatchedProgressThresholdChange,
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.disable_mobile_data_warning),
                summary = stringResource(R.string.disable_mobile_data_warning_summary),
                checked = state.disableMobileDataWarning,
                iconRes = R.drawable.baseline_mobile_data_24,
                onCheckedChange = onDisableMobileDataWarningChange,
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.disable_comments_title),
                summary = stringResource(R.string.disable_comments_sum),
                checked = state.disableComments,
                iconRes = R.drawable.ic_comments,
                onCheckedChange = onDisableCommentsChange,
            )
        }
        item { SettingsGroupTitle(stringResource(R.string.settings_group_player)) }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.player_settings),
                iconRes = R.drawable.ic_baseline_play_circle_outline_24,
                onClick = onOpenPlayerSettings,
            )
        }
}

internal fun AnimatedLazyListScope.privacySettings(
    state: HomeSettingsUiState,
    onOpenFakeLauncherIcon: () -> Unit,
    onUseAnalyticsChange: (Boolean) -> Unit,
    onUseLockScreenChange: (Boolean) -> Unit,
) {
        item { SettingsGroupTitle(stringResource(R.string.privacy)) }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.analytics_title),
                summary = stringResource(R.string.analytics_summary),
                checked = state.useAnalytics,
                iconRes = R.drawable.baseline_data_usage_24,
                onCheckedChange = onUseAnalyticsChange,
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.use_lock_screen),
                summary = stringResource(R.string.use_lock_screen_sum),
                checked = state.useLockScreen,
                iconRes = R.drawable.ic_setting_applock,
                onCheckedChange = onUseLockScreenChange,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.fake_app_icon),
                summary = stringResource(R.string.select_fake_icon),
                valueText = state.fakeLauncherIconName,
                iconRes = R.drawable.ic_baseline_mask,
                onClick = onOpenFakeLauncherIcon,
            )
        }
}

internal fun AnimatedLazyListScope.dataSettings(
    state: HomeSettingsUiState,
    onClearCache: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onOpenUnifiedHistory: () -> Unit,
) {
        item { SettingsGroupTitle(stringResource(R.string.settings_group_site_history)) }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.hist_all_sites_title),
                summary = stringResource(
                    R.string.hist_all_sites_summary,
                    DefaultSiteSwitchOptions.joinToString(", ") { it.name },
                ),
                iconRes = R.drawable.ic_history_all_sites,
                onClick = onOpenUnifiedHistory,
            )
        }

        item { SettingsGroupTitle(stringResource(R.string.settings_group_backup)) }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.backup_export_title),
                summary = stringResource(R.string.backup_export_summary),
                iconRes = R.drawable.baseline_backup_24,
                onClick = onExportBackup,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.backup_import_title),
                summary = stringResource(R.string.backup_import_summary),
                iconRes = R.drawable.baseline_restore_24,
                onClick = onImportBackup,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.clear_cache),
                summary = state.cacheSummary,
                iconRes = R.drawable.ic_baseline_clear_all_24,
                onClick = onClearCache,
            )
        }
}

internal fun AnimatedLazyListScope.aboutSettings(
    state: HomeSettingsUiState,
    onCheckUpdate: () -> Unit,
    onOpenAboutPage: () -> Unit,
    onOpenDialog: (HomeSettingsChoiceDialog) -> Unit,
    onOpenForum: () -> Unit,
    onOpenOpenSourceLicense: () -> Unit,
    onSubmitBug: () -> Unit,
    onUpdatePopupIntervalDaysChange: (Int) -> Unit,
    onUseCIUpdateChannelChange: (Boolean) -> Unit,
) {
        item { SettingsGroupTitle(stringResource(R.string.update)) }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.check_update),
                summary = state.updateSummary,
                iconRes = R.drawable.ic_baseline_update_24,
                onClick = onCheckUpdate,
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(R.string.use_ci_update_channel),
                checked = state.useCIUpdateChannel,
                iconRes = R.drawable.ic_setting_ci,
                onCheckedChange = onUseCIUpdateChannelChange,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.update_installer),
                summary = stringResource(R.string.update_installer_summary),
                valueText = state.updateInstallerLabel,
                iconRes = R.drawable.ic_baseline_update_24,
                onClick = { onOpenDialog(HomeSettingsChoiceDialog.UpdateInstaller) },
            )
        }
        item {
            SettingSliderItem(
                title = stringResource(R.string.update_popup_interval_days),
                summary = state.updatePopupIntervalSummary,
                value = state.updatePopupIntervalDays,
                valueRange = 0..30,
                iconRes = R.drawable.ic_clock,
                onValueChange = onUpdatePopupIntervalDaysChange,
            )
        }

        item { SettingsGroupTitle(stringResource(R.string.settings_group_feedback)) }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.submit_bug),
                summary = stringResource(R.string.submit_bug_summary),
                iconRes = R.drawable.baseline_bug_report_24,
                onClick = onSubmitBug,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.forum),
                summary = stringResource(R.string.forum_summary),
                iconRes = R.drawable.baseline_forum_24,
                onClick = onOpenForum,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.open_source_license),
                iconRes = R.drawable.ic_oss,
                onClick = onOpenOpenSourceLicense,
            )
        }

        item { SettingsGroupTitle(stringResource(R.string.settings_group_version)) }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.about),
                summary = state.versionSummary,
                iconRes = R.drawable.ic_baseline_info_24,
                onClick = onOpenAboutPage,
            )
        }
}

@Composable
private fun SettingsGroupTitle(title: String) {
    SettingSectionHeader(title)
}
