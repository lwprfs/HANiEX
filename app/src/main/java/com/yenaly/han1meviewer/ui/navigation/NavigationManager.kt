package com.yenaly.han1meviewer.ui.navigation

import com.yenaly.han1meviewer.util.logId

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import com.yenaly.han1meviewer.FapFolder.ui.navigation.FapFolderHomeRoute
import com.yenaly.han1meviewer.SupJav.ui.navigation.SupJavHistoryRoute
import com.yenaly.han1meviewer.SupJav.ui.navigation.SupJavHomeRoute
import com.yenaly.han1meviewer.SupJav.ui.navigation.SupJavSearchRoute
import com.yenaly.han1meviewer.SupJav.ui.navigation.SupJavVideoRoute
import com.yenaly.han1meviewer.RouVideo.ui.navigation.RouVideoHomeRoute
import com.yenaly.han1meviewer.RouVideo.ui.navigation.RouVideoSearchRoute
import com.yenaly.han1meviewer.RouVideo.ui.navigation.RouVideoVideoRoute
import com.yenaly.han1meviewer.Buondua.ui.navigation.BuonduaAlbumRoute
import com.yenaly.han1meviewer.Buondua.ui.navigation.BuonduaHomeRoute
import com.yenaly.han1meviewer.Buondua.ui.navigation.BuonduaSearchRoute
import com.yenaly.han1meviewer.Cosplaytele.ui.navigation.CosplayteleHomeRoute
import com.yenaly.han1meviewer.FullVideos.ui.navigation.FullVideosHomeRoute
import com.yenaly.han1meviewer.FullVideos.ui.navigation.FullVideosSearchRoute
import com.yenaly.han1meviewer.FullVideos.ui.navigation.FullVideosVideoRoute
import com.yenaly.han1meviewer.FuckingSession.ui.navigation.FuckingSessionHomeRoute
import com.yenaly.han1meviewer.RedGifs.ui.navigation.RedGifsGifRoute
import com.yenaly.han1meviewer.RedGifs.ui.navigation.RedGifsHomeRoute
import com.yenaly.han1meviewer.RedGifs.ui.navigation.RedGifsSearchRoute
import com.yenaly.han1meviewer.FuckingSession.ui.navigation.FuckingSessionSearchRoute
import com.yenaly.han1meviewer.FuckingSession.ui.navigation.FuckingSessionVideoRoute
import com.yenaly.han1meviewer.XXXTube.ui.navigation.XXXTubeHomeRoute
import com.yenaly.han1meviewer.XXXTube.ui.navigation.XXXTubeSearchRoute
import com.yenaly.han1meviewer.XXXTube.ui.navigation.XXXTubeVideoRoute
import com.yenaly.han1meviewer.Cosplaytele.ui.navigation.CosplayteleSearchRoute
import com.yenaly.han1meviewer.Cosplaytele.ui.navigation.CosplaytelePostRoute
import com.yenaly.han1meviewer.Coser.ui.navigation.CoserHomeRoute
import com.yenaly.han1meviewer.Coser.ui.navigation.CoserPostRoute
import com.yenaly.han1meviewer.Coser.ui.navigation.CoserSearchRoute
import com.yenaly.han1meviewer.XHamster.ui.navigation.XHamsterHomeRoute
import com.yenaly.han1meviewer.XHamster.ui.navigation.XHamsterSearchRoute
import com.yenaly.han1meviewer.XHamster.ui.navigation.XHamsterVideoRoute
import com.yenaly.han1meviewer.FapFolder.ui.navigation.FapFolderPostRoute
import com.yenaly.han1meviewer.FapFolder.ui.navigation.FapFolderSearchRoute
import com.yenaly.han1meviewer.HentaiMama.ui.navigation.HentaiMamaHomeRoute
import com.yenaly.han1meviewer.HentaiMama.ui.navigation.HentaiMamaSearchRoute
import com.yenaly.han1meviewer.HentaiMama.ui.navigation.HentaiMamaVideoRoute
import com.yenaly.han1meviewer.OnAir.ui.navigation.OnAirHomeRoute
import com.yenaly.han1meviewer.OnAir.ui.navigation.OnAirSearchRoute
import com.yenaly.han1meviewer.OnAir.ui.navigation.OnAirVideoRoute
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.navigation.main.HomeRoute
import com.yenaly.han1meviewer.ui.navigation.main.SearchRoute
import com.yenaly.han1meviewer.ui.navigation.main.VideoRoute
import com.yenaly.han1meviewer.MissAV.ui.navigation.MissAvHomeRoute
import com.yenaly.han1meviewer.MissAV.ui.navigation.MissAvSearchRoute
import com.yenaly.han1meviewer.MissAV.ui.navigation.MissAvVideoRoute

