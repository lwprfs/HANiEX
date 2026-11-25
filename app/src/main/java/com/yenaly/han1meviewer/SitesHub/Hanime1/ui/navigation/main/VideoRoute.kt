package com.yenaly.han1meviewer.SitesHub.Hanime1.ui.navigation.main

import androidx.compose.runtime.Composable
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.video.VideoRouteHostScreen
import com.yenaly.han1meviewer.ui.navigation.main.VideoRoute

@Composable
fun VideoRouteScreen(
    activity: MainActivity,
    route: VideoRoute,
) {
    VideoRouteHostScreen(activity = activity, route = route)
}
