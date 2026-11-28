package com.yenaly.han1meviewer.ui.screen.main

import com.yenaly.han1meviewer.SitesHub.MissAV.data.remote.MissAvAuth
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.navigation.MissAvAccountRoute
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.navigation.MissAvLoginRoute
import com.yenaly.han1meviewer.SitesHub.RouVideo.data.remote.RouVideoAuth
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.navigation.RouVideoAccountRoute
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.SitesHub.FapFolder.data.remote.FapFolderNetwork
import com.yenaly.han1meviewer.SitesHub.SupJav.ui.navigation.SupJavHistoryRoute
import com.yenaly.han1meviewer.SitesHub.SupJav.ui.navigation.SupJavSearchRoute
import com.yenaly.han1meviewer.SitesHub.SupJav.ui.navigation.SupJavSettingsRoute
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.navigation.RouVideoCategoriesRoute
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.navigation.RouVideoHistoryRoute
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.navigation.RouVideoHostSettingsRoute
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.navigation.RouVideoSearchRoute
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.navigation.RouVideoSettingsRoute
import com.yenaly.han1meviewer.util.translation.AppTranslation
import com.yenaly.han1meviewer.SitesHub.XHamster.data.remote.XHamsterAuth
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.navigation.XHamsterAccountRoute
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.navigation.XHamsterChannelsRoute
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.navigation.XHamsterCreatorsRoute
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.navigation.XHamsterPhotosRoute
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.navigation.XHamsterCategoriesRoute
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.navigation.XHamsterCategoryRoute
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.navigation.XHamsterHistoryRoute
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.navigation.XHamsterLibraryRoute
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.navigation.XHamsterStarsRoute
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.navigation.XHamsterSearchRoute
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.navigation.XHamsterSettingsRoute
import com.yenaly.han1meviewer.SitesHub.FapFolder.ui.navigation.FapFolderCookiesRoute
import com.yenaly.han1meviewer.SitesHub.FapFolder.ui.navigation.FapFolderLoginRoute
import com.yenaly.han1meviewer.SitesHub.FapFolder.ui.navigation.FapFolderSearchHistoryRoute
import com.yenaly.han1meviewer.SitesHub.FapFolder.ui.navigation.FapFolderSearchRoute
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.navigation.HentaiMamaHomeSettingsRoute
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.navigation.HentaiMamaVideoSettingsRoute
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.navigation.MissAvHomeCategoriesRoute
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.navigation.MissAvHostSettingsRoute
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.navigation.MissAvQualitySettingsRoute
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.navigation.MissAvSubtitleSettingsRoute
import com.yenaly.han1meviewer.SitesHub.OnAir.data.local.OnAirProfileCache
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.local.StrawberryProfileCache
import com.yenaly.han1meviewer.SitesHub.OnAir.data.remote.OnAirCookieManager
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote.StrawberryCookieManager
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.navigation.OnAirAccountSettingsRoute
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.navigation.StrawberryAccountSettingsRoute
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.navigation.OnAirCookieRoute
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.navigation.StrawberryCookieRoute
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.NetworkSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.PlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SitePlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.UnifiedHistoryRoute
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator
import com.yenaly.han1meviewer.SitesHub.FapFolder.ui.navigation.FapFolderFollowedCommunitiesRoute
import com.yenaly.han1meviewer.SitesHub.FapFolder.ui.navigation.FapFolderHistoryRoute
import com.yenaly.han1meviewer.SitesHub.FapFolder.ui.navigation.FapFolderSavedRoute
import com.yenaly.han1meviewer.SitesHub.FapFolder.ui.navigation.FapFolderSettingsRoute
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.navigation.HentaiMamaHistoryRoute
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.navigation.HentaiMamaPlaylistsRoute
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.navigation.HentaiMamaRecentEpisodesRoute
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.navigation.HentaiMamaSettingsHubRoute
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.navigation.HentaiMamaUpcomingRoute
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.navigation.MissAvHistoryRoute
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.navigation.MissAvSettingsHubRoute
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.navigation.OnAirAnnouncementsRoute
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.navigation.StrawberryAnnouncementsRoute
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.navigation.OnAirChatRoute
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.navigation.StrawberryChatRoute
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.navigation.OnAirHistoryRoute
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.navigation.StrawberryHistoryRoute
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.navigation.OnAirPicturesRoute
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.navigation.StrawberryPicturesRoute
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.navigation.OnAirSettingsRoute
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.navigation.StrawberrySettingsRoute
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.navigation.OnAirTagsIndexRoute
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.navigation.StrawberryTagsIndexRoute
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.navigation.CrossSiteNavigator
import com.yenaly.han1meviewer.ui.navigation.NavigationManager
import com.yenaly.han1meviewer.ui.navigation.main.DailyCheckInRoute
import com.yenaly.han1meviewer.ui.navigation.main.DownloadRoute
import com.yenaly.han1meviewer.SitesHub.FapFolder.ui.navigation.FapFolderImageFolderRoute

data class SiteLoginStatus(
    val title: String,
    val subtitle: String,
    val avatarUrl: String?,
    val loggedIn: Boolean,
    val onClick: ((Navigator) -> Unit)?,
)

