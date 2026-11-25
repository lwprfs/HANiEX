package com.yenaly.han1meviewer.SitesHub.Cosplaytele.ui.post

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.exoplayer.ExoPlayer
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.common.CosplayteleImageLoader
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.component.player.SiteVideoPlayer

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CosplayteleVideoPlayer(
    state: CosplaytellePlayerState,
    exoPlayer: ExoPlayer?,
    coverUrl: String,
    isFullscreen: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkip: (Long) -> Unit,
    onToggleFullscreen: () -> Unit,
    onPositionUpdate: (Long, Long) -> Unit,
    modifier: Modifier = Modifier,
    showResumeButton: Boolean = false,
    savedPosition: Long = 0L,
    onResumeFromSaved: () -> Unit = {},
    onStartFromBeginning: () -> Unit = {},
    title: String = "",
    onBack: () -> Unit = {},
    onSpeedSelect: (Float) -> Unit = {},
    onQualitySelect: (String) -> Unit = {},
    referer: String = "",
) = SiteVideoPlayer(
    siteType = SiteType.COSPLAYTELE,
    playerTag = "CosplaytelePlayer",
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
    showResumeButton = showResumeButton,
    savedPosition = savedPosition,
    onResumeFromSaved = onResumeFromSaved,
    onStartFromBeginning = onStartFromBeginning,
    title = title,
    onBack = onBack,
    onSpeedSelect = onSpeedSelect,
    onQualitySelect = onQualitySelect,
    imageLoader = { CosplayteleImageLoader.get(it) },
    mpvReferer = referer.ifBlank { "https://cossora.stream/" },
)