object NavigationManager {

    private const val TAG = "NavigationManager"

    private const val SWITCH_HOME_DELAY_MS = 300L
    private const val PENDING_OPEN_POLL_MS = 150L
    private const val PENDING_OPEN_MAX_ATTEMPTS = 240

    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentNavController: NavHostController? = null
    private var currentSiteType: SiteType? = null

    private var pendingToken = 0L

    fun initialize(navController: NavHostController, siteType: SiteType) {
        currentNavController = navController
        currentSiteType = siteType
        Log.d(TAG, "Initialized with site: $siteType")
    }

    fun getCurrentSiteType(): SiteType = currentSiteType ?: Preferences.siteType

    fun getNavController(): NavHostController? = currentNavController

    fun navigateToVideo(
        videoCode: String,
        path: String? = null,
        resumePosition: Long = 0L,
        autoPlay: Boolean = false,
    ) {
        currentNavController?.let { navController ->
            try {
                when (getCurrentSiteType()) {
                    SiteType.HANIME, SiteType.JAVCHU -> {
                        navController.navigate(VideoRoute(videoCode))
                    }
                    SiteType.MISSAV -> {
                        navController.navigate(MissAvVideoRoute(videoCode, path ?: "/en/$videoCode"))
                    }
                    SiteType.HENTAIMAMA -> {
                        navController.navigate(
                            HentaiMamaVideoRoute(
                                videoCode = videoCode,
                                path = path ?: "/$videoCode",
                                resumePosition = resumePosition,
                            )
                        )
                    }
                    SiteType.ONAIR -> {
                        navController.navigate(OnAirVideoRoute(videoId = videoCode))
                    }
                    SiteType.FAPFOLDER -> {
                        navController.navigate(
                            FapFolderPostRoute(slugOrId = path ?: videoCode, autoPlay = autoPlay)
                        )
                    }
                    SiteType.SUPJAV -> {
                        navController.navigate(
                            SupJavVideoRoute(videoCode = videoCode, resumePosition = resumePosition)
                        )
                    }
                    SiteType.ROUVIDEO -> {
                        navController.navigate(
                            RouVideoVideoRoute(videoCode = videoCode, resumePosition = resumePosition)
                        )
                    }
                    SiteType.XHAMSTER -> {
                        navController.navigate(
                            XHamsterVideoRoute(videoCode = videoCode, resumePosition = resumePosition)
                        )
                    }
                    SiteType.BUONDUA -> {
                        navController.navigate(BuonduaAlbumRoute(videoCode = videoCode))
                    }
                    SiteType.COSPLAYTELE -> {
                        navController.navigate(
                            CosplaytelePostRoute(slug = videoCode, resumePosition = resumePosition)
                        )
                    }
                    SiteType.COSER -> {
                        navController.navigate(
                            CoserPostRoute(slug = videoCode, resumePosition = resumePosition)
                        )
                    }
                    SiteType.FULLVIDEOS -> {
                        navController.navigate(
                            FullVideosVideoRoute(videoCode = videoCode, resumePosition = resumePosition)
                        )
                    }
                    SiteType.FUCKINGSESSION -> {
                        navController.navigate(
                            FuckingSessionVideoRoute(videoCode = videoCode, resumePosition = resumePosition)
                        )
                    }
                    SiteType.REDGIFS -> {
                        navController.navigate(RedGifsGifRoute(id = videoCode))
                    }
                    SiteType.XXXTUBE -> {
                        navController.navigate(
                            com.yenaly.han1meviewer.XXXTube.ui.navigation.XXXTubeVideoRoute(videoCode = videoCode, resumePosition = resumePosition)
                        )
                    }
                    SiteType.WUMAOBI -> {
                        navController.navigate(
                            com.yenaly.han1meviewer.Wumaobi.ui.navigation.WumaobiVideoRoute(videoCode = videoCode, resumePosition = resumePosition)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "navigateToVideo failed: ${e.message}")
            }
        }
    }

    fun navigateToSearch(query: String? = null) {
        currentNavController?.let { navController ->
            try {
                when (getCurrentSiteType()) {
                    SiteType.HANIME, SiteType.JAVCHU -> {
                        navController.navigate(SearchRoute(query))
                    }
                    SiteType.MISSAV -> {
                        navController.navigate(MissAvSearchRoute(query))
                    }
                    SiteType.HENTAIMAMA -> {
                        navController.navigate(HentaiMamaSearchRoute(query))
                    }
                    SiteType.ONAIR -> {
                        navController.navigate(OnAirSearchRoute(query = query))
                    }
                    SiteType.FAPFOLDER -> {
                        navController.navigate(FapFolderSearchRoute(query = query))
                    }
                    SiteType.SUPJAV -> {
                        navController.navigate(SupJavSearchRoute(query = query))
                    }
                    SiteType.ROUVIDEO -> {
                        navController.navigate(RouVideoSearchRoute(query = query))
                    }
                    SiteType.XHAMSTER -> {
                        navController.navigate(XHamsterSearchRoute(query = query))
                    }
                    SiteType.BUONDUA -> {
                        navController.navigate(BuonduaSearchRoute(query = query))
                    }
                    SiteType.COSPLAYTELE -> {
                        navController.navigate(CosplayteleSearchRoute(query = query))
                    }
                    SiteType.COSER -> {
                        navController.navigate(CoserSearchRoute(query = query))
                    }
                    SiteType.FULLVIDEOS -> {
                        navController.navigate(FullVideosSearchRoute(query = query))
                    }
                    SiteType.FUCKINGSESSION -> {
                        navController.navigate(FuckingSessionSearchRoute(query = query))
                    }
                    SiteType.REDGIFS -> {
                        navController.navigate(RedGifsSearchRoute(query = query))
                    }
                    SiteType.XXXTUBE -> {
                        navController.navigate(com.yenaly.han1meviewer.XXXTube.ui.navigation.XXXTubeSearchRoute(query = query))
                    }
                    SiteType.WUMAOBI -> {
                        navController.navigate(com.yenaly.han1meviewer.Wumaobi.ui.navigation.WumaobiSearchRoute(query = query))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "navigateToSearch failed: ${e.message}")
            }
        }
    }

    fun navigateToHome() {
        currentNavController?.let { navController ->
            try {
                val site = getCurrentSiteType()
                Log.d(TAG, "navigateToHome called for site: $site")

                if (isAtHome(site)) return
                when (site) {
                    SiteType.HANIME, SiteType.JAVCHU -> {
                        navController.popToRootSafely()
                        if (!isAtHome(site)) navController.navigate(HomeRoute) { launchSingleTop = true }
                    }
                    SiteType.MISSAV -> {
                        navController.popToRootSafely()
                        if (!isAtHome(site)) navController.navigate(MissAvHomeRoute) { launchSingleTop = true }
                    }
                    SiteType.HENTAIMAMA -> {
                        navController.popToRootSafely()
                        if (!isAtHome(site)) navController.navigate(HentaiMamaHomeRoute) { launchSingleTop = true }
                    }
                    SiteType.ONAIR -> {
                        navController.popToRootSafely()
                        if (!isAtHome(site)) navController.navigate(OnAirHomeRoute) { launchSingleTop = true }
                    }
                    SiteType.FAPFOLDER -> {
                        navController.popToRootSafely()
                        if (!isAtHome(site)) navController.navigate(FapFolderHomeRoute) { launchSingleTop = true }
                    }
                    SiteType.SUPJAV -> {
                        navController.popToRootSafely()
                        if (!isAtHome(site)) navController.navigate(SupJavHomeRoute) { launchSingleTop = true }
                    }
                    SiteType.ROUVIDEO -> {
                        navController.popToRootSafely()
                        if (!isAtHome(site)) navController.navigate(RouVideoHomeRoute) { launchSingleTop = true }
                    }
                    SiteType.XHAMSTER -> {
                        navController.popToRootSafely()
                        if (!isAtHome(site)) navController.navigate(XHamsterHomeRoute) { launchSingleTop = true }
                    }
                    SiteType.BUONDUA -> {
                        navController.popToRootSafely()
                        if (!isAtHome(site)) navController.navigate(BuonduaHomeRoute) { launchSingleTop = true }
                    }
                    SiteType.COSPLAYTELE -> {
                        navController.popToRootSafely()
                        if (!isAtHome(site)) navController.navigate(CosplayteleHomeRoute) { launchSingleTop = true }
                    }
                    SiteType.COSER -> {
                        navController.popToRootSafely()
                        if (!isAtHome(site)) navController.navigate(CoserHomeRoute) { launchSingleTop = true }
                    }
                    SiteType.FULLVIDEOS -> {
                        navController.popToRootSafely()
                        if (!isAtHome(site)) navController.navigate(FullVideosHomeRoute) { launchSingleTop = true }
                    }
                    SiteType.FUCKINGSESSION -> {
                        navController.popToRootSafely()
                        if (!isAtHome(site)) navController.navigate(FuckingSessionHomeRoute) { launchSingleTop = true }
                    }
                    SiteType.REDGIFS -> {
                        navController.popToRootSafely()
                        if (!isAtHome(site)) navController.navigate(RedGifsHomeRoute) { launchSingleTop = true }
                    }
                    SiteType.XXXTUBE -> {
                        navController.popToRootSafely()
                        if (!isAtHome(site)) navController.navigate(com.yenaly.han1meviewer.XXXTube.ui.navigation.XXXTubeHomeRoute) { launchSingleTop = true }
                    }
                    SiteType.WUMAOBI -> {
                        navController.popToRootSafely()
                        if (!isAtHome(site)) navController.navigate(com.yenaly.han1meviewer.Wumaobi.ui.navigation.WumaobiHomeRoute) { launchSingleTop = true }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "navigateToHome failed: ${e.message}")
                navController.popToRootSafely()
            }
        }
    }

    fun navigateBack() {
        currentNavController?.popBackStackSafely()
    }

    fun clearBackStack() {
        currentNavController?.popToRootSafely()
    }

    fun switchSite(newSiteType: SiteType, thenOpen: PendingVideoOpen? = null) {
        Log.d(TAG, "switchSite called: $newSiteType (pending open: ${thenOpen?.videoCode.logId()})")
        currentSiteType = newSiteType
        Preferences.siteType = newSiteType
        pendingToken++
        currentNavController?.popToRootSafely()
        mainHandler.postDelayed({
            navigateToHome()
            if (thenOpen != null) schedulePendingOpen(thenOpen)
        }, SWITCH_HOME_DELAY_MS)
    }

    private fun schedulePendingOpen(request: PendingVideoOpen) {
        val token = ++pendingToken
        var attempts = 0
        val poll = object : Runnable {
            override fun run() {
                if (token != pendingToken) return
                if (getCurrentSiteType() != request.site) {
                    Log.d(TAG, "Pending open dropped: site changed again")
                    return
                }
                if (isAtHome(request.site) && isNavigationReady()) {
                    pendingToken++
                    navigateToVideo(
                        request.videoCode,
                        request.path,
                        request.resumePosition,
                        request.autoPlay,
                    )
                    return
                }
                if (++attempts >= PENDING_OPEN_MAX_ATTEMPTS) {
                    Log.w(TAG, "Pending open timed out for ${request.videoCode.logId()}")
                    return
                }
                mainHandler.postDelayed(this, PENDING_OPEN_POLL_MS)
            }
        }
        mainHandler.postDelayed(poll, PENDING_OPEN_POLL_MS)
    }

    private fun isNavigationReady(): Boolean {
        val entry = currentNavController?.currentBackStackEntry ?: return false
        return entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }

    private fun isAtHome(site: SiteType): Boolean {
        val destination = currentNavController?.currentDestination ?: return false
        return try {
            when (site) {
                SiteType.HANIME, SiteType.JAVCHU -> destination.hasRoute(HomeRoute::class)
                SiteType.MISSAV -> destination.hasRoute(MissAvHomeRoute::class)
                SiteType.HENTAIMAMA -> destination.hasRoute(HentaiMamaHomeRoute::class)
                SiteType.ONAIR -> destination.hasRoute(OnAirHomeRoute::class)
                SiteType.FAPFOLDER -> destination.hasRoute(FapFolderHomeRoute::class)
                SiteType.SUPJAV -> destination.hasRoute(SupJavHomeRoute::class)
                SiteType.ROUVIDEO -> destination.hasRoute(RouVideoHomeRoute::class)
                SiteType.XHAMSTER -> destination.hasRoute(XHamsterHomeRoute::class)
                SiteType.BUONDUA -> destination.hasRoute(BuonduaHomeRoute::class)
                SiteType.COSPLAYTELE -> destination.hasRoute(CosplayteleHomeRoute::class)
                SiteType.COSER -> destination.hasRoute(CoserHomeRoute::class)
                SiteType.FULLVIDEOS -> destination.hasRoute(FullVideosHomeRoute::class)
                SiteType.FUCKINGSESSION -> destination.hasRoute(FuckingSessionHomeRoute::class)
                SiteType.REDGIFS -> destination.hasRoute(RedGifsHomeRoute::class)
                SiteType.XXXTUBE -> destination.hasRoute(com.yenaly.han1meviewer.XXXTube.ui.navigation.XXXTubeHomeRoute::class)
                SiteType.WUMAOBI -> destination.hasRoute(com.yenaly.han1meviewer.Wumaobi.ui.navigation.WumaobiHomeRoute::class)
            }
        } catch (e: Exception) {
            false
        }
    }
}
