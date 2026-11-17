package com.yenaly.han1meviewer.FullVideos.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.rememberPlayerSettingsEntry
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig

@Composable
fun fullVideosHubConfig(navController: NavHostController, activity: MainActivity): SiteHubConfig =
    standardSiteHubConfig(
        site = SiteType.FULLVIDEOS,
        navController = navController,
        activity = activity,
        siteSettingsRoute = FullVideosSettingsHubRoute,
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.fullvideos_history_title),
                subtitle = stringResource(R.string.fullvideos_history_summary),
                icon = Icons.Filled.History,
                onClick = { navController.navigateSafely(FullVideosHistoryRoute) },
            ),
        ),
        customizeEntries = listOf(
            SiteHubEntry(
                key = "playback",
                title = stringResource(R.string.fullvideos_settings_playback_title),
                subtitle = stringResource(R.string.fullvideos_settings_playback_summary),
                icon = Icons.Filled.HighQuality,
                onClick = { navController.navigateSafely(FullVideosSettingsRoute) },
            ),
            rememberPlayerSettingsEntry(navController, SiteType.FULLVIDEOS),
        ),
    )
