package com.yenaly.han1meviewer.ui.screen.main

import com.yenaly.han1meviewer.MissAV.ui.navigation.MissAvSavedRoute
import com.yenaly.han1meviewer.MissAV.ui.navigation.MissAvPlaylistsRoute
import com.yenaly.han1meviewer.MissAV.ui.navigation.MissAvSiteHistoryRoute
import com.yenaly.han1meviewer.MissAV.ui.navigation.MissAvLoginRoute
import com.yenaly.han1meviewer.RouVideo.ui.library.RouVideoLibraryTab
import com.yenaly.han1meviewer.RouVideo.ui.navigation.RouVideoLibraryRoute
import com.yenaly.han1meviewer.RouVideo.ui.navigation.RouVideoAccountRoute
import com.yenaly.han1meviewer.FapFolder.ui.navigation.FapFolderAccountRoute
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
import com.yenaly.han1meviewer.FullVideos.ui.navigation.FullVideosBrowseRoute
import com.yenaly.han1meviewer.FullVideos.ui.navigation.FullVideosHistoryRoute
import com.yenaly.han1meviewer.FullVideos.ui.navigation.FullVideosSearchRoute
import com.yenaly.han1meviewer.FullVideos.ui.navigation.FullVideosSettingsRoute
import com.yenaly.han1meviewer.FuckingSession.ui.navigation.FuckingSessionHistoryRoute
import com.yenaly.han1meviewer.FuckingSession.ui.navigation.FuckingSessionListRoute
import com.yenaly.han1meviewer.FuckingSession.ui.navigation.FuckingSessionSearchRoute
import com.yenaly.han1meviewer.FuckingSession.ui.navigation.FuckingSessionSettingsRoute
import com.yenaly.han1meviewer.SupJav.ui.navigation.SupJavHistoryRoute
import com.yenaly.han1meviewer.SupJav.ui.navigation.SupJavSearchRoute
import com.yenaly.han1meviewer.SupJav.ui.navigation.SupJavSettingsRoute
import com.yenaly.han1meviewer.RouVideo.ui.navigation.RouVideoCategoriesRoute
import com.yenaly.han1meviewer.RouVideo.ui.navigation.RouVideoHistoryRoute
import com.yenaly.han1meviewer.RouVideo.ui.navigation.RouVideoHostSettingsRoute
import com.yenaly.han1meviewer.RouVideo.ui.navigation.RouVideoSearchRoute
import com.yenaly.han1meviewer.RouVideo.ui.navigation.RouVideoSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.TranslationSettingsRoute
import com.yenaly.han1meviewer.util.translation.AppTranslation
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
import com.yenaly.han1meviewer.Buondua.ui.navigation.BuonduaHistoryRoute
import com.yenaly.han1meviewer.Buondua.ui.navigation.BuonduaSearchRoute
import com.yenaly.han1meviewer.Cosplaytele.common.CosplayteleConstants
import com.yenaly.han1meviewer.Cosplaytele.ui.navigation.CosplayteleHistoryRoute
import com.yenaly.han1meviewer.Cosplaytele.ui.navigation.CosplayteleListRoute
import com.yenaly.han1meviewer.Cosplaytele.ui.navigation.CosplayteleSearchRoute
import com.yenaly.han1meviewer.Cosplaytele.ui.navigation.CosplayteleSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.ImageDownloadsRoute
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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

data class SiteDrawerEntry(
    val title: String,
    val iconRes: Int,
    val locked: Boolean = false,
    val onClick: (NavHostController) -> Unit,
)

private fun entry(title: String, icon: Int, route: Any) =
    SiteDrawerEntry(title, icon) { nav -> nav.navigateSafely(route) }

private fun gatedEntry(
    title: String,
    icon: Int,
    route: Any,
    loggedIn: Boolean,
    loginRoute: Any,
) = SiteDrawerEntry(title, icon, locked = !loggedIn) { nav ->
    nav.navigateSafely(if (loggedIn) route else loginRoute)
}

data class SiteDrawerSection(val title: String, val entries: List<SiteDrawerEntry>)

