package com.yenaly.han1meviewer.ui.navigation.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.PendingVideoOpen
import com.yenaly.han1meviewer.ui.navigation.main.DailyCheckInRoute
import com.yenaly.han1meviewer.ui.navigation.main.DailyCheckInRouteScreen
import com.yenaly.han1meviewer.ui.navigation.main.DownloadRoute
import com.yenaly.han1meviewer.ui.navigation.main.DownloadRouteScreen
import com.yenaly.han1meviewer.ui.navigation.main.VideoRoute
import com.yenaly.han1meviewer.ui.navigation.main.VideoRouteScreen
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.navigation.popBackStackSafely
import com.yenaly.han1meviewer.ui.screen.history.UnifiedHistoryScreen
import com.yenaly.han1meviewer.ui.screen.library.GifDownloadsScreen
import com.yenaly.han1meviewer.ui.screen.library.ImageDownloadsScreen
import com.yenaly.han1meviewer.ui.screen.library.LocalPlaylistsScreen
import kotlinx.serialization.Serializable

@Serializable
object UnifiedHistoryRoute : NavKey

@Serializable
object LocalPlaylistsRoute : NavKey

@Serializable
object ImageDownloadsRoute : NavKey

@Serializable
object GifDownloadsRoute : NavKey

fun NavGraphBuilder.registerGlobalSettingsRoutes(
    activity: MainActivity,
    navController: NavHostController,
    homeFallback: Any,
) {
    val openHanimeVideo: (String) -> Unit = { code ->
        activity.requestCrossSiteOpen(PendingVideoOpen(site = SiteType.HANIME, videoCode = code))
    }

    registerSettingsRoutes(
        activity = activity,
        navController = navController,
        homeFallback = homeFallback,
        onOpenVideo = openHanimeVideo,
    )
    registerUnifiedHistoryRoute(activity, navController)

    composable<DailyCheckInRoute> {
        DailyCheckInRouteScreen(
            activity = activity,
            onBack = { navController.popBackStackSafely() },
            onNavigateToVideo = openHanimeVideo,
        )
    }

    composable<GifDownloadsRoute> {
        GifDownloadsScreen(onBack = { navController.popBackStackSafely() })
    }

    composable<ImageDownloadsRoute> {
        ImageDownloadsScreen(
            onBack = { navController.popBackStackSafely() },
        )
    }

    composable<DownloadRoute> {
        DownloadRouteScreen(
            onBack = { navController.popBackStackSafely() },
            onNavigateToVideo = openHanimeVideo,
            onNavigateToLocalVideo = { code, uri ->
                navController.navigateSafely(VideoRoute(code, uri))
            },
        )
    }

    composable<VideoRoute> {
        VideoRouteScreen(
            activity = activity,
            route = it.toRoute(),
        )
    }
}

fun NavGraphBuilder.registerUnifiedHistoryRoute(
    activity: MainActivity,
    navController: NavHostController,
) {
    composable<UnifiedHistoryRoute> {
        UnifiedHistoryScreen(
            activity = activity,
            onBack = { navController.popBackStackSafely() },
        )
    }
    composable<LocalPlaylistsRoute> {
        LocalPlaylistsScreen(
            activity = activity,
            onBack = { navController.popBackStackSafely() },
        )
    }
}
