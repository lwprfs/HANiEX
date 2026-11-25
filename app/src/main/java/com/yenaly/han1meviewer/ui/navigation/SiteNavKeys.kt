package com.yenaly.han1meviewer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.SitesHub.Buondua.ui.navigation.BuonduaAlbumRoute
import com.yenaly.han1meviewer.SitesHub.Buondua.ui.navigation.BuonduaHomeRoute
import com.yenaly.han1meviewer.SitesHub.Buondua.ui.navigation.BuonduaSearchRoute
import com.yenaly.han1meviewer.SitesHub.Coomer.ui.navigation.CoomerHomeRoute
import com.yenaly.han1meviewer.SitesHub.Coomer.ui.navigation.CoomerPostRoute
import com.yenaly.han1meviewer.SitesHub.Coomer.ui.navigation.CoomerSearchRoute
import com.yenaly.han1meviewer.SitesHub.Coser.ui.navigation.CoserHomeRoute
import com.yenaly.han1meviewer.SitesHub.Coser.ui.navigation.CoserPostRoute
import com.yenaly.han1meviewer.SitesHub.Coser.ui.navigation.CoserSearchRoute
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.ui.navigation.CosplayteleHomeRoute
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.ui.navigation.CosplaytelePostRoute
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.ui.navigation.CosplayteleSearchRoute
import com.yenaly.han1meviewer.SitesHub.FapFolder.ui.navigation.FapFolderHomeRoute
import com.yenaly.han1meviewer.SitesHub.FapFolder.ui.navigation.FapFolderPostRoute
import com.yenaly.han1meviewer.SitesHub.FapFolder.ui.navigation.FapFolderSearchRoute
import com.yenaly.han1meviewer.SitesHub.FuckingSession.ui.navigation.FuckingSessionHomeRoute
import com.yenaly.han1meviewer.SitesHub.FuckingSession.ui.navigation.FuckingSessionSearchRoute
import com.yenaly.han1meviewer.SitesHub.FuckingSession.ui.navigation.FuckingSessionVideoRoute
import com.yenaly.han1meviewer.SitesHub.FullVideos.ui.navigation.FullVideosHomeRoute
import com.yenaly.han1meviewer.SitesHub.FullVideos.ui.navigation.FullVideosSearchRoute
import com.yenaly.han1meviewer.SitesHub.FullVideos.ui.navigation.FullVideosVideoRoute
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.navigation.HentaiMamaHomeRoute
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.navigation.HentaiMamaSearchRoute
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.navigation.HentaiMamaVideoRoute
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.navigation.MissAvHomeRoute
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.navigation.MissAvSearchRoute
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.navigation.MissAvVideoRoute
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.navigation.OnAirHomeRoute
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.navigation.OnAirSearchRoute
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.navigation.OnAirVideoRoute
import com.yenaly.han1meviewer.SitesHub.RedGifs.ui.navigation.RedGifsGifRoute
import com.yenaly.han1meviewer.SitesHub.RedGifs.ui.navigation.RedGifsHomeRoute
import com.yenaly.han1meviewer.SitesHub.RedGifs.ui.navigation.RedGifsSearchRoute
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.navigation.RouVideoHomeRoute
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.navigation.RouVideoSearchRoute
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.navigation.RouVideoVideoRoute
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.SitesHub.SupJav.ui.navigation.SupJavHomeRoute
import com.yenaly.han1meviewer.SitesHub.SupJav.ui.navigation.SupJavSearchRoute
import com.yenaly.han1meviewer.SitesHub.SupJav.ui.navigation.SupJavVideoRoute
import com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.navigation.WumaobiHomeRoute
import com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.navigation.WumaobiSearchRoute
import com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.navigation.WumaobiVideoRoute
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.navigation.XHamsterHomeRoute
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.navigation.XHamsterSearchRoute
import com.yenaly.han1meviewer.SitesHub.XHamster.ui.navigation.XHamsterVideoRoute
import com.yenaly.han1meviewer.SitesHub.XXXTube.ui.navigation.XXXTubeHomeRoute
import com.yenaly.han1meviewer.SitesHub.XXXTube.ui.navigation.XXXTubeSearchRoute
import com.yenaly.han1meviewer.SitesHub.XXXTube.ui.navigation.XXXTubeVideoRoute
import com.yenaly.han1meviewer.ui.navigation.main.HomeRoute
import com.yenaly.han1meviewer.ui.navigation.main.SearchRoute
import com.yenaly.han1meviewer.ui.navigation.main.VideoRoute
import com.yenaly.han1meviewer.ui.navigation.site.SiteLibraryRoute
import com.yenaly.han1meviewer.ui.navigation.site.SiteProfileRoute
import com.yenaly.han1meviewer.ui.navigation.site.SiteSubscriptionRoute
import com.yenaly.han1meviewer.ui.screen.site.supportsSubscriptions

