package com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryConstants
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.SiteHubSection
import com.yenaly.han1meviewer.ui.screen.site.rememberPlayerSettingsEntry
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig

@Composable
fun leakgalleryHubConfig(navigator: Navigator, activity: MainActivity): SiteHubConfig =
    standardSiteHubConfig(
        site = SiteType.LEAKGALLERY,
        navigator = navigator,
        activity = activity,
        siteSettingsRoute = LeakGallerySettingsHubRoute,
        subscriptionSections = listOf(
            SiteHubSection(
                title = null,
                entries = listOf(
                    pageEntry(navigator, LeakGalleryConstants.SECTION_TRENDING, R.string.leakgallery_drawer_trending, R.string.leakgallery_pages_trending_desc, Icons.Filled.Whatshot),
                    pageEntry(navigator, LeakGalleryConstants.SECTION_PHOTOS, R.string.leakgallery_drawer_photos, R.string.leakgallery_pages_photos_desc, Icons.Filled.PhotoLibrary),
                    pageEntry(navigator, LeakGalleryConstants.SECTION_VIDEOS, R.string.leakgallery_drawer_videos, R.string.leakgallery_pages_videos_desc, Icons.Filled.Videocam),
                    pageEntry(navigator, LeakGalleryConstants.SECTION_MODELS, R.string.leakgallery_drawer_models, R.string.leakgallery_pages_models_desc, Icons.Filled.People),
                    pageEntry(navigator, LeakGalleryConstants.SECTION_SHORTS, R.string.leakgallery_drawer_shorts, R.string.leakgallery_pages_shorts_desc, Icons.Filled.PlayArrow),
                ),
            ),
        ),
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.leakgallery_history_title),
                subtitle = stringResource(R.string.leakgallery_history_summary),
                icon = Icons.Filled.History,
                onClick = { navigator.navigateSafely(LeakGalleryHistoryRoute) },
            ),
        ),
        customizeEntries = listOf(
            SiteHubEntry(
                key = "playback",
                title = stringResource(R.string.leakgallery_settings_title),
                subtitle = stringResource(R.string.leakgallery_settings_summary),
                icon = Icons.Filled.Tune,
                onClick = { navigator.navigateSafely(LeakGallerySettingsRoute) },
            ),
            rememberPlayerSettingsEntry(navigator, SiteType.LEAKGALLERY),
        ),
    )

@Composable
private fun pageEntry(
    navigator: Navigator,
    section: LeakGalleryConstants.Section,
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
            LeakGalleryListRoute(section.path, section.title),
        )
    },
)
