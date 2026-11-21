package com.yenaly.han1meviewer.XXXTube.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.XXXTube.common.XXXTubeConstants
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.SiteHubSection
import com.yenaly.han1meviewer.ui.screen.site.rememberPlayerSettingsEntry
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig

@Composable
fun xxxTubeHubConfig(navController: NavHostController, activity: MainActivity): SiteHubConfig =
    standardSiteHubConfig(
        site = SiteType.XXXTUBE,
        navController = navController,
        activity = activity,
        siteSettingsRoute = XXXTubeSettingsHubRoute,
        subscriptionSections = listOf(
            SiteHubSection(
                null,
                listOf(
                    SiteHubEntry(
                        key = "models",
                        title = stringResource(R.string.xxxtube_models),
                        icon = Icons.Filled.People,
                        onClick = {
                            navController.navigateSafely(XXXTubeEntityRoute(XXXTubeConstants.EntityKind.MODELS.key))
                        },
                    ),
                    SiteHubEntry(
                        key = "categories",
                        title = stringResource(R.string.xxxtube_categories),
                        icon = Icons.Filled.Category,
                        onClick = {
                            navController.navigateSafely(XXXTubeEntityRoute(XXXTubeConstants.EntityKind.CATEGORIES.key))
                        },
                    ),
                    SiteHubEntry(
                        key = "top-rated",
                        title = "Top Rated",
                        icon = Icons.Filled.Star,
                        onClick = {
                            navController.navigateSafely(XXXTubeListRoute("Top Rated", "/videos/?by=rating"))
                        },
                    ),
                    SiteHubEntry(
                        key = "longest",
                        title = "Longest",
                        icon = Icons.Filled.HighQuality,
                        onClick = {
                            navController.navigateSafely(XXXTubeListRoute("Longest", "/videos/?by=duration"))
                        },
                    ),
                ),
            ),
        ),
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.xxxtube_history_title),
                subtitle = stringResource(R.string.xxxtube_history_summary),
                icon = Icons.Filled.History,
                onClick = { navController.navigateSafely(XXXTubeHistoryRoute) },
            ),
        ),
        customizeEntries = listOf(
            SiteHubEntry(
                key = "playback",
                title = stringResource(R.string.xxxtube_settings_playback_title),
                subtitle = stringResource(R.string.xxxtube_settings_playback_summary),
                icon = Icons.Filled.HighQuality,
                onClick = { navController.navigateSafely(XXXTubeSettingsRoute) },
            ),
            rememberPlayerSettingsEntry(navController, SiteType.XXXTUBE),
        ),
    )
