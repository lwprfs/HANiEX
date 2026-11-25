package com.yenaly.han1meviewer.SitesHub.SupJav.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.rememberPlayerSettingsEntry
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig

@Composable
fun supJavHubConfig(navigator: Navigator, activity: MainActivity): SiteHubConfig =
    standardSiteHubConfig(
        site = SiteType.SUPJAV,
        navigator = navigator,
        activity = activity,
        siteSettingsRoute = SupJavSettingsHubRoute,
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.supjav_history_title),
                subtitle = stringResource(R.string.supjav_history_summary),
                icon = Icons.Filled.History,
                onClick = { navigator.navigateSafely(SupJavHistoryRoute) },
            ),
        ),
        customizeEntries = listOf(
            SiteHubEntry(
                key = "playback",
                title = stringResource(R.string.supjav_settings_playback_title),
                subtitle = stringResource(R.string.supjav_settings_playback_summary),
                icon = Icons.Filled.HighQuality,
                onClick = { navigator.navigateSafely(SupJavSettingsRoute) },
            ),
            rememberPlayerSettingsEntry(navigator, SiteType.SUPJAV),
        ),
    )
