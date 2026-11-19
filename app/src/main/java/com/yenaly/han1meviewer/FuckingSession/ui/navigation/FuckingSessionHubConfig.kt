package com.yenaly.han1meviewer.FuckingSession.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.SiteHubSection
import com.yenaly.han1meviewer.ui.screen.site.rememberPlayerSettingsEntry
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig

@Composable
fun fuckingSessionHubConfig(navController: NavHostController, activity: MainActivity): SiteHubConfig =
    standardSiteHubConfig(
        site = SiteType.FUCKINGSESSION,
        navController = navController,
        activity = activity,
        siteSettingsRoute = FuckingSessionSettingsHubRoute,
        subscriptionSections = listOf(
            SiteHubSection(
                null,
                listOf(
                    SiteHubEntry(
                        key = "hd-porn",
                        title = "HD Porn",
                        icon = Icons.Filled.HighQuality,
                        onClick = {
                            navController.navigateSafely(FuckingSessionListRoute("HD Porn", "/category/hd-porn"))
                        },
                    ),
                    SiteHubEntry(
                        key = "anal",
                        title = "Anal",
                        icon = Icons.Filled.Category,
                        onClick = {
                            navController.navigateSafely(FuckingSessionListRoute("Anal", "/category/anal"))
                        },
                    ),
                    SiteHubEntry(
                        key = "milf",
                        title = "MILF",
                        icon = Icons.Filled.Star,
                        onClick = {
                            navController.navigateSafely(FuckingSessionListRoute("MILF", "/category/milf"))
                        },
                    ),
                    SiteHubEntry(
                        key = "asian-uncensored",
                        title = "Asian Uncensored",
                        icon = Icons.Filled.Language,
                        onClick = {
                            navController.navigateSafely(
                                FuckingSessionListRoute("Asian Uncensored", "/category/asian-uncensored")
                            )
                        },
                    ),
                ),
            ),
        ),
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.fuckingsession_history_title),
                subtitle = stringResource(R.string.fuckingsession_history_summary),
                icon = Icons.Filled.History,
                onClick = { navController.navigateSafely(FuckingSessionHistoryRoute) },
            ),
        ),
        customizeEntries = listOf(
            SiteHubEntry(
                key = "playback",
                title = stringResource(R.string.fuckingsession_settings_playback_title),
                subtitle = stringResource(R.string.fuckingsession_settings_playback_summary),
                icon = Icons.Filled.HighQuality,
                onClick = { navController.navigateSafely(FuckingSessionSettingsRoute) },
            ),
            rememberPlayerSettingsEntry(navController, SiteType.FUCKINGSESSION),
        ),
    )
