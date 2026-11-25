package com.yenaly.han1meviewer.SitesHub.Cosplaytele.ui.navigation

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
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.data.model.CosplayteleTerm
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.ui.history.CosplayteleHistoryScreen
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.ui.home.CosplayteleHomeScreen
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.ui.list.CosplayteleListScreen
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.ui.post.CosplaytellePhotosScreen
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.ui.post.CosplaytellePostScreen
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.ui.search.CosplayteleSearchScreen
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.ui.settings.CosplayteleSettingsScreen
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
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
object CosplayteleHomeRoute : NavKey

@Serializable
object CosplayteleSettingsHubRoute : NavKey

@Serializable
data class CosplayteleSearchRoute(val query: String? = null) : NavKey

@Serializable
data class CosplaytelePostRoute(val slug: String, val resumePosition: Long = 0L) : NavKey

@Serializable
data class CosplaytelePhotosRoute(val slug: String) : NavKey

@Serializable
data class CosplayteleListRoute(
    val path: String,
    val title: String,
    val collectTerms: Boolean = false,
) : NavKey

@Serializable
object CosplayteleHistoryRoute : NavKey

@Serializable
object CosplayteleSettingsRoute : NavKey

fun CosplayteleTerm.toListRoute() = CosplayteleListRoute(path = path, title = name)

@Composable
fun CosplayteleNavHost(
    activity: MainActivity,
    navigator: Navigator,
    navigationState: NavigationState,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    val openPost: (String) -> Unit = { navigator.navigateSafely(CosplaytelePostRoute(it)) }
    val openTerm: (CosplayteleTerm) -> Unit = { navigator.navigateSafely(it.toListRoute()) }

    val entryProvider = entryProvider<NavKey> {
        screen<CosplayteleHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                CosplayteleHomeScreen(
                    onNavigateToPost = openPost,
                    onNavigateToSearch = { navigator.navigateSafely(CosplayteleSearchRoute(it)) },
                    onNavigateToHistory = { navigator.navigateSafely(CosplayteleHistoryRoute) },
                    onNavigateToTerm = openTerm,
                )
            }
        }

        screen<CosplayteleSettingsHubRoute> {
            SiteSettingsHubScreen(
                site = SiteType.COSPLAYTELE,
                onResetSite = { com.yenaly.han1meviewer.SitesHub.Cosplaytele.common.CosplayteleSettings.resetToDefaults() },
                siteEntries = listOf(
                    SiteSettingsEntry(
                        key = "playback",
                        title = stringResource(R.string.cosplaytele_settings_title),
                        subtitle = stringResource(R.string.cosplaytele_settings_summary),
                        icon = Icons.Default.HighQuality,
                        onClick = { navigator.navigateSafely(CosplayteleSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "history",
                        title = stringResource(R.string.cosplaytele_history_title),
                        subtitle = stringResource(R.string.cosplaytele_history_summary),
                        icon = Icons.Default.History,
                        onClick = { navigator.navigateSafely(CosplayteleHistoryRoute) },
                    ),
                ),
                onBack = { navigator.goBack() },
                onOpenAppSettings = { navigator.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = { navigator.navigateSafely(SitePlayerSettingsRoute(SiteType.COSPLAYTELE.name)) },
                onOpenNetworkSettings = { navigator.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navigator.navigateSafely(UnifiedHistoryRoute) },
            )
        }

        screen<CosplayteleSettingsRoute> {
            CosplayteleSettingsScreen(onBack = { navigator.goBack() })
        }

        screen<CosplayteleSearchRoute> {
            val route = it
            CosplayteleSearchScreen(
                initialQuery = route.query,
                onBack = { navigator.goBack() },
                onNavigateToPost = openPost,
                onNavigateToTerm = openTerm,
            )
        }

        screen<CosplayteleListRoute> {
            val route = it
            CosplayteleListScreen(
                path = route.path,
                title = route.title,
                collectTerms = route.collectTerms,
                onBack = { navigator.goBack() },
                onNavigateToPost = openPost,
                onNavigateToTerm = openTerm,
            )
        }

        screen<CosplaytelePostRoute> {
            val route = it
            CosplaytellePostScreen(
                slug = route.slug,
                resumePosition = route.resumePosition,
                onBack = { navigator.goBack() },
                onNavigateToPost = openPost,
                onNavigateToTerm = openTerm,
                onOpenPhotos = { navigator.navigateSafely(CosplaytelePhotosRoute(route.slug)) },
            )
        }

        screen<CosplaytelePhotosRoute> {
            val route = it
            CosplaytellePhotosScreen(
                slug = route.slug,
                onBack = { navigator.goBack() },
            )
        }

        screen<CosplayteleHistoryRoute> {
            CosplayteleHistoryScreen(
                onNavigateToPost = openPost,
                onBack = { navigator.goBack() },
            )
        }

        siteHubEntries(
            navigator = navigator,
            site = SiteType.COSPLAYTELE,
            config = { cosplayteleHubConfig(navigator, activity) },
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
