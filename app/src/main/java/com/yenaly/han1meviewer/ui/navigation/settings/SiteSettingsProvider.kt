package com.yenaly.han1meviewer.ui.navigation.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.SitesHub.Coomer.ui.navigation.CoomerSettingsRoute
import com.yenaly.han1meviewer.SitesHub.Coser.ui.navigation.CoserSettingsRoute
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.ui.navigation.CosplayteleSettingsRoute
import com.yenaly.han1meviewer.SitesHub.FapFolder.data.remote.FapFolderNetwork
import com.yenaly.han1meviewer.SitesHub.FapFolder.ui.navigation.FapFolderAccountRoute
import com.yenaly.han1meviewer.SitesHub.FapFolder.ui.navigation.FapFolderCookiesRoute
import com.yenaly.han1meviewer.SitesHub.FapFolder.ui.navigation.FapFolderLoginRoute
import com.yenaly.han1meviewer.SitesHub.FapFolder.ui.navigation.FapFolderPrivacyRoute
import com.yenaly.han1meviewer.SitesHub.FapFolder.ui.navigation.FapFolderRegisterRoute
import com.yenaly.han1meviewer.SitesHub.FuckingSession.ui.navigation.FuckingSessionSettingsRoute
import com.yenaly.han1meviewer.SitesHub.FullVideos.ui.navigation.FullVideosSettingsRoute
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.navigation.HentaiMamaHomeSettingsRoute
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.navigation.HentaiMamaVideoSettingsRoute
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.navigation.LeakGalleryAccountRoute
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.navigation.LeakGallerySettingsRoute
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.navigation.MissAvAccountRoute
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.navigation.MissAvHomeCategoriesRoute
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.navigation.MissAvHostSettingsRoute
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.navigation.MissAvQualitySettingsRoute
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.navigation.MissAvSubtitleSettingsRoute
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.navigation.OnAirAccountSettingsRoute
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.navigation.OnAirCookieRoute
import com.yenaly.han1meviewer.SitesHub.RedGifs.ui.navigation.RedGifsAccountRoute
import com.yenaly.han1meviewer.SitesHub.RedGifs.ui.navigation.RedGifsSettingsRoute
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.navigation.RouVideoAccountRoute
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.navigation.RouVideoHostSettingsRoute
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.navigation.RouVideoSettingsRoute
import com.yenaly.han1meviewer.SitesHub.SupJav.ui.navigation.SupJavSettingsRoute
import com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.navigation.WumaobiAccountRoute
import com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.navigation.WumaobiSettingsRoute
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.navigation.XHamsterAccountSettingsRoute
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.navigation.XHamsterPrivacyRoute
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.navigation.XHamsterSettingsRoute
import com.yenaly.han1meviewer.SitesHub.XXXTube.ui.navigation.XXXTubeSettingsRoute
import com.yenaly.han1meviewer.ui.component.player.SitePlayerSettings
import com.yenaly.han1meviewer.ui.component.social.SiteSocialStyle
import com.yenaly.han1meviewer.ui.navigation.CrossSiteNavigator
import com.yenaly.han1meviewer.ui.navigation.isHanimeFamily
import com.yenaly.han1meviewer.ui.screen.SiteCardSettings
import com.yenaly.han1meviewer.ui.screen.SiteHomeSettings
import com.yenaly.han1meviewer.ui.screen.SiteImageCardSettings
import com.yenaly.han1meviewer.ui.screen.SiteImageSettings
import com.yenaly.han1meviewer.ui.screen.SiteLayoutSettings
import com.yenaly.han1meviewer.ui.screen.SiteSearchSettings
import com.yenaly.han1meviewer.ui.screen.SiteShortsCardSettings
import com.yenaly.han1meviewer.util.translation.AppTranslation

val LocalSiteContentAvailable = compositionLocalOf { true }

data class SiteSettingsRow(
    val key: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val summaryRes: Int?,
    val icon: ImageVector,
    val route: NavKey,
)

data class SiteSettingsItem(
    val section: SiteSettingsSection,
    @param:StringRes val titleRes: Int,
    val titleArg: String? = null,
    @param:StringRes val summaryRes: Int,
    val summaryArg: String? = null,
    val icon: ImageVector,
    val destination: NavKey?,
)

fun siteSettingsLabel(site: SiteType): String =
    if (site.isHanimeFamily()) "Hanime1 / Javchu" else CrossSiteNavigator.siteLabel(site)

