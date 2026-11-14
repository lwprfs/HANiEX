package com.yenaly.han1meviewer.ui.screen.main

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
import com.yenaly.han1meviewer.FapFolder.data.remote.FapFolderNetwork
import com.yenaly.han1meviewer.SupJav.ui.navigation.SupJavHistoryRoute
import com.yenaly.han1meviewer.SupJav.ui.navigation.SupJavSearchRoute
import com.yenaly.han1meviewer.SupJav.ui.navigation.SupJavSettingsRoute
import com.yenaly.han1meviewer.RouVideo.ui.navigation.RouVideoCategoriesRoute
import com.yenaly.han1meviewer.RouVideo.ui.navigation.RouVideoHistoryRoute
import com.yenaly.han1meviewer.RouVideo.ui.navigation.RouVideoHostSettingsRoute
import com.yenaly.han1meviewer.RouVideo.ui.navigation.RouVideoSearchRoute
import com.yenaly.han1meviewer.RouVideo.ui.navigation.RouVideoSettingsRoute
import com.yenaly.han1meviewer.RouVideo.ui.navigation.RouVideoTranslationSettingsRoute
import com.yenaly.han1meviewer.RouVideo.common.RouVideoTranslation
import com.yenaly.han1meviewer.XHamster.data.remote.XHamsterAuth
import com.yenaly.han1meviewer.XHamster.ui.navigation.XHamsterAccountRoute
import com.yenaly.han1meviewer.XHamster.ui.navigation.XHamsterChannelsRoute
import com.yenaly.han1meviewer.XHamster.ui.navigation.XHamsterCreatorsRoute
import com.yenaly.han1meviewer.XHamster.ui.navigation.XHamsterPhotosRoute
import com.yenaly.han1meviewer.XHamster.ui.navigation.XHamsterCategoriesRoute
import com.yenaly.han1meviewer.XHamster.ui.navigation.XHamsterCategoryRoute
import com.yenaly.han1meviewer.XHamster.ui.navigation.XHamsterHistoryRoute
import com.yenaly.han1meviewer.XHamster.ui.navigation.XHamsterLibraryRoute
import com.yenaly.han1meviewer.XHamster.ui.navigation.XHamsterStarsRoute
import com.yenaly.han1meviewer.XHamster.ui.navigation.XHamsterSearchRoute
import com.yenaly.han1meviewer.XHamster.ui.navigation.XHamsterSettingsRoute
import com.yenaly.han1meviewer.FapFolder.ui.navigation.FapFolderCookiesRoute
import com.yenaly.han1meviewer.FapFolder.ui.navigation.FapFolderLoginRoute
import com.yenaly.han1meviewer.FapFolder.ui.navigation.FapFolderSearchHistoryRoute
import com.yenaly.han1meviewer.FapFolder.ui.navigation.FapFolderSearchRoute
import com.yenaly.han1meviewer.HentaiMama.ui.navigation.HentaiMamaHomeSettingsRoute
import com.yenaly.han1meviewer.HentaiMama.ui.navigation.HentaiMamaVideoSettingsRoute
import com.yenaly.han1meviewer.MissAV.ui.navigation.MissAvHomeCategoriesRoute
import com.yenaly.han1meviewer.MissAV.ui.navigation.MissAvHostSettingsRoute
import com.yenaly.han1meviewer.MissAV.ui.navigation.MissAvQualitySettingsRoute
import com.yenaly.han1meviewer.MissAV.ui.navigation.MissAvSubtitleSettingsRoute
import com.yenaly.han1meviewer.OnAir.data.local.OnAirProfileCache
import com.yenaly.han1meviewer.OnAir.ui.navigation.OnAirAccountSettingsRoute
import com.yenaly.han1meviewer.OnAir.ui.navigation.OnAirCookieRoute
import com.yenaly.han1meviewer.Preferences
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
import androidx.navigation.NavHostController
import com.yenaly.han1meviewer.FapFolder.ui.navigation.FapFolderFollowedCommunitiesRoute
import com.yenaly.han1meviewer.FapFolder.ui.navigation.FapFolderHistoryRoute
import com.yenaly.han1meviewer.FapFolder.ui.navigation.FapFolderSavedRoute
import com.yenaly.han1meviewer.FapFolder.ui.navigation.FapFolderSettingsRoute
import com.yenaly.han1meviewer.HentaiMama.ui.navigation.HentaiMamaHistoryRoute
import com.yenaly.han1meviewer.HentaiMama.ui.navigation.HentaiMamaPlaylistsRoute
import com.yenaly.han1meviewer.HentaiMama.ui.navigation.HentaiMamaRecentEpisodesRoute
import com.yenaly.han1meviewer.HentaiMama.ui.navigation.HentaiMamaSettingsHubRoute
import com.yenaly.han1meviewer.HentaiMama.ui.navigation.HentaiMamaUpcomingRoute
import com.yenaly.han1meviewer.MissAV.ui.navigation.MissAvHistoryRoute
import com.yenaly.han1meviewer.MissAV.ui.navigation.MissAvSettingsHubRoute
import com.yenaly.han1meviewer.OnAir.ui.navigation.OnAirAnnouncementsRoute
import com.yenaly.han1meviewer.OnAir.ui.navigation.OnAirChatRoute
import com.yenaly.han1meviewer.OnAir.ui.navigation.OnAirHistoryRoute
import com.yenaly.han1meviewer.OnAir.ui.navigation.OnAirPicturesRoute
import com.yenaly.han1meviewer.OnAir.ui.navigation.OnAirSettingsRoute
import com.yenaly.han1meviewer.OnAir.ui.navigation.OnAirTagsIndexRoute
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.navigation.CrossSiteNavigator
import com.yenaly.han1meviewer.ui.navigation.NavigationManager
import com.yenaly.han1meviewer.ui.navigation.main.DailyCheckInRoute
import com.yenaly.han1meviewer.ui.navigation.main.DownloadRoute
import com.yenaly.han1meviewer.FapFolder.ui.navigation.FapFolderImageFolderRoute
import com.yenaly.han1meviewer.ui.navigation.navigateSafely

