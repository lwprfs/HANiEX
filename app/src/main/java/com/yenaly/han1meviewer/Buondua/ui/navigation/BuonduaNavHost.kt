package com.yenaly.han1meviewer.Buondua.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator
import androidx.navigation3.runtime.entryProvider
import com.yenaly.han1meviewer.ui.navigation.NavigationState
import com.yenaly.han1meviewer.ui.navigation.SiteNavDisplay
import com.yenaly.han1meviewer.ui.navigation.screen
import com.yenaly.han1meviewer.ui.navigation.settings.globalSettingsEntries
import com.yenaly.han1meviewer.ui.screen.site.siteHubEntries

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.Buondua.ui.album.BuonduaAlbumScreen
import com.yenaly.han1meviewer.Buondua.ui.album.BuonduaPhotosScreen
import com.yenaly.han1meviewer.Buondua.ui.history.BuonduaHistoryScreen
import com.yenaly.han1meviewer.Buondua.ui.home.BuonduaHomeScreen
import com.yenaly.han1meviewer.Buondua.ui.home.BuonduaTagScreen
import com.yenaly.han1meviewer.Buondua.ui.search.BuonduaSearchScreen
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.NavigationManager
import com.yenaly.han1meviewer.ui.navigation.main.MainDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.NetworkSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SitePlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.UnifiedHistoryRoute
import com.yenaly.han1meviewer.ui.navigation.site.SiteProfileRoute
import com.yenaly.han1meviewer.ui.screen.settings.SiteSettingsEntry
import com.yenaly.han1meviewer.ui.screen.settings.SiteSettingsHubScreen
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
object BuonduaHomeRoute : NavKey

@Serializable
object BuonduaSettingsHubRoute : NavKey

@Serializable
data class BuonduaSearchRoute(val query: String? = null) : NavKey

@Serializable
data class BuonduaAlbumRoute(val videoCode: String, val resumePosition: Long = 0L) : NavKey

@Serializable
data class BuonduaPhotosRoute(val code: String) : NavKey

@Serializable
data class BuonduaTagRoute(val url: String, val title: String) : NavKey

@Serializable
object BuonduaHistoryRoute : NavKey

@Composable
fun buonduaHubConfig(navigator: Navigator, activity: MainActivity): SiteHubConfig =
    standardSiteHubConfig(
        site = SiteType.BUONDUA,
        navigator = navigator,
        activity = activity,
        siteSettingsRoute = BuonduaSettingsHubRoute,
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.buondua_history_title),
                subtitle = stringResource(R.string.buondua_history_summary),
                icon = Icons.Filled.History,
                onClick = { navigator.navigateSafely(BuonduaHistoryRoute) },
            ),
        ),
        customizeEntries = emptyList(),
    )

@Composable
fun BuonduaNavHost(
    activity: MainActivity,
    navigator: Navigator,
    navigationState: NavigationState,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    val entryProvider = entryProvider<NavKey> {
        screen<BuonduaHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                BuonduaHomeScreen(
                    onNavigateToAlbum = { code -> navigator.navigateSafely(BuonduaAlbumRoute(code)) },
                    onNavigateToSearch = { q -> navigator.navigateSafely(BuonduaSearchRoute(q)) },
                    onNavigateToHistory = { navigator.navigateSafely(BuonduaHistoryRoute) },
                )
            }
        }

        screen<BuonduaSettingsHubRoute> {
            SiteSettingsHubScreen(
                site = SiteType.BUONDUA,
                siteEntries = listOf(
                    SiteSettingsEntry(
                        key = "history",
                        title = stringResource(R.string.buondua_history_title),
                        subtitle = stringResource(R.string.buondua_history_summary),
                        icon = Icons.Default.History,
                        onClick = { navigator.navigateSafely(BuonduaHistoryRoute) },
                    ),
                ),
                onBack = { navigator.goBack() },
                onOpenAppSettings = { navigator.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = { navigator.navigateSafely(SitePlayerSettingsRoute(SiteType.BUONDUA.name)) },
                onOpenNetworkSettings = { navigator.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navigator.navigateSafely(UnifiedHistoryRoute) },
            )
        }

        screen<BuonduaSearchRoute> {
            val route = it
            BuonduaSearchScreen(
                initialQuery = route.query,
                onBack = { navigator.goBack() },
                onNavigateToAlbum = { code -> navigator.navigateSafely(BuonduaAlbumRoute(code)) },
            )
        }

        screen<BuonduaTagRoute> {
            val route = it
            BuonduaTagScreen(
                url = route.url,
                title = route.title,
                onBack = { navigator.goBack() },
                onNavigateToAlbum = { code -> navigator.navigateSafely(BuonduaAlbumRoute(code)) },
            )
        }

        screen<BuonduaAlbumRoute> {
            val route = it
            BuonduaAlbumScreen(
                code = route.videoCode,
                onBack = { navigator.goBack() },
                onNavigateToAlbum = { code -> navigator.navigateSafely(BuonduaAlbumRoute(code)) },
                onNavigateToTag = { url, title -> navigator.navigateSafely(BuonduaTagRoute(url, title)) },
                onOpenPhotos = { navigator.navigateSafely(BuonduaPhotosRoute(route.videoCode)) },
            )
        }

        screen<BuonduaPhotosRoute> {
            val route = it
            BuonduaPhotosScreen(
                code = route.code,
                onBack = { navigator.goBack() },
            )
        }

        screen<BuonduaHistoryRoute> {
            BuonduaHistoryScreen(
                onNavigateToAlbum = { code -> navigator.navigateSafely(BuonduaAlbumRoute(code)) },
                onBack = { navigator.goBack() },
            )
        }

        siteHubEntries(
            navigator = navigator,
            site = SiteType.BUONDUA,
            config = { buonduaHubConfig(navigator, activity) },
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
    )
}