fun siteMainRows(site: SiteType): List<SiteSettingsRow> {
    fun playback(titleRes: Int, summaryRes: Int, route: NavKey) =
        listOf(SiteSettingsRow("playback", titleRes, summaryRes, Icons.Default.HighQuality, route))
    return when (site) {
        SiteType.MISSAV -> listOf(
            SiteSettingsRow("home_categories", R.string.drawer_home_categories, R.string.site_home_categories_subtitle, Icons.Default.Category, MissAvHomeCategoriesRoute),
            SiteSettingsRow("host", R.string.drawer_host_mirror, R.string.missav_host_subtitle, Icons.Default.Dns, MissAvHostSettingsRoute),
            SiteSettingsRow("video_quality", R.string.missav_video_resolution, R.string.missav_choose_auto_or_a_fixed_starting, Icons.Default.HighQuality, MissAvQualitySettingsRoute),
            SiteSettingsRow("video_subtitle", R.string.missav_subtitle_title, R.string.missav_subtitle_entry_summary, Icons.Default.Subtitles, MissAvSubtitleSettingsRoute),
        )
        SiteType.HENTAIMAMA -> listOf(
            SiteSettingsRow("video", R.string.drawer_video_card_size, R.string.hm_video_card_subtitle, Icons.Default.PlayCircleOutline, HentaiMamaVideoSettingsRoute(target = "all")),
            SiteSettingsRow("home_categories", R.string.drawer_home_categories, R.string.site_home_categories_subtitle, Icons.Default.Category, HentaiMamaHomeSettingsRoute),
        )
        SiteType.ROUVIDEO -> listOf(
            SiteSettingsRow("host", R.string.drawer_host_mirror, R.string.rouvideo_host_subtitle, Icons.Default.Dns, RouVideoHostSettingsRoute),
            SiteSettingsRow("site_settings", R.string.rouvideo_settings_title, R.string.rouvideo_settings_summary, Icons.Default.Tune, RouVideoSettingsRoute),
        )
        SiteType.SUPJAV -> playback(R.string.supjav_settings_playback_title, R.string.supjav_settings_playback_summary, SupJavSettingsRoute)
        SiteType.XHAMSTER -> playback(R.string.xhamster_settings_title, R.string.xhamster_settings_summary, XHamsterSettingsRoute)
        SiteType.COSPLAYTELE -> playback(R.string.cosplaytele_settings_title, R.string.cosplaytele_settings_summary, CosplayteleSettingsRoute)
        SiteType.COSER -> playback(R.string.coser_settings_title, R.string.coser_settings_summary, CoserSettingsRoute)
        SiteType.FULLVIDEOS -> playback(R.string.fullvideos_settings_playback_title, R.string.fullvideos_settings_playback_summary, FullVideosSettingsRoute)
        SiteType.FUCKINGSESSION -> playback(R.string.fuckingsession_settings_playback_title, R.string.fuckingsession_settings_playback_summary, FuckingSessionSettingsRoute)
        SiteType.REDGIFS -> playback(R.string.redgifs_settings_title, R.string.redgifs_settings_summary, RedGifsSettingsRoute)
        SiteType.WUMAOBI -> playback(R.string.wumaobi_settings_playback_title, R.string.wumaobi_settings_playback_summary, WumaobiSettingsRoute)
        SiteType.XXXTUBE -> playback(R.string.xxxtube_settings_playback_title, R.string.xxxtube_settings_playback_summary, XXXTubeSettingsRoute)
        SiteType.COOMER -> playback(R.string.coomer_settings_title, R.string.coomer_settings_summary, CoomerSettingsRoute)
        SiteType.LEAKGALLERY -> playback(R.string.leakgallery_settings_title, R.string.leakgallery_settings_summary, LeakGallerySettingsRoute)

        SiteType.HANIME, SiteType.JAVCHU, SiteType.ONAIR, SiteType.FAPFOLDER, SiteType.BUONDUA -> emptyList()
    }
}

