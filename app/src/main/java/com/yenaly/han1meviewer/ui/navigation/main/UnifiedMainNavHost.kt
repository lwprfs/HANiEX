package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import android.util.Log
import com.yenaly.han1meviewer.FapFolder.ui.navigation.FapFolderNavHost
import com.yenaly.han1meviewer.SupJav.ui.navigation.SupJavNavHost
import com.yenaly.han1meviewer.RouVideo.ui.navigation.RouVideoNavHost
import com.yenaly.han1meviewer.Buondua.ui.navigation.BuonduaNavHost
import com.yenaly.han1meviewer.Cosplaytele.ui.navigation.CosplayteleNavHost
import com.yenaly.han1meviewer.Coomer.ui.navigation.CoomerNavHost
import com.yenaly.han1meviewer.Coser.ui.navigation.CoserNavHost
import com.yenaly.han1meviewer.FullVideos.ui.navigation.FullVideosNavHost
import com.yenaly.han1meviewer.FuckingSession.ui.navigation.FuckingSessionNavHost
import com.yenaly.han1meviewer.RedGifs.ui.navigation.RedGifsNavHost
import com.yenaly.han1meviewer.XXXTube.ui.navigation.XXXTubeNavHost
import com.yenaly.han1meviewer.XHamster.ui.navigation.XHamsterNavHost
import com.yenaly.han1meviewer.HentaiMama.ui.navigation.HentaiMamaNavHost
import com.yenaly.han1meviewer.MissAV.ui.navigation.MissAvNavHost
import com.yenaly.han1meviewer.OnAir.ui.navigation.OnAirNavHost
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.NavigationState
import com.yenaly.han1meviewer.ui.navigation.Navigator

@Composable
fun UnifiedMainNavHost(
    activity: MainActivity,
    navigator: Navigator,
    navigationState: NavigationState,
    isLoggedIn: Boolean,
    avatarUrl: String?,
    username: String?,
    currentSite: String,
    onRequireLogin: () -> Unit,
    onSwitchSiteClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    siteChangeKey: Long = 0L,
    railVisible: Boolean = false,
) {
    val siteType = Preferences.siteType

    Log.d("UnifiedMainNavHost", "Current siteType: $siteType, baseUrl: ${Preferences.baseUrl}")

    key(siteChangeKey) {
        when (siteType) {
            SiteType.HANIME, SiteType.JAVCHU -> {
                MainNavHost(
                    activity = activity,
                    navigator = navigator,
                    navigationState = navigationState,
                    isLoggedIn = isLoggedIn,
                    avatarUrl = avatarUrl,
                    username = username,
                    currentSite = currentSite,
                    onRequireLogin = onRequireLogin,
                    onSwitchSiteClick = onSwitchSiteClick,
                    onLogoutClick = onLogoutClick,
                    onDestinationChanged = onDestinationChanged,
                )
            }
            SiteType.MISSAV -> {
                MissAvNavHost(
                    activity = activity,
                    navigator = navigator,
                    navigationState = navigationState,
                    onDestinationChanged = onDestinationChanged,
                )
            }
            SiteType.HENTAIMAMA -> {
                HentaiMamaNavHost(
                    activity = activity,
                    navigator = navigator,
                    navigationState = navigationState,
                    onDestinationChanged = onDestinationChanged,
                )
            }
            SiteType.ONAIR -> {
                OnAirNavHost(
                    activity = activity,
                    navigator = navigator,
                    navigationState = navigationState,
                    onDestinationChanged = onDestinationChanged,
                )
            }
            SiteType.FAPFOLDER -> {
                FapFolderNavHost(
                    activity = activity,
                    navigator = navigator,
                    navigationState = navigationState,
                    onDestinationChanged = onDestinationChanged,
                )
            }
            SiteType.SUPJAV -> {
                SupJavNavHost(
                    activity = activity,
                    navigator = navigator,
                    navigationState = navigationState,
                    onDestinationChanged = onDestinationChanged,
                )
            }
            SiteType.ROUVIDEO -> {
                RouVideoNavHost(
                    activity = activity,
                    navigator = navigator,
                    navigationState = navigationState,
                    onDestinationChanged = onDestinationChanged,
                )
            }
            SiteType.XHAMSTER -> {
                XHamsterNavHost(
                    activity = activity,
                    navigator = navigator,
                    navigationState = navigationState,
                    onDestinationChanged = onDestinationChanged,
                )
            }
            SiteType.BUONDUA -> {
                BuonduaNavHost(
                    activity = activity,
                    navigator = navigator,
                    navigationState = navigationState,
                    onDestinationChanged = onDestinationChanged,
                )
            }
            SiteType.COSPLAYTELE -> {
                CosplayteleNavHost(
                    activity = activity,
                    navigator = navigator,
                    navigationState = navigationState,
                    onDestinationChanged = onDestinationChanged,
                )
            }
            SiteType.COOMER -> {
                CoomerNavHost(
                    activity = activity,
                    navigator = navigator,
                    navigationState = navigationState,
                    onDestinationChanged = onDestinationChanged,
                )
            }
            SiteType.COSER -> {
                CoserNavHost(
                    activity = activity,
                    navigator = navigator,
                    navigationState = navigationState,
                    onDestinationChanged = onDestinationChanged,
                )
            }
            SiteType.FULLVIDEOS -> {
                FullVideosNavHost(
                    activity = activity,
                    navigator = navigator,
                    navigationState = navigationState,
                    onDestinationChanged = onDestinationChanged,
                )
            }
            SiteType.FUCKINGSESSION -> {
                FuckingSessionNavHost(
                    activity = activity,
                    navigator = navigator,
                    navigationState = navigationState,
                    onDestinationChanged = onDestinationChanged,
                )
            }
            SiteType.REDGIFS -> {
                RedGifsNavHost(
                    activity = activity,
                    navigator = navigator,
                    navigationState = navigationState,
                    onDestinationChanged = onDestinationChanged,
                )
            }
            SiteType.XXXTUBE -> {
                XXXTubeNavHost(
                    activity = activity,
                    navigator = navigator,
                    navigationState = navigationState,
                    onDestinationChanged = onDestinationChanged,
                )
            }
            SiteType.WUMAOBI -> {
                com.yenaly.han1meviewer.Wumaobi.ui.navigation.WumaobiNavHost(
                    activity = activity,
                    navigator = navigator,
                    navigationState = navigationState,
                    onDestinationChanged = onDestinationChanged,
                )
            }
        }
    }
}
