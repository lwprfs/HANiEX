package com.yenaly.han1meviewer.SitesHub.Coomer.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.SitesHub.Coomer.common.CoomerConstants
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.SiteHubSection
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig

@Composable
fun coomerHubConfig(navigator: Navigator, activity: MainActivity): SiteHubConfig =
    standardSiteHubConfig(
        site = SiteType.COOMER,
        navigator = navigator,
        activity = activity,
        subscriptionSections = listOf(
            SiteHubSection(
                title = null,
                entries = listOf(
                    pageEntry(navigator, CoomerConstants.SECTION_CREATORS, R.string.coomer_drawer_creators, R.string.coomer_pages_creators_desc, Icons.Filled.People),
                    pageEntry(navigator, CoomerConstants.SECTION_FAVORITED, R.string.coomer_drawer_favorited, R.string.coomer_pages_favorited_desc, Icons.Filled.Favorite),
                    pageEntry(navigator, CoomerConstants.SECTION_POSTS, R.string.coomer_drawer_posts, R.string.coomer_pages_posts_desc, Icons.Filled.PhotoLibrary),
                ),
            ),
        ),
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.coomer_history_title),
                subtitle = stringResource(R.string.coomer_history_summary),
                icon = Icons.Filled.History,
                onClick = { navigator.navigateSafely(CoomerHistoryRoute) },
            ),
        ),
    )

@Composable
private fun pageEntry(
    navigator: Navigator,
    section: CoomerConstants.Section,
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
            CoomerListRoute(section.path, section.title),
        )
    },
)
