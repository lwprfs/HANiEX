package com.yenaly.han1meviewer.SupJav.ui.navigation

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

/** SupJav: no account and no subscriptions, so only real pages are listed. */
@Composable
fun supJavHubConfig(navController: NavHostController, activity: MainActivity): SiteHubConfig =
    standardSiteHubConfig(
        site = SiteType.SUPJAV,
        navController = navController,
        activity = activity,
        siteSettingsRoute = SupJavSettingsHubRoute,
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.supjav_history_title),
                subtitle = stringResource(R.string.supjav_history_summary),
                icon = Icons.Filled.History,
                onClick = { navController.navigateSafely(SupJavHistoryRoute) },
            ),
        ),
        customizeEntries = listOf(
            SiteHubEntry(
                key = "playback",
                title = stringResource(R.string.supjav_settings_playback_title),
                subtitle = stringResource(R.string.supjav_settings_playback_summary),
                icon = Icons.Filled.HighQuality,
                onClick = { navController.navigateSafely(SupJavSettingsRoute) },
            ),
            rememberPlayerSettingsEntry(navController, SiteType.SUPJAV),
        ),
    )