fun siteDrawerSections(context: Context, site: SiteType, loggedIn: Boolean): List<SiteDrawerSection> {
    fun str(res: Int) = context.getString(res)
    val home = SiteDrawerEntry(str(R.string.home_page), R.drawable.ic_baseline_home_24) {
        NavigationManager.navigateToHome()
    }
    val checkIn = SiteDrawerEntry(str(R.string.has_mastur), R.drawable.ic_baseline_check_circle_24) {
        it.navigateSafely(DailyCheckInRoute)
    }
    val history = R.drawable.ic_baseline_history_24
    val browse = str(R.string.site_drawer_section_browse)
    val library = str(R.string.site_drawer_section_library)
    val account = str(R.string.site_drawer_section_account)
    val customize = str(R.string.site_drawer_section_customize)
    val playerSettings = entry(
        str(R.string.player_settings), R.drawable.baseline_player_24, SitePlayerSettingsRoute(site.name),
    )

    return when (site) {
        SiteType.MISSAV -> listOf(
            SiteDrawerSection(browse, listOf(home, checkIn)),
            SiteDrawerSection(
                library,
                listOf(
                    entry(str(R.string.watch_history), history, MissAvHistoryRoute),
                    gatedEntry(str(R.string.missav_acc_saved), R.drawable.ic_drawer_bookmark, MissAvSavedRoute(0), loggedIn, MissAvLoginRoute),
                    gatedEntry(str(R.string.missav_acc_saved_actresses), R.drawable.ic_baseline_favorite_24, MissAvSavedRoute(1), loggedIn, MissAvLoginRoute),
                    gatedEntry(str(R.string.missav_acc_playlists), R.drawable.baseline_format_list_bulleted_24, MissAvPlaylistsRoute, loggedIn, MissAvLoginRoute),
                    gatedEntry(str(R.string.missav_acc_site_history), history, MissAvSiteHistoryRoute, loggedIn, MissAvLoginRoute),
                ),
            ),
            SiteDrawerSection(
                customize,
                listOf(
                    entry(str(R.string.drawer_home_categories), R.drawable.baseline_grid_24, MissAvHomeCategoriesRoute),
                    entry(str(R.string.drawer_host_mirror), R.drawable.baseline_domain_24, MissAvHostSettingsRoute),
                    entry(str(R.string.drawer_video_resolution), R.drawable.ic_video_quilty, MissAvQualitySettingsRoute),
                    entry(str(R.string.missav_subtitle_title), R.drawable.ic_baseline_language_24, MissAvSubtitleSettingsRoute),
                    playerSettings,
                ),
            ),
        )

        SiteType.HENTAIMAMA -> listOf(
            SiteDrawerSection(
                browse,
                listOf(
                    home,
                    checkIn,
                    entry(str(R.string.drawer_recent_episodes), R.drawable.ic_baseline_update_24, HentaiMamaRecentEpisodesRoute),
                    entry(str(R.string.drawer_upcoming), R.drawable.ic_baseline_access_time_24, HentaiMamaUpcomingRoute),
                ),
            ),
            SiteDrawerSection(
                library,
                listOf(
                    entry(str(R.string.watch_history), history, HentaiMamaHistoryRoute),
                    entry(str(R.string.play_list), R.drawable.baseline_format_list_bulleted_24, HentaiMamaPlaylistsRoute),
                ),
            ),
            SiteDrawerSection(
                customize,
                listOf(
                    entry(str(R.string.drawer_video_card_size), R.drawable.ic_drawer_card_size, HentaiMamaVideoSettingsRoute(target = "all")),
                    entry(str(R.string.drawer_home_categories), R.drawable.baseline_grid_24, HentaiMamaHomeSettingsRoute),
                    playerSettings,
                ),
            ),
        )

        SiteType.ONAIR -> listOf(
            SiteDrawerSection(
                browse,
                listOf(
                    home,
                    checkIn,
                    entry(str(R.string.drawer_tags), R.drawable.ic_drawer_label, OnAirTagsIndexRoute),
                    entry(str(R.string.drawer_pictures), R.drawable.ic_baseline_image_24, OnAirPicturesRoute),
                    entry(str(R.string.drawer_chat), R.drawable.baseline_forum_24, OnAirChatRoute),
                    entry(str(R.string.announcement), R.drawable.ic_baseline_newspaper_24, OnAirAnnouncementsRoute),
                ),
            ),
            SiteDrawerSection(
                library,
                listOf(entry(str(R.string.watch_history), history, OnAirHistoryRoute)),
            ),
            SiteDrawerSection(
                account,
                buildList {
                    add(entry(str(R.string.drawer_cookies_sign_in), R.drawable.ic_baseline_cookie_24, OnAirCookieRoute))
                    add(gatedEntry(str(R.string.drawer_account_session), R.drawable.ic_baseline_person_24, OnAirAccountSettingsRoute, loggedIn, OnAirCookieRoute))
                },
            ),
            SiteDrawerSection(customize, listOf(playerSettings)),
        )

        SiteType.FAPFOLDER -> listOf(
            SiteDrawerSection(
                browse,
                listOf(
                    home,
                    checkIn,
                    entry(str(R.string.search), R.drawable.ic_baseline_search_24, FapFolderSearchRoute()),
                    entry(str(R.string.drawer_search_history), R.drawable.ic_drawer_search_history, FapFolderSearchHistoryRoute),
                ),
            ),
            SiteDrawerSection(
                library,
                listOf(
                    entry(str(R.string.watch_history), history, FapFolderHistoryRoute),
                    gatedEntry(str(R.string.saved), R.drawable.ic_drawer_bookmark, FapFolderSavedRoute, loggedIn, FapFolderLoginRoute),
                    entry(str(R.string.drawer_downloaded_photos), R.drawable.ic_baseline_image_24, FapFolderImageFolderRoute),
                    gatedEntry(str(R.string.drawer_followed_communities), R.drawable.ic_drawer_groups, FapFolderFollowedCommunitiesRoute, loggedIn, FapFolderLoginRoute),
                ),
            ),
            SiteDrawerSection(
                account,
                buildList {
                    if (!loggedIn) add(entry(str(R.string.login), R.drawable.ic_baseline_unlock_24, FapFolderLoginRoute))
                    add(gatedEntry(str(R.string.site_my_account), R.drawable.ic_baseline_person_24, FapFolderAccountRoute, loggedIn, FapFolderLoginRoute))
                    add(entry(str(R.string.cookies_label), R.drawable.ic_baseline_cookie_24, FapFolderCookiesRoute))
                },
            ),
            SiteDrawerSection(customize, listOf(playerSettings)),
        )

        SiteType.SUPJAV -> listOf(
            SiteDrawerSection(
                browse,
                listOf(
                    home,
                    checkIn,
                    entry(str(R.string.search), R.drawable.ic_baseline_search_24, SupJavSearchRoute()),
                ),
            ),
            SiteDrawerSection(
                library,
                listOf(entry(str(R.string.watch_history), history, SupJavHistoryRoute)),
            ),
            SiteDrawerSection(
                customize,
                listOf(
                    entry(str(R.string.supjav_settings_playback_title), R.drawable.ic_video_quilty, SupJavSettingsRoute),
                    playerSettings,
                ),
            ),
        )

        SiteType.FULLVIDEOS -> listOf(
            SiteDrawerSection(
                browse,
                listOf(
                    home,
                    checkIn,
                    entry(str(R.string.search), R.drawable.ic_baseline_search_24, FullVideosSearchRoute()),
                    entry(str(R.string.fullvideos_categories), R.drawable.baseline_grid_24, FullVideosBrowseRoute("categories")),
                    entry(str(R.string.fullvideos_models), R.drawable.baseline_grid_24, FullVideosBrowseRoute("models")),
                    entry(str(R.string.fullvideos_sites), R.drawable.baseline_grid_24, FullVideosBrowseRoute("sites")),
                ),
            ),
            SiteDrawerSection(
                library,
                listOf(entry(str(R.string.watch_history), history, FullVideosHistoryRoute)),
            ),
            SiteDrawerSection(
                customize,
                listOf(
                    entry(str(R.string.fullvideos_settings_playback_title), R.drawable.ic_video_quilty, FullVideosSettingsRoute),
                    playerSettings,
                ),
            ),
        )

        SiteType.FUCKINGSESSION -> listOf(
            SiteDrawerSection(
                browse,
                listOf(
                    home,
                    checkIn,
                    entry(str(R.string.search), R.drawable.ic_baseline_search_24, FuckingSessionSearchRoute()),
                    entry("HD Porn", R.drawable.baseline_grid_24, FuckingSessionListRoute("HD Porn", "/category/hd-porn")),
                    entry("Anal", R.drawable.baseline_grid_24, FuckingSessionListRoute("Anal", "/category/anal")),
                    entry("MILF", R.drawable.baseline_grid_24, FuckingSessionListRoute("MILF", "/category/milf")),
                    entry("Asian Uncensored", R.drawable.baseline_grid_24, FuckingSessionListRoute("Asian Uncensored", "/category/asian-uncensored")),
                ),
            ),
            SiteDrawerSection(
                library,
                listOf(entry(str(R.string.watch_history), history, FuckingSessionHistoryRoute)),
            ),
            SiteDrawerSection(
                customize,
                listOf(
                    entry(str(R.string.fuckingsession_settings_playback_title), R.drawable.ic_video_quilty, FuckingSessionSettingsRoute),
                    playerSettings,
                ),
            ),
        )

        SiteType.BUONDUA -> listOf(
            SiteDrawerSection(
                browse,
                listOf(
                    home,
                    checkIn,
                    entry(str(R.string.search), R.drawable.ic_baseline_search_24, BuonduaSearchRoute()),
                ),
            ),
            SiteDrawerSection(
                library,
                listOf(
                    entry(str(R.string.watch_history), history, BuonduaHistoryRoute),
                    entry(str(R.string.hub_image_downloads), R.drawable.ic_baseline_image_24, ImageDownloadsRoute),
                ),
            ),
            SiteDrawerSection(
                customize,
                listOf(playerSettings),
            ),
        )

        SiteType.ROUVIDEO -> listOf(
            SiteDrawerSection(
                browse,
                listOf(
                    home,
                    checkIn,
                    entry(str(R.string.search), R.drawable.ic_baseline_search_24, RouVideoSearchRoute()),
                    entry(str(R.string.rouvideo_categories), R.drawable.ic_drawer_category, RouVideoCategoriesRoute),
                ),
            ),
            SiteDrawerSection(
                library,
                listOf(
                    entry(str(R.string.watch_history), history, RouVideoHistoryRoute),
                    gatedEntry(str(R.string.rouvideo_my_follows), R.drawable.ic_subscribtion, RouVideoLibraryRoute(RouVideoLibraryTab.FOLLOWS.name), loggedIn, RouVideoAccountRoute),
                    gatedEntry(str(R.string.rouvideo_my_favorites), R.drawable.ic_baseline_favorite_24, RouVideoLibraryRoute(RouVideoLibraryTab.FAVORITES.name), loggedIn, RouVideoAccountRoute),
                    gatedEntry(str(R.string.rouvideo_online_watch_history), history, RouVideoLibraryRoute(RouVideoLibraryTab.HISTORY.name), loggedIn, RouVideoAccountRoute),
                ),
            ),
            SiteDrawerSection(
                customize,
                listOf(
                    entry(str(R.string.drawer_host_mirror), R.drawable.baseline_domain_24, RouVideoHostSettingsRoute),
                    entry(str(R.string.rouvideo_settings_title), R.drawable.ic_video_quilty, RouVideoSettingsRoute),
                ) + listOfNotNull(
                    if (AppTranslation.available) {
                        entry(str(R.string.rouvideo_tr_title), R.drawable.ic_translate_24, TranslationSettingsRoute)
                    } else null,
                    playerSettings,
                ),
            ),
        )

        SiteType.XHAMSTER -> listOf(
            SiteDrawerSection(
                browse,
                listOf(
                    home,
                    checkIn,
                    entry(str(R.string.search), R.drawable.ic_baseline_search_24, XHamsterSearchRoute()),
                    entry(str(R.string.xhamster_categories), R.drawable.ic_drawer_category, XHamsterCategoriesRoute),
                    entry(str(R.string.xhamster_channels), R.drawable.ic_drawer_channel, XHamsterChannelsRoute),
                    entry(str(R.string.xhamster_creators), R.drawable.ic_drawer_creator, XHamsterCreatorsRoute),
                    entry(str(R.string.xhamster_photos), R.drawable.ic_drawer_photos, XHamsterPhotosRoute),
                    entry(str(R.string.xhamster_stars), R.drawable.ic_drawer_creator, XHamsterStarsRoute),
                ),
            ),
            SiteDrawerSection(
                library,
                listOf(
                    entry(str(R.string.watch_history), history, XHamsterHistoryRoute),
                    gatedEntry(str(R.string.xhamster_watch_history), history, XHamsterLibraryRoute("HISTORY"), loggedIn, XHamsterAccountRoute),
                    gatedEntry(str(R.string.xhamster_my_favorites), R.drawable.ic_baseline_favorite_24, XHamsterLibraryRoute("FAVORITES"), loggedIn, XHamsterAccountRoute),
                    gatedEntry(str(R.string.watch_later), R.drawable.ic_baseline_watch_later_24, XHamsterLibraryRoute("WATCH_LATER"), loggedIn, XHamsterAccountRoute),
                    gatedEntry(str(R.string.xhamster_liked_videos), R.drawable.ic_baseline_thumb_up_alt_24, XHamsterLibraryRoute("LIKED"), loggedIn, XHamsterAccountRoute),
                    gatedEntry(str(R.string.xhamster_playlists), R.drawable.ic_drawer_category, XHamsterLibraryRoute("PLAYLISTS"), loggedIn, XHamsterAccountRoute),
                    gatedEntry(str(R.string.xhamster_my_subscriptions), R.drawable.ic_subscribtion, XHamsterLibraryRoute("SUBSCRIPTIONS"), loggedIn, XHamsterAccountRoute),
                    entry(str(R.string.hub_image_downloads), R.drawable.ic_baseline_image_24, ImageDownloadsRoute),
                ),
            ),
            SiteDrawerSection(
                customize,
                listOf(
                    entry(str(R.string.xhamster_settings_title), R.drawable.ic_video_quilty, XHamsterSettingsRoute),
                    playerSettings,
                ),
            ),
        )

        SiteType.COSPLAYTELE -> listOf(
            SiteDrawerSection(
                browse,
                listOf(
                    home,
                    checkIn,
                    entry(str(R.string.search), R.drawable.ic_baseline_search_24, CosplayteleSearchRoute()),
                    entry(
                        str(R.string.cosplaytele_drawer_24h), history,
                        CosplayteleListRoute(CosplayteleConstants.SECTION_24H.path, CosplayteleConstants.SECTION_24H.title),
                    ),
                    entry(
                        str(R.string.cosplaytele_drawer_videos), R.drawable.ic_drawer_photos,
                        CosplayteleListRoute(CosplayteleConstants.HOME_SECTIONS[2].path, CosplayteleConstants.HOME_SECTIONS[2].title),
                    ),
                    entry(
                        str(R.string.cosplaytele_drawer_top_search), R.drawable.ic_drawer_label,
                        CosplayteleListRoute(CosplayteleConstants.SECTION_TOP_SEARCH.path, CosplayteleConstants.SECTION_TOP_SEARCH.title, true),
                    ),
                    entry(
                        str(R.string.cosplaytele_drawer_best), R.drawable.ic_drawer_creator,
                        CosplayteleListRoute(CosplayteleConstants.SECTION_BEST.path, CosplayteleConstants.SECTION_BEST.title, true),
                    ),
                    entry(
                        str(R.string.cosplaytele_drawer_explore), R.drawable.ic_drawer_category,
                        CosplayteleListRoute(CosplayteleConstants.SECTION_EXPLORE.path, CosplayteleConstants.SECTION_EXPLORE.title, true),
                    ),
                ),
            ),
            SiteDrawerSection(
                library,
                listOf(
                    entry(str(R.string.watch_history), history, CosplayteleHistoryRoute),
                    entry(str(R.string.hub_image_downloads), R.drawable.ic_baseline_image_24, ImageDownloadsRoute),
                ),
            ),
            SiteDrawerSection(
                customize,
                listOf(
                    entry(str(R.string.cosplaytele_settings_title), R.drawable.ic_video_quilty, CosplayteleSettingsRoute),
                    playerSettings,
                ),
            ),
        )

        SiteType.HANIME, SiteType.JAVCHU -> emptyList()
    }
}

