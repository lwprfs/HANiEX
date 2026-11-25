package com.yenaly.han1meviewer.SitesHub.FullVideos.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.SitesHub.FullVideos.common.FullVideosConstants
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.SiteHubSection
import com.yenaly.han1meviewer.ui.screen.site.rememberPlayerSettingsEntry
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig

@Composable
fun fullVideosHubConfig(navigator: Navigator, activity: MainActivity): SiteHubConfig =
    standardSiteHubConfig(
        site = SiteType.FULLVIDEOS,
        navigator = navigator,
        activity = activity,
        siteSettingsRoute = FullVideosSettingsHubRoute,
        subscriptionSections = listOf(
            SiteHubSection(
                null,
                listOf(
                    SiteHubEntry(
                        key = "latest",
                        title = "Latest",
                        icon = Icons.Filled.NewReleases,
                        onClick = {
                            navigator.navigateSafely(FullVideosListRoute("Latest", "/latest-updates"))
                        },
                    ),
                    SiteHubEntry(
                        key = "top-rated",
                        title = "Top Rated",
                        icon = Icons.Filled.Star,
                        onClick = {
                            navigator.navigateSafely(FullVideosListRoute("Top Rated", "/top-rated"))
                        },
                    ),
                    SiteHubEntry(
                        key = "most-popular",
                        title = "Most Viewed",
                        icon = Icons.Filled.Visibility,
                        onClick = {
                            navigator.navigateSafely(FullVideosListRoute("Most Viewed", "/most-popular"))
                        },
                    ),
                    SiteHubEntry(
                        key = "categories",
                        title = stringResource(R.string.fullvideos_categories),
                        icon = Icons.Filled.Category,
                        onClick = {
                            navigator.navigateSafely(FullVideosBrowseRoute(FullVideosConstants.EntityKind.CATEGORIES.key))
                        },
                    ),
                    SiteHubEntry(
                        key = "models",
                        title = stringResource(R.string.fullvideos_models),
                        icon = Icons.Filled.People,
                        onClick = {
                            navigator.navigateSafely(FullVideosBrowseRoute(FullVideosConstants.EntityKind.MODELS.key))
                        },
                    ),
                    SiteHubEntry(
                        key = "sites",
                        title = stringResource(R.string.fullvideos_sites),
                        icon = Icons.Filled.Language,
                        onClick = {
                            navigator.navigateSafely(FullVideosBrowseRoute(FullVideosConstants.EntityKind.SITES.key))
                        },
                    ),
                ),
            ),
        ),
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.fullvideos_history_title),
                subtitle = stringResource(R.string.fullvideos_history_summary),
                icon = Icons.Filled.History,
                onClick = { navigator.navigateSafely(FullVideosHistoryRoute) },
            ),
        ),
        customizeEntries = listOf(
            SiteHubEntry(
                key = "playback",
                title = stringResource(R.string.fullvideos_settings_playback_title),
                subtitle = stringResource(R.string.fullvideos_settings_playback_summary),
                icon = Icons.Filled.HighQuality,
                onClick = { navigator.navigateSafely(FullVideosSettingsRoute) },
            ),
            rememberPlayerSettingsEntry(navigator, SiteType.FULLVIDEOS),
        ),
    )
