package com.yenaly.han1meviewer.SitesHub.FuckingSession.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.SiteHubSection
import com.yenaly.han1meviewer.ui.screen.site.rememberPlayerSettingsEntry
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig

@Composable
fun fuckingSessionHubConfig(navigator: Navigator, activity: MainActivity): SiteHubConfig =
    standardSiteHubConfig(
        site = SiteType.FUCKINGSESSION,
        navigator = navigator,
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
                            navigator.navigateSafely(FuckingSessionListRoute("HD Porn", "/category/hd-porn"))
                        },
                    ),
                    SiteHubEntry(
                        key = "anal",
                        title = "Anal",
                        icon = Icons.Filled.Category,
                        onClick = {
                            navigator.navigateSafely(FuckingSessionListRoute("Anal", "/category/anal"))
                        },
                    ),
                    SiteHubEntry(
                        key = "milf",
                        title = "MILF",
                        icon = Icons.Filled.Star,
                        onClick = {
                            navigator.navigateSafely(FuckingSessionListRoute("MILF", "/category/milf"))
                        },
                    ),
                    SiteHubEntry(
                        key = "asian-uncensored",
                        title = "Asian Uncensored",
                        icon = Icons.Filled.Language,
                        onClick = {
                            navigator.navigateSafely(
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
                onClick = { navigator.navigateSafely(FuckingSessionHistoryRoute) },
            ),
        ),
        customizeEntries = listOf(
            SiteHubEntry(
                key = "playback",
                title = stringResource(R.string.fuckingsession_settings_playback_title),
                subtitle = stringResource(R.string.fuckingsession_settings_playback_summary),
                icon = Icons.Filled.HighQuality,
                onClick = { navigator.navigateSafely(FuckingSessionSettingsRoute) },
            ),
            rememberPlayerSettingsEntry(navigator, SiteType.FUCKINGSESSION),
        ),
    )
