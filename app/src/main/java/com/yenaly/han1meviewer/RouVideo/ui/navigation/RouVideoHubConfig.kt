package com.yenaly.han1meviewer.RouVideo.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.RouVideo.common.RouVideoTranslation
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.rememberPlayerSettingsEntry
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig

/** RouVideo: categories and history in Library; host, site settings, translation in Customize. */
@Composable
fun rouVideoHubConfig(navController: NavHostController, activity: MainActivity): SiteHubConfig {
    val translation = if (RouVideoTranslation.available) {
        SiteHubEntry(
            key = "translation",
            title = stringResource(R.string.rouvideo_tr_title),
            subtitle = stringResource(R.string.rouvideo_tr_privacy),
            icon = Icons.Filled.Translate,
            onClick = { navController.navigateSafely(RouVideoTranslationSettingsRoute) },
        )
    } else null
    return standardSiteHubConfig(
        site = SiteType.ROUVIDEO,
        navController = navController,
        activity = activity,
        siteSettingsRoute = RouVideoSettingsHubRoute,
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "categories",
                title = stringResource(R.string.rouvideo_categories),
                subtitle = stringResource(R.string.rouvideo_categories_summary),
                icon = Icons.Filled.Category,
                onClick = { navController.navigateSafely(RouVideoCategoriesRoute) },
            ),
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.rouvideo_history_title),
                subtitle = stringResource(R.string.rouvideo_history_summary),
                icon = Icons.Filled.History,
                onClick = { navController.navigateSafely(RouVideoHistoryRoute) },
            ),
        ),
        profileExtraQuickActions = listOfNotNull(translation),
        customizeEntries = listOfNotNull(
            SiteHubEntry(
                key = "host",
                title = stringResource(R.string.drawer_host_mirror),
                subtitle = stringResource(R.string.rouvideo_host_subtitle),
                icon = Icons.Filled.Dns,
                onClick = { navController.navigateSafely(RouVideoHostSettingsRoute) },
            ),
            SiteHubEntry(
                key = "site_settings",
                title = stringResource(R.string.rouvideo_settings_title),
                subtitle = stringResource(R.string.rouvideo_settings_summary),
                icon = Icons.Filled.Tune,
                onClick = { navController.navigateSafely(RouVideoSettingsRoute) },
            ),
            translation,
            rememberPlayerSettingsEntry(navController, SiteType.ROUVIDEO),
        ),
    )
}