fun siteAccountRows(site: SiteType, contentAvailable: Boolean): List<SiteSettingsRow> {
    fun account(titleRes: Int, summaryRes: Int?, route: NavKey) =
        SiteSettingsRow("account", titleRes, summaryRes, Icons.Default.AccountCircle, route)
    return when (site) {
        SiteType.MISSAV -> listOf(account(R.string.missav_acc_account_title, R.string.login_status_tap_account, MissAvAccountRoute))
        SiteType.ROUVIDEO -> listOf(account(R.string.rouvideo_account_title, R.string.rouvideo_account_summary, RouVideoAccountRoute))
        SiteType.REDGIFS -> listOf(account(R.string.redgifs_account, R.string.redgifs_account_summary, RedGifsAccountRoute))
        SiteType.WUMAOBI -> listOf(account(R.string.wumaobi_account_title, null, WumaobiAccountRoute))
        SiteType.LEAKGALLERY -> listOf(account(R.string.leakgallery_account, R.string.leakgallery_account_summary, LeakGalleryAccountRoute))
        SiteType.XHAMSTER -> listOf(
            account(R.string.xhamster_account_settings, R.string.xhamster_account_settings_summary, XHamsterAccountSettingsRoute),
            SiteSettingsRow("privacy", R.string.site_privacy, R.string.settings_xhamster_privacy_summary, Icons.Default.Lock, XHamsterPrivacyRoute),
        )
        SiteType.ONAIR -> buildList {
            if (contentAvailable) {
                add(account(R.string.drawer_account_session, R.string.onair_account_subtitle, OnAirAccountSettingsRoute))
            }
            add(SiteSettingsRow("cookies", R.string.drawer_cookies_sign_in, R.string.onair_cookies_subtitle, Icons.Default.Cookie, OnAirCookieRoute))
        }
        SiteType.FAPFOLDER -> buildList {
            val loggedIn = contentAvailable && FapFolderNetwork.hasAuthCookies()
            add(SiteSettingsRow("cookies", R.string.site_cookies, R.string.fap_cookies_subtitle, Icons.Default.Cookie, FapFolderCookiesRoute))
            if (!loggedIn) {
                add(SiteSettingsRow("login", R.string.site_sign_in, R.string.fap_signin_subtitle, Icons.AutoMirrored.Filled.Login, FapFolderLoginRoute))
                add(SiteSettingsRow("register", R.string.site_create_account, R.string.fap_create_account_subtitle, Icons.Default.PersonAdd, FapFolderRegisterRoute))
            }
            if (contentAvailable) {
                val summary = if (loggedIn) R.string.fap_account_subtitle_in else R.string.fap_account_subtitle_out
                add(account(R.string.site_my_account, summary, FapFolderAccountRoute))
                if (loggedIn) {
                    add(SiteSettingsRow("privacy", R.string.site_privacy, R.string.fap_privacy_subtitle, Icons.Default.Lock, FapFolderPrivacyRoute))
                }
            }
        }
        else -> emptyList()
    }
}

fun hasSiteLayoutSettings(site: SiteType): Boolean =

    site.isHanimeFamily() ||
        site in SiteLayoutSettings.gridColumnSites ||
        site in SiteCardSettings.sites ||
        site in SiteShortsCardSettings.sites ||
        site in SiteImageCardSettings.sites ||
        site in SiteImageSettings.sites ||
        site in SiteHomeSettings.cacheSites ||
        SiteHomeSettings.sectionsOf(site).isNotEmpty() ||
        SiteSearchSettings.sortChoices(site) != null

private fun siteResetAction(site: SiteType): (suspend () -> Unit)? = when (site) {
    SiteType.MISSAV -> {
        {
            com.yenaly.han1meviewer.SitesHub.MissAV.ui.home.MissAvHomeCategoryRepo.resetToDefaults()
            com.yenaly.han1meviewer.Preferences.resetMissAvCustomization()
        }
    }
    SiteType.HENTAIMAMA -> {
        {
            com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.home.HentaiMamaHomeCategoryRepo.resetToDefaults()
            com.yenaly.han1meviewer.SitesHub.HentaiMama.settings.HentaiMamaCardSettings.resetAllToDefaults()
            com.yenaly.han1meviewer.SitesHub.HentaiMama.settings.HentaiMamaVideoSettings.resetToDefaults()
        }
    }
    SiteType.ROUVIDEO -> {
        { com.yenaly.han1meviewer.SitesHub.RouVideo.common.RouVideoSettings.resetToDefaults() }
    }
    SiteType.XHAMSTER -> {
        { com.yenaly.han1meviewer.SitesHub.XHamster.common.XHamsterSettings.resetToDefaults() }
    }
    SiteType.REDGIFS -> {
        { com.yenaly.han1meviewer.SitesHub.RedGifs.common.RedGifsSettings.resetToDefaults() }
    }
    SiteType.SUPJAV -> {
        { com.yenaly.han1meviewer.SitesHub.SupJav.common.SupJavSettings.resetToDefaults() }
    }
    SiteType.COOMER -> {
        { com.yenaly.han1meviewer.SitesHub.Coomer.common.CoomerSettings.resetToDefaults() }
    }
    SiteType.COSER -> {
        { com.yenaly.han1meviewer.SitesHub.Coser.common.CoserSettings.resetToDefaults() }
    }
    SiteType.COSPLAYTELE -> {
        { com.yenaly.han1meviewer.SitesHub.Cosplaytele.common.CosplayteleSettings.resetToDefaults() }
    }
    SiteType.FULLVIDEOS -> {
        { com.yenaly.han1meviewer.SitesHub.FullVideos.common.FullVideosSettings.resetToDefaults() }
    }
    SiteType.FUCKINGSESSION -> {
        { com.yenaly.han1meviewer.SitesHub.FuckingSession.common.FuckingSessionSettings.resetToDefaults() }
    }
    SiteType.LEAKGALLERY -> {
        { com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGallerySettings.resetToDefaults() }
    }
    SiteType.WUMAOBI -> {
        { com.yenaly.han1meviewer.SitesHub.Wumaobi.common.WumaobiSettings.resetToDefaults() }
    }
    SiteType.XXXTUBE -> {
        { com.yenaly.han1meviewer.SitesHub.XXXTube.common.XXXTubeSettings.resetToDefaults() }
    }
    else -> null
}