fun siteHomeKey(site: SiteType): NavKey = when (site) {
    SiteType.HANIME, SiteType.JAVCHU -> HomeRoute
    SiteType.MISSAV -> MissAvHomeRoute
    SiteType.HENTAIMAMA -> HentaiMamaHomeRoute
    SiteType.ONAIR -> OnAirHomeRoute
    SiteType.FAPFOLDER -> FapFolderHomeRoute
    SiteType.SUPJAV -> SupJavHomeRoute
    SiteType.ROUVIDEO -> RouVideoHomeRoute
    SiteType.XHAMSTER -> XHamsterHomeRoute
    SiteType.BUONDUA -> BuonduaHomeRoute
    SiteType.COSPLAYTELE -> CosplayteleHomeRoute
    SiteType.COSER -> CoserHomeRoute
    SiteType.FULLVIDEOS -> FullVideosHomeRoute
    SiteType.FUCKINGSESSION -> FuckingSessionHomeRoute
    SiteType.REDGIFS -> RedGifsHomeRoute
    SiteType.XXXTUBE -> XXXTubeHomeRoute
    SiteType.COOMER -> CoomerHomeRoute
    SiteType.WUMAOBI -> WumaobiHomeRoute
}

fun siteTopLevelKeys(site: SiteType): Set<NavKey> = buildSet {
    add(siteHomeKey(site))
    if (site.supportsSubscriptions()) add(SiteSubscriptionRoute)
    add(SiteLibraryRoute)
    add(SiteProfileRoute)
}

fun SiteType.isHanimeFamily(): Boolean = this == SiteType.HANIME || this == SiteType.JAVCHU

@Composable
fun rememberSiteNavigationState(site: SiteType): NavigationState =
    if (site.isHanimeFamily()) {
        rememberNavigationState(
            topLevelRoutes = setOf(
                HomeRoute,
                com.yenaly.han1meviewer.ui.navigation.main.SubscriptionRoute,
                com.yenaly.han1meviewer.ui.navigation.main.ContentLibraryRoute,
                com.yenaly.han1meviewer.ui.navigation.main.ProfileRoute,
            )
        )
    } else {
        rememberNavigationState(
            startRoute = siteHomeKey(site),
            topLevelRoutes = siteTopLevelKeys(site),
        )
    }

fun siteVideoKey(
    site: SiteType,
    videoCode: String,
    path: String? = null,
    resumePosition: Long = 0L,
    autoPlay: Boolean = false,
): NavKey = when (site) {
    SiteType.HANIME, SiteType.JAVCHU -> VideoRoute(videoCode)
    SiteType.MISSAV -> MissAvVideoRoute(videoCode, path ?: "/en/$videoCode")
    SiteType.HENTAIMAMA -> HentaiMamaVideoRoute(
        videoCode = videoCode,
        path = path ?: "/$videoCode",
        resumePosition = resumePosition,
    )
    SiteType.ONAIR -> OnAirVideoRoute(videoId = videoCode)
    SiteType.FAPFOLDER -> FapFolderPostRoute(slugOrId = path ?: videoCode, autoPlay = autoPlay)
    SiteType.SUPJAV -> SupJavVideoRoute(videoCode = videoCode, resumePosition = resumePosition)
    SiteType.ROUVIDEO -> RouVideoVideoRoute(videoCode = videoCode, resumePosition = resumePosition)
    SiteType.XHAMSTER -> XHamsterVideoRoute(videoCode = videoCode, resumePosition = resumePosition)
    SiteType.BUONDUA -> BuonduaAlbumRoute(videoCode = videoCode)
    SiteType.COSPLAYTELE -> CosplaytelePostRoute(slug = videoCode, resumePosition = resumePosition)
    SiteType.COSER -> CoserPostRoute(slug = videoCode, resumePosition = resumePosition)
    SiteType.FULLVIDEOS -> FullVideosVideoRoute(videoCode = videoCode, resumePosition = resumePosition)
    SiteType.FUCKINGSESSION -> FuckingSessionVideoRoute(videoCode = videoCode, resumePosition = resumePosition)
    SiteType.REDGIFS -> RedGifsGifRoute(id = videoCode)
    SiteType.XXXTUBE -> XXXTubeVideoRoute(videoCode = videoCode, resumePosition = resumePosition)
    SiteType.COOMER -> CoomerPostRoute(slug = videoCode, resumePosition = resumePosition)
    SiteType.WUMAOBI -> WumaobiVideoRoute(videoCode = videoCode, resumePosition = resumePosition)
}

fun siteSearchKey(site: SiteType, query: String?): NavKey = when (site) {
    SiteType.HANIME, SiteType.JAVCHU -> SearchRoute(query)
    SiteType.MISSAV -> MissAvSearchRoute(query)
    SiteType.HENTAIMAMA -> HentaiMamaSearchRoute(query)
    SiteType.ONAIR -> OnAirSearchRoute(query = query)
    SiteType.FAPFOLDER -> FapFolderSearchRoute(query = query)
    SiteType.SUPJAV -> SupJavSearchRoute(query = query)
    SiteType.ROUVIDEO -> RouVideoSearchRoute(query = query)
    SiteType.XHAMSTER -> XHamsterSearchRoute(query = query)
    SiteType.BUONDUA -> BuonduaSearchRoute(query = query)
    SiteType.COSPLAYTELE -> CosplayteleSearchRoute(query = query)
    SiteType.COSER -> CoserSearchRoute(query = query)
    SiteType.FULLVIDEOS -> FullVideosSearchRoute(query = query)
    SiteType.FUCKINGSESSION -> FuckingSessionSearchRoute(query = query)
    SiteType.REDGIFS -> RedGifsSearchRoute(query = query)
    SiteType.XXXTUBE -> XXXTubeSearchRoute(query = query)
    SiteType.COOMER -> CoomerSearchRoute(query = query)
    SiteType.WUMAOBI -> WumaobiSearchRoute(query = query)
}
