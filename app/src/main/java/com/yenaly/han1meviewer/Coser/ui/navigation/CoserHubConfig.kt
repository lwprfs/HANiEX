package com.yenaly.han1meviewer.Coser.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.Coser.common.CoserConstants
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.SiteHubSection
import com.yenaly.han1meviewer.ui.screen.site.rememberPlayerSettingsEntry
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig

@Composable
fun coserHubConfig(navigator: Navigator, activity: MainActivity): SiteHubConfig =
    standardSiteHubConfig(
        site = SiteType.COSER,
        navigator = navigator,
        activity = activity,
        siteSettingsRoute = CoserSettingsHubRoute,
        subscriptionSections = listOf(
            SiteHubSection(
                title = null,
                entries = listOf(
                    pageEntry(navigator, CoserConstants.SECTION_TRENDING, R.string.coser_drawer_trending, R.string.coser_pages_trending_desc, Icons.Filled.Whatshot),
                    pageEntry(navigator, CoserConstants.SECTION_LATEST, R.string.coser_drawer_latest, R.string.coser_pages_latest_desc, Icons.Filled.Today),
                    pageEntry(navigator, CoserConstants.SECTION_MOST_VIEWED, R.string.coser_drawer_most_viewed, R.string.coser_pages_most_viewed_desc, Icons.Filled.Star),
                    pageEntry(navigator, CoserConstants.SECTION_SERIES, R.string.coser_drawer_series, R.string.coser_pages_series_desc, Icons.Filled.Category),
                ),
            ),
        ),
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.coser_history_title),
                subtitle = stringResource(R.string.coser_history_summary),
                icon = Icons.Filled.History,
                onClick = { navigator.navigateSafely(CoserHistoryRoute) },
            ),
        ),
        customizeEntries = listOf(
            SiteHubEntry(
                key = "playback",
                title = stringResource(R.string.coser_settings_title),
                subtitle = stringResource(R.string.coser_settings_summary),
                icon = Icons.Filled.HighQuality,
                onClick = { navigator.navigateSafely(CoserSettingsRoute) },
            ),
            rememberPlayerSettingsEntry(navigator, SiteType.COSER),
        ),
    )

@Composable
private fun pageEntry(
    navigator: Navigator,
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
        navigator.navigateSafely(
            CoserListRoute(section.path, section.title, section.collectTerms),
        )
    },
)
