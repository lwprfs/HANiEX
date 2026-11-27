package com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryConstants
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.SiteHubSection
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGallerySession
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote.LeakGalleryAuth
import com.yenaly.han1meviewer.ui.screen.site.gated
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig

@Composable
fun leakgalleryHubConfig(navigator: Navigator, activity: MainActivity): SiteHubConfig {
    @Suppress("UNUSED_VARIABLE")
    val sessionEpoch by LeakGallerySession.epoch.collectAsState()
    val loggedIn = LeakGalleryAuth.isLoggedIn
    val onLogin = { navigator.navigateSafely(LeakGalleryLoginRoute()) }

    return standardSiteHubConfig(
        site = SiteType.LEAKGALLERY,
        navigator = navigator,
        activity = activity,
        subscriptionSections = listOf(
            SiteHubSection(
                title = null,
                entries = listOf(
                    pageEntry(navigator, LeakGalleryConstants.SECTION_TRENDING, R.string.leakgallery_drawer_trending, R.string.leakgallery_pages_trending_desc, Icons.Filled.Whatshot),
                    pageEntry(navigator, LeakGalleryConstants.SECTION_PHOTOS, R.string.leakgallery_drawer_photos, R.string.leakgallery_pages_photos_desc, Icons.Filled.PhotoLibrary),
                    pageEntry(navigator, LeakGalleryConstants.SECTION_VIDEOS, R.string.leakgallery_drawer_videos, R.string.leakgallery_pages_videos_desc, Icons.Filled.Videocam),
                    pageEntry(navigator, LeakGalleryConstants.SECTION_MODELS, R.string.leakgallery_drawer_models, R.string.leakgallery_pages_models_desc, Icons.Filled.People),
                    pageEntry(navigator, LeakGalleryConstants.SECTION_SHORTS, R.string.leakgallery_drawer_shorts, R.string.leakgallery_pages_shorts_desc, Icons.Filled.PlayArrow),
                    SiteHubEntry(
                        key = "shorts_for_you",
                        title = stringResource(R.string.leakgallery_shorts_for_you),
                        subtitle = stringResource(R.string.leakgallery_shorts_for_you_desc),
                        icon = Icons.Filled.PlayArrow,
                        onClick = { navigator.navigateSafely(LeakGalleryShortsRoute(LeakGalleryConstants.SHORTS_FOR_YOU)) },
                    ),
                    SiteHubEntry(
                        key = "shorts_followed",
                        title = stringResource(R.string.leakgallery_shorts_following),
                        subtitle = stringResource(R.string.leakgallery_shorts_following_desc),
                        icon = Icons.Filled.Groups,
                        onClick = { navigator.navigateSafely(LeakGalleryShortsRoute(LeakGalleryConstants.SHORTS_FOLLOWED)) },
                    ).gated(loggedIn, onLogin),
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
            SiteHubEntry(
                key = "liked",
                title = stringResource(R.string.leakgallery_liked_title),
                icon = Icons.Filled.Favorite,
                onClick = { navigator.navigateSafely(LeakGalleryLikedRoute) },
            ).gated(loggedIn, onLogin),
            SiteHubEntry(
                key = "following",
                title = stringResource(R.string.leakgallery_following_title),
                icon = Icons.Filled.Groups,
                onClick = { navigator.navigateSafely(LeakGalleryFollowingRoute) },
            ).gated(loggedIn, onLogin),
        ),
        profileExtraQuickActions = listOf(
            SiteHubEntry(
                key = "account",
                title = stringResource(R.string.leakgallery_account),
                subtitle = stringResource(R.string.leakgallery_account_summary),
                icon = Icons.Filled.AccountCircle,
                onClick = { navigator.navigateSafely(LeakGalleryAccountRoute) },
            ),
        ),
    )
}

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
