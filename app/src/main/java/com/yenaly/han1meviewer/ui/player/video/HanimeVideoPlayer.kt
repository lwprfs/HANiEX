package com.yenaly.han1meviewer.ui.player.video

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.yenaly.han1meviewer.ui.component.player.PinchZoomState
import com.yenaly.han1meviewer.ui.component.player.pinchZoomGestures
import com.yenaly.han1meviewer.ui.component.player.pinchZoomLayer
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.ui.component.GlobalDialogs
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.animation.WalkingCatAnimation
import com.yenaly.han1meviewer.ui.player.HanimePlayerController
import com.yenaly.han1meviewer.ui.player.PlayerDefaults
import com.yenaly.han1meviewer.ui.player.PlayerEvent
import com.yenaly.han1meviewer.ui.player.PlayerState
import com.yenaly.han1meviewer.ui.player.PlayerUtils
import com.yenaly.han1meviewer.ui.player.kernel.KernelType
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.milliseconds

/**
 * keepScreenOn 的最后写入者：多个播放页共享 Activity 根 View，转场期间同时
 * 组合，只有当前持有者才能把标记清零。
 */
private val KeepScreenOnOwner = AtomicReference<Any?>(null)

/**
 * Compose 视频播放器主组件。
 *
 * 取代旧版 `HJzvdStd`（JzvdStd）：渲染 Surface + 手势层 + 控件层 + 弹层全部分层组合。
 * 页面负责把 [playerSurface]（可移动 Surface）与本组件一起放置到小窗或全屏层。
 *
 * ### 分层（自底向上）
 * 1. 视频渲染层（[playerSurface]，movableContent）
 * 2. 海报层（准备中显示封面）
 * 3. 手势层（[Modifier.playerGestures]）
 * 4. 控件层（顶栏/底栏/中键/锁/倒计时/迷你进度条）
 * 5. 浮层（手势指示、从头开始、重试）
 * 6. 侧边弹窗（倍速/清晰度/超分/关键帧）
 *
 * @param controller 播放器控制器（唯一状态来源）
 * @param playerSurface 可移动的视频 Surface（[com.yenaly.han1meviewer.ui.player.surface.rememberMovablePlayerSurface] 的返回值）
 * @param isFullscreen 是否全屏（顶栏内容、锁按钮、关键帧倒计时随其切换）
 * @param keyframeEntity 关键 H 帧实体（倒计时与列表弹窗），无则 null
 * @param onBack 顶栏返回
 * @param onGoHome 回到首页（普通窗口）
 * @param onToggleFullscreen 切换全屏
 * @param videoInfoLoading 页面视频信息仍在加载（此时无可播链接，中键显示
 *   loading 而非播放键，避免点击触发「跳转浏览器」兜底）
 * @param onNoVideoUrl 无可播放链接时点击中键
 * @param onAddHKeyframeAt 长按🥵在当前时刻添加关键帧（页面校验暂停状态）
 * @param onEditKeyframe 编辑关键帧
 * @param onDeleteKeyframe 删除关键帧
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HanimeVideoPlayer(
    controller: HanimePlayerController,
    playerSurface: @Composable (Modifier) -> Unit,
    isFullscreen: Boolean,
    keyframeEntity: HKeyframeEntity?,
    modifier: Modifier = Modifier,
    videoInfoLoading: Boolean = false,
    onBack: () -> Unit,
    onGoHome: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onNoVideoUrl: () -> Unit,
    onAddHKeyframeAt: (positionMs: Long) -> Unit,
    onEditKeyframe: (HKeyframeEntity.Keyframe) -> Unit,
    onDeleteKeyframe: (HKeyframeEntity.Keyframe) -> Unit,
) {
    val view = LocalView.current
    val uiState by controller.uiState.collectAsStateWithLifecycle()
    val progress by controller.progress.collectAsStateWithLifecycle()

    val showHKeyframe = remember { Preferences.hKeyframesEnable }
    val sensitivity = remember { PlayerDefaults.toRealSensitivity(Preferences.slideSensitivity) }
    val remindAheadMs = remember { Preferences.whenCountdownRemind }
    val showCommentCountdown = remember { Preferences.showCommentWhenCountdown }
    val showBottomProgress = remember { Preferences.showBottomProgress }
    val superResLabels = remember {
        listOf(
            R.string.super_resolution_off,
            R.string.super_resolution_performance,
            R.string.super_resolution_quality,
        )
    }

    var showControls by remember { mutableStateOf(true) }

    var lockVisible by remember { mutableStateOf(true) }

    var currentSheet by remember { mutableStateOf<PlayerSheetType?>(null) }

    var bubble by remember { mutableStateOf<PlayerSheetType?>(null) }

    var bubbleAnchor by remember {
        mutableStateOf(androidx.compose.ui.geometry.Rect.Zero)
    }

    var bubblePreferAbove by remember { mutableStateOf(false) }

    var playerRootCoords by remember {
        mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null)
    }

    var gestureIndicator by remember { mutableStateOf<GestureIndicatorData?>(null) }

    var resumeVisible by remember { mutableStateOf(false) }

    var speedBoostActive by remember { mutableStateOf(false) }

    var sliderDragging by remember { mutableStateOf(false) }

    var sliderDragValue by remember { mutableFloatStateOf(0f) }

    // 播放中（含准备/缓冲）保持屏幕常亮；view 为 Activity 共享根 View，
    // 用"最后写入者"所有权避免旧页 onDispose 覆盖新页的常亮标记
    DisposableEffect(controller, uiState.state) {
        val needKeepOn = uiState.state == PlayerState.PLAYING ||
                uiState.state == PlayerState.BUFFERING ||
                uiState.state == PlayerState.PREPARING
        if (needKeepOn) {
            KeepScreenOnOwner.set(controller)
            view.keepScreenOn = true
        } else if (KeepScreenOnOwner.get() === controller) {
            KeepScreenOnOwner.set(null)
            view.keepScreenOn = false
        }
        onDispose {
            if (KeepScreenOnOwner.compareAndSet(controller, null)) {
                view.keepScreenOn = false
            }
        }
    }

    LaunchedEffect(
        showControls,
        uiState.state,
        currentSheet,
        bubble,
        sliderDragging,
        uiState.isGestureLocked,
    ) {
        if (showControls && uiState.state == PlayerState.PLAYING &&
            currentSheet == null && bubble == null && !sliderDragging && !uiState.isGestureLocked
        ) {
            delay(3_000.milliseconds)
            showControls = false
        }
    }

    LaunchedEffect(controller) {
        controller.events.collect { event ->
            when (event) {
                PlayerEvent.RequestWifiConfirmation -> {
                    GlobalDialogs.show(
                        GlobalDialogs.ConfirmRequest(
                            title = view.context.getString(R.string.warning),
                            message = view.context.getString(R.string.not_wifi_tips),
                            confirmText = view.context.getString(R.string.not_wifi_confirm),
                            dismissText = view.context.getString(R.string.cancel),
                            onConfirm = { controller.confirmWifiAndPlay() },
                        )
                    )
                }

                is PlayerEvent.ShowResumeButton -> {
                    resumeVisible = true
                }
            }
        }
    }

    LaunchedEffect(resumeVisible) {
        if (resumeVisible) {
            delay(5_000.milliseconds)
            resumeVisible = false
        }
    }

    val controlsVisible = showControls && !uiState.isGestureLocked
    // 总时长：起播后走 ticker（progress），起播前回退到预加载回填的 uiState.durationMs
    val duration = progress.durationMs.takeIf { it > 0 } ?: uiState.durationMs
    val sliderValue = if (duration > 0) {
        progress.positionMs.toFloat() / duration
    } else {
        0f
    }
    val speedLabel = if (uiState.speedIndex == PlayerDefaults.DEF_SPEED_INDEX) {
        stringResource(R.string.speed)
    } else {
        PlayerDefaults.speedStringArray[uiState.speedIndex]
    }
    val superResLabel = if (uiState.superResolutionIndex == PlayerDefaults.DEF_SUPER_RESOLUTION_INDEX) {
        stringResource(R.string.anime_4k)
    } else {
        stringResource(superResLabels[uiState.superResolutionIndex])
    }

    fun anchorRectOf(
        coords: androidx.compose.ui.layout.LayoutCoordinates,
    ): androidx.compose.ui.geometry.Rect {
        val root = playerRootCoords ?: return androidx.compose.ui.geometry.Rect.Zero
        val chip = coords.boundsInRoot()
        val rootOrigin = root.boundsInRoot().topLeft
        return androidx.compose.ui.geometry.Rect(
            chip.left - rootOrigin.x,
            chip.top - rootOrigin.y,
            chip.right - rootOrigin.x,
            chip.bottom - rootOrigin.y,
        )
    }

    fun openBubble(
        type: PlayerSheetType,
        coords: androidx.compose.ui.layout.LayoutCoordinates,
        preferAbove: Boolean,
    ) {
        bubble = type
        bubblePreferAbove = preferAbove
        bubbleAnchor = anchorRectOf(coords)
        showControls = true
    }

    val zoomState = remember { PinchZoomState() }
    val zoomEnabled = isFullscreen && Preferences.pinchZoomFullscreen
    LaunchedEffect(isFullscreen) {
        if (!isFullscreen) zoomState.reset()
    }

    Box(
        modifier = modifier
            .background(Color.Black)

            .clipToBounds()
            .onGloballyPositioned { playerRootCoords = it }
            .pinchZoomGestures(zoomState, zoomEnabled)
            .playerGestures(
                enabled = true,
                dragEnabled = !uiState.isGestureLocked && !speedBoostActive,
                seekEnabled = uiState.state != PlayerState.ERROR,
                sensitivity = sensitivity,
                durationMsProvider = { duration },
                positionMsProvider = { progress.positionMs },
                onTap = {
                    if (uiState.isGestureLocked) {
                        lockVisible = !lockVisible
                    } else {
                        showControls = !showControls
                    }
                },
                onDoubleTap = {
                    if (uiState.state == PlayerState.PLAYING ||
                        uiState.state == PlayerState.PAUSED
                    ) {
                        controller.togglePlayPause()
                    }
                },
                onLongPressStart = {
                    if (controller.isPlaying) {
                        speedBoostActive = true
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        controller.beginSpeedBoost()
                    }
                },
                onLongPressEnd = {
                    if (speedBoostActive) {
                        speedBoostActive = false
                        controller.endSpeedBoost()
                    }
                },
                onSeekTo = { controller.seekTo(it) },
                onIndicator = { gestureIndicator = it },
            ),
    ) {

        val videoRatio = if (uiState.videoWidth > 0 && uiState.videoHeight > 0) {
            uiState.videoWidth.toFloat() / uiState.videoHeight
        } else {
            0f
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (zoomEnabled) Modifier.pinchZoomLayer(zoomState) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            playerSurface(
                if (videoRatio > 0f) {
                    Modifier.aspectRatio(videoRatio, matchHeightConstraintsFirst = videoRatio < 1f)
                } else {
                    Modifier.fillMaxSize()
                }
            )
        }

        if (uiState.state == PlayerState.IDLE ||
            uiState.state == PlayerState.PREPARING ||
            uiState.state == PlayerState.ERROR
        ) {
            AsyncImage(
                model = uiState.posterUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                            )
                        )
                )
                Box(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                            )
                        )
                )
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
        ) {
            PlayerTopBar(
                isFullscreen = isFullscreen,
                title = uiState.title,
                showHKeyframeButton = showHKeyframe,
                showSuperResolution = controller.kernelType == KernelType.MpvPlayer,
                speedLabel = speedLabel,
                superResLabel = superResLabel,
                onBack = onBack,
                onGoHome = onGoHome,
                onSpeedClick = { coords ->
                    openBubble(PlayerSheetType.Speed, coords, preferAbove = false)
                },
                onKeyframeClick = {
                    showControls = false
                    currentSheet = PlayerSheetType.Keyframes
                },
                onKeyframeLongClick = { onAddHKeyframeAt(progress.positionMs) },
                onSuperResolutionClick = { coords ->
                    openBubble(PlayerSheetType.SuperResolution, coords, preferAbove = false)
                },
            )
        }

        if (isFullscreen) {
            KeyframeCountdownOverlay(
                positionMs = progress.positionMs,
                keyframes = keyframeEntity,
                remindAheadMs = remindAheadMs,
                showComment = showCommentCountdown,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 80.dp, start = 12.dp),
            )
        }

        PlayerCenterArea(
            state = if (videoInfoLoading) PlayerState.PREPARING else uiState.state,
            controlsVisible = controlsVisible,
            onPlayClick = {
                when {

                    videoInfoLoading -> Unit
                    !uiState.hasMedia -> onNoVideoUrl()
                    uiState.state == PlayerState.IDLE -> controller.startVideo()
                    else -> controller.play()
                }
            },
            onPauseClick = { controller.pause() },
            onRetry = { controller.retry() },
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = isFullscreen &&
                    (if (uiState.isGestureLocked) lockVisible else controlsVisible),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            FilledIconButton(
                onClick = {
                    val locked = !uiState.isGestureLocked
                    controller.setGestureLocked(locked)
                    if (locked) lockVisible = true
                },
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.45f)
                ),
            ) {
                Icon(
                    imageVector = if (uiState.isGestureLocked) {
                        Icons.Outlined.Lock
                    } else {
                        Icons.Outlined.LockOpen
                    },
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            PlayerBottomBar(
                currentTime = PlayerUtils.stringForTime(progress.positionMs),
                totalTime = PlayerUtils.stringForTime(duration),
                progress = sliderValue,
                buffered = progress.bufferedPercent / 100f,
                isLooping = uiState.isLooping,
                qualityLabel = uiState.qualities.getOrNull(uiState.currentQualityIndex)
                    ?: stringResource(R.string.clarity),
                isFullscreen = isFullscreen,
                onProgressChange = { fraction ->
                    sliderDragging = true
                    showControls = true
                    sliderDragValue = fraction
                },
                onProgressChangeFinished = {
                    sliderDragging = false
                    if (duration > 0) {
                        controller.seekTo((sliderDragValue * duration).toLong())
                    }
                },
                onToggleLooping = {
                    controller.setLooping(!uiState.isLooping)
                    GlobalToasts.show(
                        view.context.getString(
                            if (uiState.isLooping) R.string.loop_play_off else R.string.loop_play_on
                        ),
                        level = if (uiState.isLooping) {
                            GlobalToasts.ToastLevel.INFO
                        } else {
                            GlobalToasts.ToastLevel.SUCCESS
                        },
                    )
                },
                onQualityClick = { coords ->
                    openBubble(PlayerSheetType.Quality, coords, preferAbove = true)
                },
                onToggleFullscreen = onToggleFullscreen,
            )
        }

        if (showBottomProgress && duration > 0 &&
            uiState.state != PlayerState.IDLE
        ) {
            LinearProgressIndicator(
                progress = { sliderValue.coerceIn(0f, 1f) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp),
                trackColor = Color.White.copy(alpha = 0.15f),
            )
        }

        AnimatedVisibility(
            visible = resumeVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 92.dp),
        ) {
            ElevatedButton(
                onClick = {
                    resumeVisible = false
                    controller.restartFromBeginning()
                },
                shape = RoundedCornerShape(50),
            ) {
                Text(stringResource(R.string.start_from_beginning))
            }
        }

        GestureIndicatorOverlay(
            data = gestureIndicator,
            modifier = Modifier.fillMaxSize(),
        )

        if (bubble != null) {

            BubbleDismissScrim(
                onDismiss = { bubble = null },
                modifier = Modifier.fillMaxSize(),
            )
            val itemCount = when (bubble) {
                PlayerSheetType.Speed -> PlayerDefaults.speedArray.size
                PlayerSheetType.Quality -> uiState.qualities.size.coerceAtLeast(1)
                PlayerSheetType.SuperResolution -> 3
                else -> 1
            }
            PlayerBubblePopup(
                anchorX = bubbleAnchor.center.x,
                anchorEdgeY = if (bubblePreferAbove) bubbleAnchor.top else bubbleAnchor.bottom,
                preferAbove = bubblePreferAbove,
                playerWidthPx = playerRootCoords?.size?.width ?: 0,
                itemCount = itemCount,
                onDismiss = { bubble = null },
                modifier = Modifier.fillMaxSize(),
            ) {
                when (bubble) {
                    PlayerSheetType.Speed -> SpeedBubble(
                        currentSpeedIndex = uiState.speedIndex,
                        onSelect = { index ->
                            controller.setSpeedIndex(index)
                            bubble = null
                        },
                    )

                    PlayerSheetType.Quality -> QualityBubble(
                        qualities = uiState.qualities,
                        currentIndex = uiState.currentQualityIndex,
                        onSelect = { index ->
                            controller.switchQuality(index)
                            bubble = null
                        },
                    )

                    PlayerSheetType.SuperResolution -> SuperResolutionBubble(
                        currentIndex = uiState.superResolutionIndex,
                        onSelect = { index ->
                            controller.setSuperResolution(index)
                            bubble = null
                        },
                    )

                    else -> Unit
                }
            }
        }

        PlayerSideSheet(
            visible = currentSheet == PlayerSheetType.Keyframes,
            onDismiss = { currentSheet = null },
            modifier = Modifier.fillMaxSize(),
        ) {
            KeyframesSheet(
                keyframes = keyframeEntity,
                isLocalKeyframes = keyframeEntity?.author == null,
                onSeek = { keyframe ->
                    controller.seekTo(keyframe.position)
                    currentSheet = null
                },
                onEdit = { keyframe ->
                    currentSheet = null
                    onEditKeyframe(keyframe)
                },
                onDelete = { keyframe ->
                    currentSheet = null
                    onDeleteKeyframe(keyframe)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayerCenterArea(
    state: PlayerState,
    controlsVisible: Boolean,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (state) {

            PlayerState.PREPARING, PlayerState.BUFFERING -> {
                WalkingCatAnimation(modifier = Modifier.size(80.dp))
            }

            PlayerState.ERROR -> {
                RetryCard(onRetry = onRetry)
            }

            PlayerState.PAUSED,
            PlayerState.COMPLETED,
            PlayerState.IDLE -> {
                FilledIconButton(
                    onClick = onPlayClick,
                    modifier = Modifier.size(84.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                    )
                }
                if (state == PlayerState.COMPLETED) {
                    Text(
                        text = stringResource(R.string.player_replay),
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                    )
                }
            }

            PlayerState.PLAYING -> {
                if (controlsVisible) {
                    IconButton(onClick = onPauseClick) {
                        Icon(
                            imageVector = Icons.Outlined.Pause,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(56.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RetryCard(onRetry: () -> Unit) {
    Card(shape = RoundedCornerShape(28.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.player_video_load_failed),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.player_click_retry))
            }
        }
    }
}
