package com.yenaly.han1meviewer.ui.navigation.settings

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.Navigator
import com.yenaly.han1meviewer.ui.navigation.PendingVideoOpen
import com.yenaly.han1meviewer.ui.navigation.main.DailyCheckInRoute
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.navigation.main.DailyCheckInRouteScreen
import com.yenaly.han1meviewer.ui.navigation.main.DownloadRoute
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.navigation.main.DownloadRouteScreen
import com.yenaly.han1meviewer.ui.navigation.main.VideoRoute
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.navigation.main.VideoRouteScreen
import com.yenaly.han1meviewer.ui.navigation.screen
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

fun EntryProviderScope<NavKey>.globalSettingsEntries(
    activity: MainActivity,
    navigator: Navigator,
    homeFallback: NavKey,
) {
    val openHanimeVideo: (String) -> Unit = { code ->
        activity.requestCrossSiteOpen(PendingVideoOpen(site = SiteType.HANIME, videoCode = code))
    }

    settingsEntries(
        activity = activity,
        navigator = navigator,
        homeFallback = homeFallback,
        onOpenVideo = openHanimeVideo,
    )
    unifiedHistoryEntries(activity, navigator)

    screen<DailyCheckInRoute> {
        DailyCheckInRouteScreen(
            activity = activity,
            onBack = { navigator.goBack() },
            onNavigateToVideo = openHanimeVideo,
        )
    }

    screen<GifDownloadsRoute> {
        GifDownloadsScreen(onBack = { navigator.goBack() })
    }

    screen<ImageDownloadsRoute> {
        ImageDownloadsScreen(
            onBack = { navigator.goBack() },
        )
    }

    screen<DownloadRoute> {
        DownloadRouteScreen(
            onBack = { navigator.goBack() },
            onNavigateToVideo = openHanimeVideo,
            onNavigateToLocalVideo = { code, uri ->
                navigator.navigateSafely(VideoRoute(code, uri))
            },
        )
    }

    screen<VideoRoute> { key ->
        VideoRouteScreen(
            activity = activity,
            route = key,
        )
    }
}

fun EntryProviderScope<NavKey>.unifiedHistoryEntries(
    activity: MainActivity,
    navigator: Navigator,
) {
    screen<UnifiedHistoryRoute> {
        UnifiedHistoryScreen(
            activity = activity,
            onBack = { navigator.goBack() },
        )
    }
    screen<LocalPlaylistsRoute> {
        LocalPlaylistsScreen(
            activity = activity,
            onBack = { navigator.goBack() },
        )
    }
}
