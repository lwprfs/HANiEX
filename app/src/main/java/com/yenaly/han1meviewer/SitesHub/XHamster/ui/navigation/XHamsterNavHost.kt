package com.yenaly.han1meviewer.SitesHub.XHamster.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator
import androidx.navigation3.runtime.entryProvider
import com.yenaly.han1meviewer.ui.navigation.NavigationState
import com.yenaly.han1meviewer.ui.navigation.SiteNavDisplay
import com.yenaly.han1meviewer.ui.navigation.screen
import com.yenaly.han1meviewer.ui.navigation.settings.globalSettingsEntries
import com.yenaly.han1meviewer.ui.screen.site.siteHubEntries

import com.yenaly.han1meviewer.ui.navigation.site.SiteProfileRoute
import com.yenaly.han1meviewer.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.account.XHamsterAccountScreen
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.categories.XHamsterCategoriesScreen
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.categories.XHamsterCategoryScreen
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.history.XHamsterHistoryScreen
import com.yenaly.han1meviewer.SitesHub.XHamster.common.XHamsterConstants
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.entities.XHamsterAccountSettingsScreen
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.entities.XHamsterChannelsScreen
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.entities.XHamsterCreatorDashboardScreen
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.entities.XHamsterCreatorsScreen
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.entities.XHamsterPhotosScreen
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.gallery.XHamsterGalleryScreen
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.profile.XHamsterProfileScreen
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.entities.XHamsterTopCreatorVideosScreen
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.home.XHamsterHomeScreen
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.discover.XHamsterDiscoverActions
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.entities.XHamsterStarsScreen
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.library.XHamsterCollectionScreen
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.library.XHamsterLibraryScreen
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.search.XHamsterSearchScreen
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.settings.XHamsterSettingsScreen
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.video.XHamsterVideoScreen
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.NavigationManager
import com.yenaly.han1meviewer.ui.navigation.main.MainDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.NetworkSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.PlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SitePlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.UnifiedHistoryRoute
import com.yenaly.han1meviewer.ui.screen.settings.SiteSettingsEntry
import com.yenaly.han1meviewer.ui.screen.settings.SiteSettingsHubScreen
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
object XHamsterHomeRoute : NavKey

@Serializable
object XHamsterSettingsHubRoute : NavKey

@Serializable
data class XHamsterSearchRoute(val query: String? = null) : NavKey

@Serializable
data class XHamsterVideoRoute(val videoCode: String, val resumePosition: Long = 0L) : NavKey

@Serializable
object XHamsterHistoryRoute : NavKey

@Serializable
object XHamsterSettingsRoute : NavKey

@Serializable
object XHamsterCategoriesRoute : NavKey

@Serializable
object XHamsterAccountRoute : NavKey

@Serializable
data class XHamsterCategoryRoute(val path: String, val title: String) : NavKey

@Serializable
object XHamsterChannelsRoute : NavKey

@Serializable
object XHamsterCreatorsRoute : NavKey

@Serializable
object XHamsterPhotosRoute : NavKey

@Serializable
object XHamsterPrivacyRoute : NavKey

@Serializable
data class XHamsterMomentsRoute(val kind: String, val title: String) : NavKey

@Serializable
data class XHamsterTagShortsRoute(val path: String, val title: String) : NavKey

@Serializable
object XHamsterTopCreatorVideosRoute : NavKey

@Serializable
object XHamsterDashboardRoute : NavKey

@Serializable
object XHamsterAccountSettingsRoute : NavKey

@Serializable
data class XHamsterProfileRoute(val path: String, val title: String) : NavKey

@Serializable
data class XHamsterGalleryRoute(val url: String, val title: String) : NavKey

@Serializable
data class XHamsterLibraryRoute(val tab: String = "HISTORY") : NavKey

@Serializable
data class XHamsterCollectionRoute(val path: String, val title: String) : NavKey

@Serializable
object XHamsterStarsRoute : NavKey

private fun isProfilePath(path: String): Boolean =
    path.startsWith("/users/") || path.startsWith("/creators/") || path.startsWith("/pornstars/") ||
            path.startsWith("/channels/")