fun siteLoginStatus(context: Context, site: SiteType): SiteLoginStatus = when (site) {
    SiteType.ONAIR -> {
        val profile = OnAirProfileCache.current()
        val loggedIn = profile != null || OnAirCookieManager.hasSessionCookie()
        if (loggedIn) {
            SiteLoginStatus(
                title = profile?.displayName?.takeIf { it.isNotBlank() } ?: context.getString(R.string.login_status_logged_in_site, CrossSiteNavigator.siteLabel(SiteType.ONAIR)),
                subtitle = profile?.username?.let { "@$it" } ?: context.getString(R.string.login_status_tap_account),
                avatarUrl = profile?.avatarUrl,
                loggedIn = true,
                onClick = { it.navigateSafely(OnAirAccountSettingsRoute) },
            )
        } else {
            SiteLoginStatus(
                title = context.getString(R.string.not_logged_in),
                subtitle = context.getString(R.string.login_status_tap_login_cookies),
                avatarUrl = null,
                loggedIn = false,
                onClick = { it.navigateSafely(OnAirCookieRoute) },
            )
        }
    }

    SiteType.STRAWBERRY -> {
        val profile = StrawberryProfileCache.current()
        val loggedIn = profile != null || StrawberryCookieManager.hasSessionCookie()
        if (loggedIn) {
            SiteLoginStatus(
                title = profile?.displayName?.takeIf { it.isNotBlank() } ?: context.getString(R.string.login_status_logged_in_site, CrossSiteNavigator.siteLabel(SiteType.STRAWBERRY)),
                subtitle = profile?.username?.let { "@$it" } ?: context.getString(R.string.login_status_tap_account),
                avatarUrl = profile?.avatarUrl,
                loggedIn = true,
                onClick = { it.navigateSafely(StrawberryAccountSettingsRoute) },
            )
        } else {
            SiteLoginStatus(
                title = context.getString(R.string.not_logged_in),
                subtitle = context.getString(R.string.login_status_tap_login_cookies),
                avatarUrl = null,
                loggedIn = false,
                onClick = { it.navigateSafely(StrawberryCookieRoute) },
            )
        }
    }

    SiteType.FAPFOLDER -> {
        val loggedIn = runCatching { FapFolderNetwork.hasAuthCookies() }.getOrDefault(false)
        if (loggedIn) {
            SiteLoginStatus(
                title = context.getString(R.string.login_status_logged_in_site, CrossSiteNavigator.siteLabel(SiteType.FAPFOLDER)),
                subtitle = context.getString(R.string.login_status_tap_manage_cookies),
                avatarUrl = null,
                loggedIn = true,
                onClick = { it.navigateSafely(FapFolderCookiesRoute) },
            )
        } else {
            SiteLoginStatus(
                title = context.getString(R.string.not_logged_in),
                subtitle = context.getString(R.string.login_status_tap_sign_in, CrossSiteNavigator.siteLabel(SiteType.FAPFOLDER)),
                avatarUrl = null,
                loggedIn = false,
                onClick = { it.navigateSafely(FapFolderLoginRoute) },
            )
        }
    }

    SiteType.MISSAV -> {
        val user = MissAvAuth.currentUser()
        if (user != null) {
            SiteLoginStatus(
                title = user.displayName.ifBlank { context.getString(R.string.login_status_logged_in_site, CrossSiteNavigator.siteLabel(SiteType.MISSAV)) },
                subtitle = context.getString(R.string.login_status_tap_account),
                avatarUrl = null,
                loggedIn = true,
                onClick = { it.navigateSafely(MissAvAccountRoute) },
            )
        } else {
            SiteLoginStatus(
                title = context.getString(R.string.not_logged_in),
                subtitle = context.getString(R.string.login_status_tap_sign_in, CrossSiteNavigator.siteLabel(SiteType.MISSAV)),
                avatarUrl = null,
                loggedIn = false,
                onClick = { it.navigateSafely(MissAvLoginRoute) },
            )
        }
    }

    SiteType.HENTAIMAMA, SiteType.SUPJAV, SiteType.BUONDUA, SiteType.COSPLAYTELE, SiteType.COSER, SiteType.FULLVIDEOS,
    SiteType.FUCKINGSESSION, SiteType.XXXTUBE, SiteType.COOMER ->
        SiteLoginStatus(
            title = context.getString(R.string.login_status_no_account),
            subtitle = context.getString(R.string.login_status_unavailable, CrossSiteNavigator.siteLabel(site)),
            avatarUrl = null,
            loggedIn = false,
            onClick = null,
        )

    SiteType.WUMAOBI -> {
        com.yenaly.han1meviewer.SitesHub.Wumaobi.data.remote.WumaobiAuth.reload()
        val auth = com.yenaly.han1meviewer.SitesHub.Wumaobi.data.remote.WumaobiAuth.state.value
        if (auth.loggedIn) {
            SiteLoginStatus(
                title = auth.userName.ifBlank {
                    context.getString(R.string.login_status_logged_in_site, CrossSiteNavigator.siteLabel(SiteType.WUMAOBI))
                },
                subtitle = context.getString(R.string.login_status_tap_account),
                avatarUrl = null,
                loggedIn = true,
                onClick = { it.navigateSafely(com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.navigation.WumaobiAccountRoute) },
            )
        } else {
            SiteLoginStatus(
                title = context.getString(R.string.not_logged_in),
                subtitle = context.getString(R.string.login_status_tap_sign_in, CrossSiteNavigator.siteLabel(SiteType.WUMAOBI)),
                avatarUrl = null,
                loggedIn = false,
                onClick = { it.navigateSafely(com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.navigation.WumaobiAccountRoute) },
            )
        }
    }

    SiteType.ROUVIDEO -> {
        RouVideoAuth.reload()
        val auth = RouVideoAuth.state.value
        if (auth.loggedIn) {
            SiteLoginStatus(
                title = auth.user?.displayName
                    ?: context.getString(R.string.login_status_logged_in_site, CrossSiteNavigator.siteLabel(SiteType.ROUVIDEO)),
                subtitle = context.getString(R.string.login_status_tap_account),
                avatarUrl = null,
                loggedIn = true,
                onClick = { it.navigateSafely(RouVideoAccountRoute) },
            )
        } else {
            SiteLoginStatus(
                title = context.getString(R.string.not_logged_in),
                subtitle = context.getString(R.string.login_status_tap_sign_in, CrossSiteNavigator.siteLabel(SiteType.ROUVIDEO)),
                avatarUrl = null,
                loggedIn = false,
                onClick = { it.navigateSafely(RouVideoAccountRoute) },
            )
        }
    }

    SiteType.XHAMSTER -> {
        val user = if (XHamsterAuth.isLoggedIn) XHamsterAuth.cachedUser() else null
        if (user != null) {
            SiteLoginStatus(
                title = user.name,
                subtitle = context.getString(R.string.login_status_tap_account),
                avatarUrl = null,
                loggedIn = true,
                onClick = { it.navigateSafely(XHamsterAccountRoute) },
            )
        } else {
            SiteLoginStatus(
                title = context.getString(R.string.not_logged_in),
                subtitle = context.getString(R.string.login_status_tap_sign_in, CrossSiteNavigator.siteLabel(SiteType.XHAMSTER)),
                avatarUrl = null,
                loggedIn = false,
                onClick = { it.navigateSafely(XHamsterAccountRoute) },
            )
        }
    }

    SiteType.LEAKGALLERY -> {
        val auth = com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote.LeakGalleryAuth
        val route = com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.navigation.LeakGalleryAccountRoute
        if (auth.isLoggedIn) {
            SiteLoginStatus(
                title = auth.username ?: context.getString(R.string.login_status_logged_in_site, CrossSiteNavigator.siteLabel(SiteType.LEAKGALLERY)),
                subtitle = context.getString(R.string.login_status_tap_account),
                avatarUrl = auth.avatarUrl,
                loggedIn = true,
                onClick = { it.navigateSafely(route) },
            )
        } else {
            SiteLoginStatus(
                title = context.getString(R.string.not_logged_in),
                subtitle = context.getString(R.string.login_status_tap_sign_in, CrossSiteNavigator.siteLabel(SiteType.LEAKGALLERY)),
                avatarUrl = null,
                loggedIn = false,
                onClick = { it.navigateSafely(route) },
            )
        }
    }

    SiteType.REDGIFS -> {
        val name = if (com.yenaly.han1meviewer.SitesHub.RedGifs.data.remote.RedGifsAuth.isLoggedIn) {
            com.yenaly.han1meviewer.SitesHub.RedGifs.data.remote.RedGifsAuth.username
        } else null
        if (com.yenaly.han1meviewer.SitesHub.RedGifs.data.remote.RedGifsAuth.isLoggedIn) {
            SiteLoginStatus(
                title = name ?: context.getString(R.string.login_status_logged_in_site, CrossSiteNavigator.siteLabel(SiteType.REDGIFS)),
                subtitle = context.getString(R.string.login_status_tap_account),
                avatarUrl = com.yenaly.han1meviewer.SitesHub.RedGifs.data.remote.RedGifsAuth.avatarUrl,
                loggedIn = true,
                onClick = { it.navigateSafely(com.yenaly.han1meviewer.SitesHub.RedGifs.ui.navigation.RedGifsAccountRoute) },
            )
        } else {
            SiteLoginStatus(
                title = context.getString(R.string.not_logged_in),
                subtitle = context.getString(R.string.login_status_tap_sign_in, CrossSiteNavigator.siteLabel(SiteType.REDGIFS)),
                avatarUrl = null,
                loggedIn = false,
                onClick = { it.navigateSafely(com.yenaly.han1meviewer.SitesHub.RedGifs.ui.navigation.RedGifsAccountRoute) },
            )
        }
    }

    SiteType.HANIME, SiteType.JAVCHU -> SiteLoginStatus(
        title = context.getString(R.string.not_logged_in),
        subtitle = "",
        avatarUrl = null,
        loggedIn = false,
        onClick = null,
    )
}
