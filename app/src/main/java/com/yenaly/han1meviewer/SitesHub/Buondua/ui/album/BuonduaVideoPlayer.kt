package com.yenaly.han1meviewer.SitesHub.Buondua.ui.album

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.exoplayer.ExoPlayer
import com.yenaly.han1meviewer.SitesHub.Buondua.common.BuonduaConstants
import com.yenaly.han1meviewer.SitesHub.Buondua.common.BuonduaImageLoader
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.component.player.SitePlayerState
import com.yenaly.han1meviewer.ui.component.player.SiteVideoPlayer

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun BuonduaVideoPlayer(
    state: SitePlayerState,
    exoPlayer: ExoPlayer?,
    coverUrl: String,
    isFullscreen: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkip: (Long) -> Unit,
    onToggleFullscreen: () -> Unit,
    onPositionUpdate: (Long, Long) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
    onBack: () -> Unit = {},
    onSpeedSelect: (Float) -> Unit = {},
) = SiteVideoPlayer(
    siteType = SiteType.BUONDUA,
    playerTag = "BuonduaPlayer",
    state = state,
    exoPlayer = exoPlayer,
    coverUrl = coverUrl,
    isFullscreen = isFullscreen,
    onPlayPause = onPlayPause,
    onSeek = onSeek,
    onSkip = onSkip,
    onToggleFullscreen = onToggleFullscreen,
    onPositionUpdate = onPositionUpdate,
    modifier = modifier,
    title = title,
    onBack = onBack,
    onSpeedSelect = onSpeedSelect,
    imageLoader = { BuonduaImageLoader.get(it) },
    mpvReferer = BuonduaConstants.REFERER,
)
