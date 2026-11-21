package com.yenaly.han1meviewer.Coser.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.yenaly.han1meviewer.Coser.common.CoserConstants
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
fun coserHubConfig(navController: NavHostController, activity: MainActivity): SiteHubConfig =
    standardSiteHubConfig(
        site = SiteType.COSER,
        navController = navController,
        activity = activity,
        siteSettingsRoute = CoserSettingsHubRoute,
        subscriptionSections = listOf(
            SiteHubSection(
                title = null,
                entries = listOf(
                    pageEntry(navController, CoserConstants.SECTION_TRENDING, R.string.coser_drawer_trending, R.string.coser_pages_trending_desc, Icons.Filled.Whatshot),
                    pageEntry(navController, CoserConstants.SECTION_LATEST, R.string.coser_drawer_latest, R.string.coser_pages_latest_desc, Icons.Filled.Today),
                    pageEntry(navController, CoserConstants.SECTION_MOST_VIEWED, R.string.coser_drawer_most_viewed, R.string.coser_pages_most_viewed_desc, Icons.Filled.Star),
                    pageEntry(navController, CoserConstants.SECTION_SERIES, R.string.coser_drawer_series, R.string.coser_pages_series_desc, Icons.Filled.Category),
                ),
            ),
        ),
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.coser_history_title),
                subtitle = stringResource(R.string.coser_history_summary),
                icon = Icons.Filled.History,
                onClick = { navController.navigateSafely(CoserHistoryRoute) },
            ),
        ),
        customizeEntries = listOf(
            SiteHubEntry(
                key = "playback",
                title = stringResource(R.string.coser_settings_title),
                subtitle = stringResource(R.string.coser_settings_summary),
                icon = Icons.Filled.HighQuality,
                onClick = { navController.navigateSafely(CoserSettingsRoute) },
            ),
            rememberPlayerSettingsEntry(navController, SiteType.COSER),
        ),
    )

@Composable
private fun pageEntry(
    navController: NavHostController,
    section: CoserConstants.Section,
    @androidx.annotation.StringRes titleRes: Int,
    @androidx.annotation.StringRes descRes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
): SiteHubEntry = SiteHubEntry(
    key = section.key,
    title = stringResource(titleRes),
    subtitle = stringResource(descRes),
    icon = icon,
    onClick = {
        navController.navigateSafely(
            CoserListRoute(section.path, section.title, section.collectTerms),
        )
    },
)
