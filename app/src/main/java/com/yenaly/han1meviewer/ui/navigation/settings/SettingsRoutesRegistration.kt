package com.yenaly.han1meviewer.ui.navigation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.main.navigateSettingsCategoryDestination
import com.yenaly.han1meviewer.ui.navigation.main.navigateSettingsDestination
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.navigation.popBackStackSafely
import com.yenaly.han1meviewer.ui.screen.settings.AboutAppScreen
import com.yenaly.han1meviewer.ui.screen.settings.MediaControlsSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.SitePlayerSettingsPage
import com.yenaly.han1meviewer.ui.screen.settings.TranslationCacheScreen
import com.yenaly.han1meviewer.ui.screen.settings.TranslationSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.UpdateStatusScreen

/**
 * Single registration point for every settings destination. Used by the Hanime-family
 * [com.yenaly.han1meviewer.ui.navigation.main.MainNavHost] and by every site NavHost so the
 * settings tree behaves identically no matter which site is active.
 *
 * @param homeFallback where the settings home goes when there is nothing to pop back to.
 * @param onOpenVideo opens a Hanime video (used by the H-Keyframe lists).
 */
fun NavGraphBuilder.registerSettingsRoutes(
    activity: MainActivity,
    navController: NavHostController,
    homeFallback: Any,
    onOpenVideo: (String) -> Unit,
) {
    composable<HomeSettingsRoute> {
        SettingsScaffold(navController = navController, fallbackDestination = homeFallback) {
            SettingsHomeRouteScreen(
                onOpenCategory = { navController.navigateSettingsCategoryDestination(it) },
            )
        }
    }
    composable<SettingsCategoryRoute> { entry ->
        val category = SettingsCategory.fromKey(entry.toRoute<SettingsCategoryRoute>().categoryKey)
            ?: SettingsCategory.Appearance
        SettingsScaffold(navController = navController, fallbackDestination = HomeSettingsRoute) {
            SettingsCategoryRouteScreen(
                category = category,
                activity = activity,
                onNavigateToPlayerSettings = {
                    navController.navigateSettingsDestination(PlayerSettingsRoute)
                },
                onNavigateToHKeyframes = {
                    navController.navigateSettingsDestination(HKeyframesRoute)
                },
                onNavigateToSharedHKeyframes = {
                    navController.navigateSettingsDestination(SharedHKeyframesRoute)
                },
                onNavigateToAboutPage = {
                    navController.navigateSettingsDestination(AboutAppRoute)
                },
                onNavigateToUnifiedHistory = { navController.navigateSafely(UnifiedHistoryRoute) },
                onNavigateToTranslationSettings = {
                    navController.navigateSettingsDestination(TranslationSettingsRoute)
                },
            )
        }
    }
    composable<AboutAppRoute> {
        SettingsScaffold(navController = navController, fallbackDestination = HomeSettingsRoute) {
            AboutAppScreen()
        }
    }
    composable<UpdateSettingsRoute> {
        SettingsScaffold(navController = navController, fallbackDestination = HomeSettingsRoute) {
            UpdateStatusScreen(activity = activity)
        }
    }
    composable<PlayerSettingsRoute> {
        SettingsScaffold(navController = navController, fallbackDestination = HomeSettingsRoute) {
            PlayerSettingsRouteScreen(
                onNavigateToMpvSettings = {
                    navController.navigateSettingsDestination(MpvPlayerSettingsRoute)
                },
                onNavigateToMediaControls = {
                    navController.navigateSettingsDestination(MediaControlsSettingsRoute)
                },
            )
        }
    }
    composable<SitePlayerSettingsRoute> { entry ->
        val site = runCatching { SiteType.valueOf(entry.toRoute<SitePlayerSettingsRoute>().site) }
            .getOrDefault(SiteType.SUPJAV)
        SitePlayerSettingsPage(site = site, onBack = { navController.popBackStackSafely() })
    }
    composable<MediaControlsSettingsRoute> {
        MediaControlsSettingsScreen(onBack = { navController.popBackStackSafely() })
    }
    composable<TranslationSettingsRoute> {
        TranslationSettingsScreen(
            onBack = { navController.popBackStackSafely() },
            onOpenCache = { navController.navigateSafely(TranslationCacheRoute) },
        )
    }
    composable<TranslationCacheRoute> {
        TranslationCacheScreen(onBack = { navController.popBackStackSafely() })
    }
    composable<NetworkSettingsRoute> {
        SettingsScaffold(navController = navController, fallbackDestination = HomeSettingsRoute) {
            NetworkSettingsRouteScreen()
        }
    }
    composable<DownloadSettingsRoute> {
        SettingsScaffold(navController = navController, fallbackDestination = HomeSettingsRoute) {
            DownloadSettingsRouteScreen(activity = activity)
        }
    }
    composable<MpvPlayerSettingsRoute> {
        SettingsScaffold(navController = navController, fallbackDestination = PlayerSettingsRoute) {
            MpvPlayerSettingsRouteScreen()
        }
    }
    composable<HKeyframesRoute> {
        var showImportDialog by remember { mutableStateOf(false) }
        SettingsScaffold(
            navController = navController,
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
    composable<SharedHKeyframesRoute> {
        SettingsScaffold(
            navController = navController,
            fallbackDestination = HKeyframeSettingsRoute,
        ) {
            SharedHKeyframesRouteScreen(onOpenVideo = onOpenVideo)
        }
    }
    composable<HKeyframeSettingsRoute> {
        SettingsScaffold(navController = navController, fallbackDestination = HomeSettingsRoute) {
            HKeyframeSettingsRouteScreen(
                onNavigateToHKeyframes = {
                    navController.navigateSettingsDestination(HKeyframesRoute)
                },
                onNavigateToSharedHKeyframes = {
                    navController.navigateSettingsDestination(SharedHKeyframesRoute)
                },
            )
        }
    }
}
