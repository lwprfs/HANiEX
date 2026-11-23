package com.yenaly.han1meviewer.Cosplaytele.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.Cosplaytele.common.CosplayteleConstants
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.SiteHubSection
import com.yenaly.han1meviewer.ui.screen.site.rememberPlayerSettingsEntry
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig

@Composable
fun cosplayteleHubConfig(navigator: Navigator, activity: MainActivity): SiteHubConfig =
    standardSiteHubConfig(
        site = SiteType.COSPLAYTELE,
        navigator = navigator,
        activity = activity,
        siteSettingsRoute = CosplayteleSettingsHubRoute,
        subscriptionSections = listOf(
            SiteHubSection(
                title = null,
                entries = listOf(
                    pageEntry(navigator, CosplayteleConstants.SECTION_24H, R.string.cosplaytele_drawer_24h, R.string.cosplaytele_pages_24h_desc, Icons.Filled.Whatshot),
                    pageEntry(navigator, CosplayteleConstants.SECTION_3D, R.string.cosplaytele_drawer_3d, R.string.cosplaytele_pages_3d_desc, Icons.Filled.Today),
                    pageEntry(navigator, CosplayteleConstants.SECTION_7D, R.string.cosplaytele_drawer_7d, R.string.cosplaytele_pages_7d_desc, Icons.Filled.DateRange),
                    pageEntry(navigator, CosplayteleConstants.SECTION_BEST, R.string.cosplaytele_drawer_best, R.string.cosplaytele_pages_best_desc, Icons.Filled.Star),
                    pageEntry(navigator, CosplayteleConstants.SECTION_TOP_SEARCH, R.string.cosplaytele_drawer_top_search, R.string.cosplaytele_pages_top_search_desc, Icons.Filled.Search),
                    pageEntry(navigator, CosplayteleConstants.SECTION_EXPLORE, R.string.cosplaytele_drawer_explore, R.string.cosplaytele_pages_explore_desc, Icons.Filled.Category),
                ),
            ),
        ),
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.cosplaytele_history_title),
                subtitle = stringResource(R.string.cosplaytele_history_summary),
                icon = Icons.Filled.History,
                onClick = { navigator.navigateSafely(CosplayteleHistoryRoute) },
            ),
        ),
        customizeEntries = listOf(
            SiteHubEntry(
                key = "playback",
                title = stringResource(R.string.cosplaytele_settings_title),
                subtitle = stringResource(R.string.cosplaytele_settings_summary),
                icon = Icons.Filled.HighQuality,
                onClick = { navigator.navigateSafely(CosplayteleSettingsRoute) },
            ),
            rememberPlayerSettingsEntry(navigator, SiteType.COSPLAYTELE),
        ),
    )

@Composable
private fun pageEntry(
    navigator: Navigator,
    section: CosplayteleConstants.Section,
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
            CosplayteleListRoute(section.path, section.title, section.collectTerms),
        )
    },
)
