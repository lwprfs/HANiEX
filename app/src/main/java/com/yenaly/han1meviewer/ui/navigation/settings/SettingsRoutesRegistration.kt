package com.yenaly.han1meviewer.ui.navigation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.Navigator
import com.yenaly.han1meviewer.ui.navigation.main.navigateSettingsCategoryDestination
import com.yenaly.han1meviewer.ui.navigation.main.navigateSettingsDestination
import com.yenaly.han1meviewer.ui.navigation.screen
import com.yenaly.han1meviewer.ui.screen.settings.AboutAppScreen
import com.yenaly.han1meviewer.ui.screen.settings.MediaControlsSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.SitePlayerSettingsPage
import com.yenaly.han1meviewer.ui.screen.settings.SiteSocialStylePage
import com.yenaly.han1meviewer.ui.screen.settings.TranslationCacheScreen
import com.yenaly.han1meviewer.ui.screen.settings.TranslationSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.UpdateStatusScreen

fun EntryProviderScope<NavKey>.settingsEntries(
    activity: MainActivity,
    navigator: Navigator,
    homeFallback: NavKey,
    onOpenVideo: (String) -> Unit,
) {
    val onBack: () -> Unit = { navigator.goBack() }

    screen<HomeSettingsRoute> {
        SettingsScaffold(navigator = navigator, fallbackDestination = homeFallback) {
            SettingsHomeRouteScreen(
                onOpenCategory = { navigator.navigateSettingsCategoryDestination(it) },
            )
        }
    }
    screen<SettingsCategoryRoute> { key ->
        val category = SettingsCategory.fromKey(key.categoryKey)
            ?: SettingsCategory.Appearance
        SettingsScaffold(navigator = navigator, fallbackDestination = HomeSettingsRoute) {
            SettingsCategoryRouteScreen(
                category = category,
                activity = activity,
                onNavigateToPlayerSettings = {
                    navigator.navigateSettingsDestination(PlayerSettingsRoute)
                },
                onNavigateToHKeyframes = {
                    navigator.navigateSettingsDestination(HKeyframesRoute)
                },
                onNavigateToSharedHKeyframes = {
                    navigator.navigateSettingsDestination(SharedHKeyframesRoute)
                },
                onNavigateToAboutPage = {
                    navigator.navigateSettingsDestination(AboutAppRoute)
                },
                onNavigateToUnifiedHistory = { navigator.navigateSafely(UnifiedHistoryRoute) },
                onNavigateToTranslationSettings = {
                    navigator.navigateSettingsDestination(TranslationSettingsRoute)
                },
            )
        }
    }
    screen<AboutAppRoute> {
        SettingsScaffold(navigator = navigator, fallbackDestination = HomeSettingsRoute) {
            AboutAppScreen()
        }
    }
    screen<UpdateSettingsRoute> {
        SettingsScaffold(navigator = navigator, fallbackDestination = HomeSettingsRoute) {
            UpdateStatusScreen(activity = activity)
        }
    }
    screen<PlayerSettingsRoute> {
        SettingsScaffold(navigator = navigator, fallbackDestination = HomeSettingsRoute) {
            PlayerSettingsRouteScreen(
                onNavigateToMpvSettings = {
                    navigator.navigateSettingsDestination(MpvPlayerSettingsRoute)
                },
                onNavigateToMediaControls = {
                    navigator.navigateSettingsDestination(MediaControlsSettingsRoute)
                },
            )
        }
    }
    screen<SitePlayerSettingsRoute> { key ->
        val site = runCatching { SiteType.valueOf(key.site) }
            .getOrDefault(SiteType.SUPJAV)
        SitePlayerSettingsPage(site = site, onBack = onBack)
    }
    screen<SiteSocialStyleRoute> { key ->
        val site = runCatching { SiteType.valueOf(key.site) }
            .getOrDefault(SiteType.FAPFOLDER)
        SiteSocialStylePage(site = site, onBack = onBack)
    }
    screen<MediaControlsSettingsRoute> {
        MediaControlsSettingsScreen(onBack = onBack)
    }
    screen<TranslationSettingsRoute> {
        TranslationSettingsScreen(
            onBack = onBack,
            onOpenCache = { navigator.navigateSafely(TranslationCacheRoute) },
        )
    }
    screen<TranslationCacheRoute> {
        TranslationCacheScreen(onBack = onBack)
    }
    screen<NetworkSettingsRoute> {
        SettingsScaffold(navigator = navigator, fallbackDestination = HomeSettingsRoute) {
            NetworkSettingsRouteScreen()
        }
    }
    screen<DownloadSettingsRoute> {
        SettingsScaffold(navigator = navigator, fallbackDestination = HomeSettingsRoute) {
            DownloadSettingsRouteScreen(activity = activity)
        }
    }
    screen<MpvPlayerSettingsRoute> {
        SettingsScaffold(navigator = navigator, fallbackDestination = PlayerSettingsRoute) {
            MpvPlayerSettingsRouteScreen()
        }
    }
    screen<HKeyframesRoute> {
        var showImportDialog by remember { mutableStateOf(false) }
        SettingsScaffold(
            navigator = navigator,
            fallbackDestination = HKeyframeSettingsRoute,
            actions = { HKeyframesTopBarActions(onImportClick = { showImportDialog = true }) },
        ) {
            HKeyframesRouteScreen(
                onOpenVideo = onOpenVideo,
                showImportDialog = showImportDialog,
                onImportDialogDismiss = { showImportDialog = false },
            )
        }
    }
    screen<SharedHKeyframesRoute> {
        SettingsScaffold(
            navigator = navigator,
            fallbackDestination = HKeyframeSettingsRoute,
        ) {
            SharedHKeyframesRouteScreen(onOpenVideo = onOpenVideo)
        }
    }
    screen<HKeyframeSettingsRoute> {
        SettingsScaffold(navigator = navigator, fallbackDestination = HomeSettingsRoute) {
            HKeyframeSettingsRouteScreen(
                onNavigateToHKeyframes = {
                    navigator.navigateSettingsDestination(HKeyframesRoute)
                },
                onNavigateToSharedHKeyframes = {
                    navigator.navigateSettingsDestination(SharedHKeyframesRoute)
                },
            )
        }
    }
}