fun canResetSite(site: SiteType): Boolean =
    siteResetAction(site) != null ||
        site in SiteLayoutSettings.gridColumnSites ||
        site in SiteCardSettings.sites ||
        site in SiteShortsCardSettings.sites ||
        site in SiteImageCardSettings.sites ||
        SiteHomeSettings.sectionsOf(site).isNotEmpty() ||
        SiteSearchSettings.sortChoices(site) != null

suspend fun resetSiteSettings(site: SiteType) {
    SiteLayoutSettings.reset(site)
    siteResetAction(site)?.invoke()
}

fun buildSiteSettingsItems(site: SiteType, contentAvailable: Boolean): List<SiteSettingsItem> =
    buildList {
        val label = siteSettingsLabel(site)
        fun sectionRoute(section: SiteSettingsSection) =
            SettingsSiteSectionRoute(site.name, section.key)

        val mainRows = siteMainRows(site)
        if (site.isHanimeFamily() || mainRows.isNotEmpty()) {
            add(
                SiteSettingsItem(
                    section = SiteSettingsSection.Main,
                    titleRes = R.string.hub_site_settings_title,
                    titleArg = label,
                    summaryRes = R.string.hub_site_settings_desc,
                    icon = Icons.Default.Tune,
                    destination = mainRows.singleOrNull()?.route
                        ?: sectionRoute(SiteSettingsSection.Main),
                )
            )
        }
        val accountRows = siteAccountRows(site, contentAvailable)
        if (accountRows.isNotEmpty()) {
            add(
                SiteSettingsItem(
                    section = SiteSettingsSection.Account,
                    titleRes = R.string.settings_site_account,
                    summaryRes = R.string.settings_site_account_summary,
                    icon = Icons.Default.AccountCircle,
                    destination = accountRows.singleOrNull()?.route
                        ?: sectionRoute(SiteSettingsSection.Account),
                )
            )
        }
        if (hasSiteLayoutSettings(site)) {
            add(
                SiteSettingsItem(
                    section = SiteSettingsSection.LayoutCards,
                    titleRes = R.string.settings_site_layout_cards,
                    summaryRes = R.string.settings_site_layout_cards_summary,
                    icon = Icons.Default.GridView,
                    destination = sectionRoute(SiteSettingsSection.LayoutCards),
                )
            )
        }
        val ownPlayer = site in SitePlayerSettings.configurableSites
        if (site.isHanimeFamily() || ownPlayer) {
            val synced = !ownPlayer || SitePlayerSettings.isSynced(site)
            add(
                SiteSettingsItem(
                    section = SiteSettingsSection.Player,
                    titleRes = R.string.player_settings,
                    summaryRes = if (synced) {
                        R.string.settings_site_player_synced
                    } else {
                        R.string.site_keeps_its_own_player_settings
                    },
                    summaryArg = if (synced) null else label,
                    icon = Icons.Default.PlayCircleOutline,
                    destination = if (ownPlayer) SitePlayerSettingsRoute(site.name) else PlayerSettingsRoute,
                )
            )
        }
        add(
            SiteSettingsItem(
                section = SiteSettingsSection.Network,
                titleRes = R.string.network_settings,
                summaryRes = R.string.settings_category_network_desc,
                icon = Icons.Default.Language,
                destination = NetworkSettingsRoute,
            )
        )
        if (AppTranslation.available && site in AppTranslation.SUPPORTED_SITES) {
            add(
                SiteSettingsItem(
                    section = SiteSettingsSection.Translation,
                    titleRes = R.string.rouvideo_tr_title,
                    summaryRes = R.string.rouvideo_tr_privacy,
                    icon = Icons.Default.Translate,
                    destination = translationRouteFor(site),
                )
            )
        }
        if (SiteSocialStyle.isSupported(site)) {
            add(
                SiteSettingsItem(
                    section = SiteSettingsSection.SocialStyle,
                    titleRes = R.string.social_style_title,
                    summaryRes = R.string.social_style_subtitle,
                    icon = Icons.Default.ViewAgenda,
                    destination = SiteSocialStyleRoute(site.name),
                )
            )
        }
        if (canResetSite(site)) {
            add(
                SiteSettingsItem(
                    section = SiteSettingsSection.Reset,
                    titleRes = R.string.site_reset_title,
                    titleArg = label,
                    summaryRes = R.string.site_reset_summary,
                    icon = Icons.Default.Restore,
                    destination = null,
                )
            )
        }
    }
