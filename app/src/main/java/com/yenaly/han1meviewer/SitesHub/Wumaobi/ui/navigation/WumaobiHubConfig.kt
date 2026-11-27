package com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Language
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.SitesHub.Wumaobi.common.WumaobiConstants
import com.yenaly.han1meviewer.SitesHub.Wumaobi.data.remote.WumaobiAuth
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.SiteHubSection
import com.yenaly.han1meviewer.ui.screen.site.gated
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig

@Composable
fun wumaobiHubConfig(navigator: Navigator, activity: MainActivity): SiteHubConfig {
    val auth by WumaobiAuth.state.collectAsState()
    val loggedIn = auth.loggedIn
    val onLogin = { navigator.navigateSafely(WumaobiAccountRoute) }

    return standardSiteHubConfig(
        site = SiteType.WUMAOBI,
        navigator = navigator,
        activity = activity,
        subscriptionSections = listOf(
            SiteHubSection(
                null,
                listOf(
                    SiteHubEntry(
                        key = "hits",
                        title = "Hottest",
                        icon = Icons.Filled.LocalFireDepartment,
                        onClick = {
                            navigator.navigateSafely(WumaobiListRoute("Hottest", "/"))
                        },
                    ),
                    SiteHubEntry(
                        key = "newest",
                        title = "Newest",
                        icon = Icons.Filled.NewReleases,
                        onClick = {
                            navigator.navigateSafely(
                                WumaobiListRoute("Newest", "/", WumaobiConstants.SECTIONS[1].sort)
                            )
                        },
                    ),
                    SiteHubEntry(
                        key = "faves",
                        title = "Most Faves",
                        icon = Icons.Filled.Star,
                        onClick = {
                            navigator.navigateSafely(
                                WumaobiListRoute("Most Faves", "/", WumaobiConstants.SECTIONS[2].sort)
                            )
                        },
                    ),
                    SiteHubEntry(
                        key = "recommend",
                        title = "Recommended",
                        icon = Icons.Filled.Recommend,
                        onClick = {
                            navigator.navigateSafely(WumaobiListRoute("Recommended", "/recommend"))
                        },
                    ),
                    SiteHubEntry(
                        key = "tags",
                        title = stringResource(R.string.wumaobi_tags),
                        icon = Icons.Filled.Category,
                        onClick = {
                            navigator.navigateSafely(WumaobiBrowseRoute(WumaobiConstants.EntityKind.TAGS.key))
                        },
                    ),
                    SiteHubEntry(
                        key = "actress",
                        title = stringResource(R.string.wumaobi_models),
                        icon = Icons.Filled.People,
                        onClick = {
                            navigator.navigateSafely(WumaobiBrowseRoute(WumaobiConstants.EntityKind.ACTRESS.key))
                        },
                    ),
                    SiteHubEntry(
                        key = "series",
                        title = stringResource(R.string.wumaobi_series),
                        icon = Icons.Filled.Language,
                        onClick = {
                            navigator.navigateSafely(WumaobiBrowseRoute(WumaobiConstants.EntityKind.SERIES.key))
                        },
                    ),
                ),
            ),
        ),
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "favorites",
                title = stringResource(R.string.wumaobi_favorites),
                subtitle = stringResource(R.string.wumaobi_favorites_summary),
                icon = Icons.Filled.Favorite,
                onClick = { navigator.navigateSafely(WumaobiFavoritesRoute) },
            ).gated(loggedIn, onLogin),
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.wumaobi_history_title),
                subtitle = stringResource(R.string.wumaobi_history_summary),
                icon = Icons.Filled.History,
                onClick = { navigator.navigateSafely(WumaobiHistoryRoute) },
            ),
        ),
        profileExtraQuickActions = listOf(
            SiteHubEntry(
                key = "account",
                title = stringResource(R.string.wumaobi_account_title),
                icon = Icons.Filled.AccountCircle,
                onClick = { navigator.navigateSafely(WumaobiAccountRoute) },
            ),
        ),
    )
}