data class SiteLoginStatus(
    val title: String,
    val subtitle: String,
    val avatarUrl: String?,
    val loggedIn: Boolean,
    val onClick: ((NavHostController) -> Unit)?,
)

private const val ONAIR_COOKIES_KEY = "onair_cookies_raw"

fun siteLoginStatus(context: Context, site: SiteType): SiteLoginStatus = when (site) {
    SiteType.ONAIR -> {
        val profile = OnAirProfileCache.current()
        val raw = runCatching {
            Preferences.preferenceSp.getString(ONAIR_COOKIES_KEY, null)
        }.getOrNull().orEmpty()
        val loggedIn = profile != null || raw.contains("session=")
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

    SiteType.MISSAV -> SiteLoginStatus(
        title = context.getString(R.string.not_logged_in),
        subtitle = context.getString(R.string.login_status_unavailable, CrossSiteNavigator.siteLabel(SiteType.MISSAV)),
        avatarUrl = null,
        loggedIn = false,
        onClick = null,
    )

    SiteType.HENTAIMAMA -> SiteLoginStatus(
        title = context.getString(R.string.not_logged_in),
        subtitle = context.getString(R.string.login_status_unavailable, CrossSiteNavigator.siteLabel(SiteType.HENTAIMAMA)),
        avatarUrl = null,
        loggedIn = false,
        onClick = null,
    )

    SiteType.SUPJAV -> SiteLoginStatus(
        title = context.getString(R.string.not_logged_in),
        subtitle = context.getString(R.string.login_status_unavailable, CrossSiteNavigator.siteLabel(SiteType.SUPJAV)),
        avatarUrl = null,
        loggedIn = false,
        onClick = null,
    )

    SiteType.ROUVIDEO -> SiteLoginStatus(
        title = context.getString(R.string.not_logged_in),
        subtitle = context.getString(R.string.login_status_unavailable, CrossSiteNavigator.siteLabel(SiteType.ROUVIDEO)),
        avatarUrl = null,
        loggedIn = false,
        onClick = null,
    )

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

    SiteType.HANIME, SiteType.JAVCHU -> SiteLoginStatus(
        title = context.getString(R.string.not_logged_in),
        subtitle = "",
        avatarUrl = null,
        loggedIn = false,
        onClick = null,
    )
}