private fun appEntries(context: Context): List<SiteDrawerEntry> = listOf(
    entry(context.getString(R.string.download), R.drawable.ic_baseline_download_24, DownloadRoute),
    entry(context.getString(R.string.settings), R.drawable.ic_baseline_settings_24, HomeSettingsRoute),
    entry(context.getString(R.string.player_settings), R.drawable.baseline_player_24, PlayerSettingsRoute),
    entry(context.getString(R.string.network_settings), R.drawable.baseline_mobile_data_24, NetworkSettingsRoute),
    entry(context.getString(R.string.drawer_watch_history_all), R.drawable.ic_drawer_library, UnifiedHistoryRoute),
)

@Composable
fun SiteDrawerContent(
    site: SiteType,
    navController: NavHostController,
    onClose: () -> Unit,
    onSwitchSiteClick: () -> Unit,
    refreshKey: Any = Unit,
) {
    val context = LocalContext.current
    val status = androidx.compose.runtime.remember(site, refreshKey) { siteLoginStatus(context, site) }
    val sections = siteDrawerSections(context, site, status.loggedIn) +
            SiteDrawerSection(
                context.getString(R.string.site_drawer_section_app),
                appEntries(context),
            )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SiteDrawerHeader(
            site = site,
            status = status,
            onStatusClick = status.onClick?.let { click ->
                {
                    onClose()
                    click(navController)
                }
            },
            onSwitchSiteClick = onSwitchSiteClick,
        )
        sections.filter { it.entries.isNotEmpty() }.forEach { section ->
            DrawerSectionHeader(section.title)
            section.entries.forEach { entry ->
                DrawerItem(entry) {
                    onClose()
                    entry.onClick(navController)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
internal fun DrawerSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 16.dp, top = 18.dp, bottom = 6.dp),
    )
}

@Composable
internal fun DrawerNavItem(
    title: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    locked: Boolean = false,
) {
    NavigationDrawerItem(
        label = {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        icon = {
            Icon(
                painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
        },
        badge = if (locked) {
            {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = stringResource(R.string.login_first),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else null,
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 1.dp)
            .height(52.dp),
    )
}

@Composable
private fun DrawerItem(entry: SiteDrawerEntry, onClick: () -> Unit) {
    DrawerNavItem(
        title = entry.title,
        iconRes = entry.iconRes,
        selected = false,
        onClick = onClick,
        locked = entry.locked,
    )
}

@Composable
private fun SiteDrawerHeader(
    site: SiteType,
    status: SiteLoginStatus,
    onStatusClick: (() -> Unit)?,
    onSwitchSiteClick: () -> Unit,
) {
    DrawerHeaderCard(
        avatarModel = status.avatarUrl,
        loggedIn = status.loggedIn,
        signInAvailable = status.loggedIn || status.onClick != null,
        title = status.title,
        subtitle = status.subtitle,
        badge = CrossSiteNavigator.siteLabel(site),
        onSwitchSiteClick = onSwitchSiteClick,
        onAvatarClick = onStatusClick,
        onTitleClick = onStatusClick,
    )
}