@Composable
fun XHamsterNavHost(
    activity: MainActivity,
    navigator: Navigator,
    navigationState: NavigationState,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    val discover = XHamsterDiscoverActions(
        onOpenCreator = { path, title -> navigator.navigateSafely(XHamsterProfileRoute(path, title)) },
        onOpenChannel = { path, title -> navigator.navigateSafely(XHamsterCategoryRoute(path, title)) },
        onOpenVideo = { code -> navigator.navigateSafely(XHamsterVideoRoute(code)) },
        onSeeCreators = { navigator.navigateSafely(XHamsterStarsRoute) },
        onSeeChannels = { navigator.navigateSafely(XHamsterChannelsRoute) },
        onSeeSubscriptions = { navigator.navigateSafely(XHamsterLibraryRoute("SUBSCRIPTIONS")) },
    )

    val entryProvider = entryProvider<NavKey> {
        screen<XHamsterHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                XHamsterHomeScreen(
                    onNavigateToVideo = { code ->
                        navigator.navigateSafely(XHamsterVideoRoute(code))
                    },
                    onNavigateToSearch = { query ->
                        navigator.navigateSafely(XHamsterSearchRoute(query))
                    },
                    onNavigateToCategories = {
                        navigator.navigateSafely(XHamsterCategoriesRoute)
                    },
                    discover = discover,
                )
            }
        }

        screen<XHamsterSettingsHubRoute> {
            SiteSettingsHubScreen(
                site = SiteType.XHAMSTER,
                onResetSite = { com.yenaly.han1meviewer.SitesHub.XHamster.common.XHamsterSettings.resetToDefaults() },
                siteEntries = listOf(
                    SiteSettingsEntry(
                        key = "playback",
                        title = stringResource(R.string.xhamster_settings_title),
                        subtitle = stringResource(R.string.xhamster_settings_summary),
                        icon = Icons.Default.HighQuality,
                        onClick = { navigator.navigateSafely(XHamsterSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "account",
                        title = stringResource(R.string.xhamster_account_settings),
                        subtitle = stringResource(R.string.xhamster_account_settings_summary),
                        icon = Icons.Default.Person,
                        onClick = { navigator.navigateSafely(XHamsterAccountSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "privacy",
                        title = "Privacy",
                        subtitle = "Pause watch history, translate titles",
                        icon = Icons.Default.Lock,
                        onClick = { navigator.navigateSafely(XHamsterPrivacyRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "history",
                        title = stringResource(R.string.xhamster_history_title),
                        subtitle = stringResource(R.string.xhamster_history_summary),
                        icon = Icons.Default.History,
                        onClick = { navigator.navigateSafely(XHamsterHistoryRoute) },
                    ),
                ),
                onBack = { navigator.goBack() },
                onOpenAppSettings = { navigator.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = { navigator.navigateSafely(SitePlayerSettingsRoute(SiteType.XHAMSTER.name)) },
                onOpenNetworkSettings = { navigator.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navigator.navigateSafely(UnifiedHistoryRoute) },
            )
        }

        screen<XHamsterPrivacyRoute> {
            com.yenaly.han1meviewer.SitesHub.XHamster.ui.settings.XHamsterPrivacyScreen(
                onBack = { navigator.goBack() },
            )
        }

        screen<XHamsterMomentsRoute> {
            val route = it
            com.yenaly.han1meviewer.SitesHub.XHamster.ui.shorts.XHamsterMomentsScreen(
                kind = runCatching {
                    com.yenaly.han1meviewer.SitesHub.XHamster.data.remote.XHamsterMomentsRepo.Feed.valueOf(route.kind)
                }.getOrDefault(com.yenaly.han1meviewer.SitesHub.XHamster.data.remote.XHamsterMomentsRepo.Feed.SHORTS),
                title = route.title,
                onBack = { navigator.goBack() },
            )
        }

        screen<XHamsterTagShortsRoute> {
            val route = it
            com.yenaly.han1meviewer.SitesHub.XHamster.ui.shorts.XHamsterTagShortsScreen(
                path = route.path,
                title = route.title,
                onBack = { navigator.goBack() },
            )
        }

        screen<XHamsterSettingsRoute> {
            XHamsterSettingsScreen(onBack = { navigator.goBack() })
        }

        screen<XHamsterSearchRoute> {
            val route = it
            XHamsterSearchScreen(
                initialQuery = route.query,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(XHamsterVideoRoute(code))
                },
                onOpenGallery = { url -> navigator.navigateSafely(XHamsterGalleryRoute(url, "")) },
                onOpenAuthor = { path, title ->
                    if (isProfilePath(path)) navigator.navigateSafely(XHamsterProfileRoute(path, title))
                    else navigator.navigateSafely(XHamsterCategoryRoute(path, title))
                },
                discover = discover,
            )
        }

        screen<XHamsterLibraryRoute> {
            val route = it
            XHamsterLibraryScreen(
                initialTab = route.tab,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code -> navigator.navigateSafely(XHamsterVideoRoute(code)) },
                onOpenCollection = { path, title ->
                    navigator.navigateSafely(XHamsterCollectionRoute(path, title))
                },
                discover = discover,
                onSignIn = { navigator.navigateSafely(XHamsterAccountRoute) },
            )
        }

        screen<XHamsterCollectionRoute> {
            val route = it
            XHamsterCollectionScreen(
                path = route.path,
                title = route.title,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code -> navigator.navigateSafely(XHamsterVideoRoute(code)) },
            )
        }

        screen<XHamsterStarsRoute> {
            XHamsterStarsScreen(
                onBack = { navigator.goBack() },
                onOpenStar = { path, title -> navigator.navigateSafely(XHamsterProfileRoute(path, title)) },
                discover = discover,
            )
        }

        screen<XHamsterVideoRoute> {
            val route = it
            XHamsterVideoScreen(
                videoId = route.videoCode,
                resumePosition = route.resumePosition,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(XHamsterVideoRoute(code))
                },
                onNavigateToPath = { path, title ->
                    if (isProfilePath(path)) {
                        navigator.navigateSafely(XHamsterProfileRoute(path, title))
                    } else {
                        navigator.navigateSafely(XHamsterCategoryRoute(path, title))
                    }
                },
                onNavigateToUploader = { path, title ->
                    if (isProfilePath(path)) {
                        navigator.navigateSafely(XHamsterProfileRoute(path, title))
                    } else {
                        navigator.navigateSafely(XHamsterCategoryRoute(path, title))
                    }
                },
            )
        }

        screen<XHamsterAccountRoute> {
            XHamsterAccountScreen(
                onBack = { navigator.goBack() },
                onOpenDashboard = { navigator.navigateSafely(XHamsterDashboardRoute) },
                onOpenSettings = { navigator.navigateSafely(XHamsterAccountSettingsRoute) },
                onOpenPath = { path, title ->
                    if (isProfilePath(path)) {
                        navigator.navigateSafely(XHamsterProfileRoute(path, title))
                    } else {
                        navigator.navigateSafely(XHamsterCategoryRoute(path, title))
                    }
                },
            )
        }

        screen<XHamsterCategoriesRoute> {
            XHamsterCategoriesScreen(
                onBack = { navigator.goBack() },
                onOpenCategory = { path, title ->
                    navigator.navigateSafely(XHamsterCategoryRoute(path, title))
                },
            )
        }

        screen<XHamsterCategoryRoute> {
            val route = it
            XHamsterCategoryScreen(
                path = route.path,
                title = route.title,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(XHamsterVideoRoute(code))
                },
            )
        }

        screen<XHamsterHistoryRoute> {
            XHamsterHistoryScreen(
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(XHamsterVideoRoute(code))
                },
                onBack = { navigator.goBack() },
            )
        }

        screen<XHamsterChannelsRoute> {
            XHamsterChannelsScreen(
                onBack = { navigator.goBack() },
                onOpenChannel = { path, title ->
                    if (isProfilePath(path)) {
                        navigator.navigateSafely(XHamsterProfileRoute(path, title))
                    } else {
                        navigator.navigateSafely(XHamsterCategoryRoute(path, title))
                    }
                },
            )
        }

        screen<XHamsterCreatorsRoute> {
            XHamsterCreatorsScreen(
                onBack = { navigator.goBack() },
                onOpenCreator = { path, title ->
                    navigator.navigateSafely(XHamsterProfileRoute(path, title))
                },
                onOpenTopVideos = { navigator.navigateSafely(XHamsterTopCreatorVideosRoute) },
            )
        }

        screen<XHamsterTopCreatorVideosRoute> {
            XHamsterTopCreatorVideosScreen(
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(XHamsterVideoRoute(code))
                },
            )
        }

        screen<XHamsterPhotosRoute> {
            XHamsterPhotosScreen(
                onBack = { navigator.goBack() },
                onOpenGallery = { url ->
                    navigator.navigateSafely(XHamsterGalleryRoute(url, ""))
                },
                onOpenAuthor = { path, title ->
                    navigator.navigateSafely(XHamsterProfileRoute(path, title))
                },
            )
        }

        screen<XHamsterGalleryRoute> {
            val route = it
            XHamsterGalleryScreen(
                url = route.url,
                title = route.title,
                onBack = { navigator.goBack() },
                onOpenAuthor = { path, title ->
                    if (isProfilePath(path)) {
                        navigator.navigateSafely(XHamsterProfileRoute(path, title))
                    } else {
                        navigator.navigateSafely(XHamsterCategoryRoute(path, title))
                    }
                },
            )
        }

        screen<XHamsterProfileRoute> {
            val route = it
            XHamsterProfileScreen(
                path = route.path,
                title = route.title,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(XHamsterVideoRoute(code))
                },
                onOpenPath = { path, title ->
                    if (isProfilePath(path)) {
                        navigator.navigateSafely(XHamsterProfileRoute(path, title))
                    } else {
                        navigator.navigateSafely(XHamsterCategoryRoute(path, title))
                    }
                },
            )
        }

        screen<XHamsterDashboardRoute> {
            XHamsterCreatorDashboardScreen(
                onBack = { navigator.goBack() },
                onOpenTopVideos = { navigator.navigateSafely(XHamsterTopCreatorVideosRoute) },
            )
        }

        screen<XHamsterAccountSettingsRoute> {
            XHamsterAccountSettingsScreen(
                onBack = { navigator.goBack() },
                onOpenInBrowser = { path ->
                    openInBrowser(activity, XHamsterConstants.listUrl(path))
                },
            )
        }

        siteHubEntries(
            navigator = navigator,
            site = SiteType.XHAMSTER,
            config = { xHamsterHubConfig(navigator, activity) },
        )

        globalSettingsEntries(
            activity = activity,
            navigator = navigator,
            homeFallback = SiteProfileRoute,
        )
    }

    SiteNavDisplay(
        navigator = navigator,
        navigationState = navigationState,
        entryProvider = entryProvider,
        modifier = modifier,
        socialSite = com.yenaly.han1meviewer.SiteType.XHAMSTER,
    )
}

private fun openInBrowser(activity: MainActivity, url: String) {
    runCatching {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
