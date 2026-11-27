package com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.video

import com.yenaly.han1meviewer.util.applicationContext
import com.yenaly.han1meviewer.util.PlayerOrientation
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.graphics.drawable.Icon
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.yenaly.han1meviewer.FirebaseConstants
import com.yenaly.han1meviewer.PermissionRequester
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.logic.SiteCheckIn
import com.yenaly.han1meviewer.getHanimeVideoLink
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.dao.CheckInRecordDatabase
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.logic.entity.WatchHistoryEntity
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.util.DownloadRepair
import com.yenaly.han1meviewer.util.offline.DownloadedQueue
import com.yenaly.han1meviewer.util.offline.LocalWatchRecorder
import com.yenaly.han1meviewer.logic.dao.DownloadDatabase
import com.yenaly.han1meviewer.logic.exception.ParseException
import com.yenaly.han1meviewer.SitesHub.Hanime1.logic.model.HanimeVideo
import com.yenaly.han1meviewer.logic.state.VideoLoadingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.bridge.VideoPageHost
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.GlobalDialogs
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.player.PipSupport
import com.yenaly.han1meviewer.ui.component.TextInputField
import com.yenaly.han1meviewer.ui.navigation.main.HomeRoute
import com.yenaly.han1meviewer.ui.navigation.main.VideoRoute
import com.yenaly.han1meviewer.ui.player.HanimePlayerController
import com.yenaly.han1meviewer.ui.player.PlayerState
import com.yenaly.han1meviewer.ui.player.kernel.KernelType
import com.yenaly.han1meviewer.ui.player.surface.rememberMovablePlayerSurface
import com.yenaly.han1meviewer.ui.player.video.HanimeVideoPlayer
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.viewmodel.CommentViewModel
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.viewmodel.VideoViewModel
import com.yenaly.han1meviewer.util.OrientationManager
import com.yenaly.han1meviewer.util.OrientationManager.ScreenOrientation
import com.yenaly.han1meviewer.util.appScreenHeight
import com.yenaly.han1meviewer.util.appScreenWidth
import com.yenaly.han1meviewer.util.browse
import com.yenaly.han1meviewer.util.checkBadGuy
import com.yenaly.han1meviewer.util.copyToClipboard
import com.yenaly.han1meviewer.util.loadAssetAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import com.yenaly.han1meviewer.util.dp as dpToPx

@OptIn(ExperimentalTime::class)
@Composable
fun VideoRouteHostScreen(
    activity: MainActivity,
    route: VideoRoute,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val viewModel: VideoViewModel = viewModel()
    val commentViewModel: CommentViewModel = viewModel()
    val hostUiState by viewModel.videoHostUiStateFlow.collectAsStateWithLifecycle()
    val relatedItems =
        viewModel.hanimeVideoFlow.collectAsStateWithLifecycle().value?.relatedHanimes.orEmpty()

    val genres by produceState(
        initialValue = emptyList(),
        key1 = Preferences.baseUrl,
    ) {

        value = withContext(Dispatchers.IO) {
            loadAssetAs<List<com.yenaly.han1meviewer.SitesHub.Hanime1.logic.model.SearchOption>>(
                if (Preferences.baseUrl == com.yenaly.han1meviewer.HanimeConstants.HANIME_URL[3]) {
                    "search_options/genre_av.json"
                } else {
                    "search_options/genre.json"
                }
            ).orEmpty()
        }
    }
    val stringLongPressShare = remember(activity) {
        activity.getString(R.string.long_press_share_to_copy)
    }
    val useLargeVideoLayout = currentWindowAdaptiveInfoV2()
        .windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    commentViewModel.code = route.videoCode
    viewModel.fromDownload = route.videoCode == "-1" || route.localUri != null

    var checkedQuality by remember(route.videoCode, route.localUri) {
        mutableStateOf<String?>(null)
    }
    var pendingDownloadPrompt by remember(route.videoCode, route.localUri) {
        mutableStateOf<DownloadPromptState?>(null)
    }
    var videoTitle by remember(route.videoCode, route.localUri) { mutableStateOf<String?>(null) }
    var showAddHKeyframeDialog by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var unsubscribeArtist by remember { mutableStateOf<HanimeVideo.Artist?>(null) }
    var showDownloadPermissionDialog by remember { mutableStateOf(false) }

    val kernelType = remember(route.videoCode, route.localUri) {
        val preferred = KernelType.fromString(Preferences.switchPlayerKernel)

        if (SiteCheckIn.siteOfDishCode(route.videoCode) == SiteType.HENTAIMAMA ||
            com.yenaly.han1meviewer.ui.component.player.StreamFallback.shouldUseMpv(route.localUri)
        ) {
            KernelType.MpvPlayer
        } else preferred
    }
    val controller = remember(route.videoCode, route.localUri) {
        HanimePlayerController(activity, kernelType)
    }
    val playerUiState by controller.uiState.collectAsStateWithLifecycle()

    val isBackStackTop = if (activity.isNavigatorReady) {
        activity.navigator.state.currentKey == route
    } else {
        true
    }
    LaunchedEffect(isBackStackTop, playerUiState.state) {
        if (!isBackStackTop && playerUiState.state == PlayerState.PLAYING) {
            controller.pause()
        }
    }

    DisposableEffect(controller) {
        onDispose { controller.release() }
    }

    var keyframeEntity by remember(route.videoCode) { mutableStateOf<HKeyframeEntity?>(null) }

    val playerSurface = rememberMovablePlayerSurface(controller)

    var isFullscreen by remember { mutableStateOf(false) }

    val isInPipMode = hostUiState.isInPipMode

    var playerWindowBounds by remember {
        mutableStateOf(androidx.compose.ui.geometry.Rect(androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Offset.Zero))
    }

    var brightnessBackup by remember { mutableFloatStateOf(-1f) }

    var lastAutoFullscreenTime by remember { mutableLongStateOf(0L) }

    val fullscreenAnim = remember { Animatable(0f) }

    var fullscreenOverlayVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isFullscreen, isInPipMode) {
        when {
            isInPipMode -> {
                fullscreenOverlayVisible = false
                fullscreenAnim.snapTo(0f)
            }

            isFullscreen -> {
                fullscreenOverlayVisible = true
                fullscreenAnim.snapTo(0f)
                fullscreenAnim.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
            }

            else -> {

                if (fullscreenOverlayVisible) {
                    fullscreenAnim.animateTo(0f, tween(240, easing = FastOutSlowInEasing))
                }
                fullscreenOverlayVisible = false
            }
        }
    }

    val showFullscreenOverlay = fullscreenOverlayVisible && !isInPipMode

    fun enterFullscreen() {
        if (isFullscreen) return
        isFullscreen = true
        val window = activity.window
        brightnessBackup = window.attributes.screenBrightness
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        activity.requestedOrientation = if (controller.isPortraitVideo) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    fun exitFullscreen() {
        if (!isFullscreen) return
        isFullscreen = false
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.show(WindowInsetsCompat.Type.systemBars())

        window.attributes = window.attributes.apply {
            screenBrightness = brightnessBackup.coerceAtLeast(-1f)
        }
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    BackHandler(enabled = isFullscreen) {
        exitFullscreen()
    }

    val orientationManager = remember(activity) {
        OrientationManager(activity) { orientation ->
            val state = playerUiState.state
            if (!useLargeVideoLayout &&
                (state == PlayerState.PLAYING || state == PlayerState.PAUSED) &&
                !controller.isPortraitVideo
            ) {
                if (orientation.isLandscape && !isFullscreen &&
                    System.currentTimeMillis() - lastAutoFullscreenTime > 2_000
                ) {
                    lastAutoFullscreenTime = System.currentTimeMillis()
                    enterFullscreen()
                } else if (orientation == ScreenOrientation.PORTRAIT && isFullscreen) {
                    exitFullscreen()
                }
            }
        }
    }

    val pipAllowed = Preferences.allowPipMode && PipSupport.isPermissionGranted(activity)

    val pipEligible = playerUiState.state == PlayerState.PLAYING

    fun pipAspectRatio(): android.util.Rational {
        val w = playerUiState.videoWidth
        val h = playerUiState.videoHeight
        val ratio = if (w > 0 && h > 0) {
            (w.toFloat() / h).coerceIn(0.41841f, 2.39f)
        } else {
            16f / 9f
        }
        return android.util.Rational((ratio * 10_000).roundToInt(), 10_000)
    }

    fun buildPipParams(includeSourceHint: Boolean, eligible: Boolean): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(pipAspectRatio())
            .setActions(emptyList())
        if (includeSourceHint && playerWindowBounds.width > 0) {
            builder.setSourceRectHint(
                android.graphics.Rect(
                    playerWindowBounds.left.roundToInt(),
                    playerWindowBounds.top.roundToInt(),
                    playerWindowBounds.right.roundToInt(),
                    playerWindowBounds.bottom.roundToInt(),
                ),
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(eligible && pipAllowed)
        }
        return builder.build()
    }

    LaunchedEffect(
        pipEligible,
        playerUiState.videoWidth,
        playerUiState.videoHeight,
        isFullscreen,
        pipAllowed,
        isInPipMode,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isInPipMode) {
            runCatching { activity.setPictureInPictureParams(buildPipParams(true, pipEligible)) }
        }
    }

    DisposableEffect(activity) {
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                runCatching {
                    activity.setPictureInPictureParams(
                        PictureInPictureParams.Builder()
                            .setAutoEnterEnabled(false)
                            .build()
                    )
                }
            }
        }
    }

    fun updatePipAction() {
        if (!activity.isInPictureInPictureMode) return

        runCatching { activity.setPictureInPictureParams(buildPipParams(true, pipEligible)) }
    }

    LaunchedEffect(isInPipMode, playerUiState.state) {
        if (isInPipMode) updatePipAction()
    }

    val pageHost = remember(activity, controller, viewModel) {
        object : VideoPageHost {
            override fun showCommentBadge(count: Int) {
                viewModel.setCommentBadgeCount(count)
            }

            override fun shouldEnterPip(): Boolean {
                return controller.isActiveForPip && Preferences.allowPipMode &&
                        PipSupport.isPermissionGranted(activity)
            }

            override fun enterPipMode() {
                activity.enterPictureInPictureMode(buildPipParams(includeSourceHint = true, eligible = true))
            }

            override fun onPipModeChanged(isInPip: Boolean) {
                viewModel.setPipMode(isInPip)
                if (isInPip) {
                    updatePipAction()
                }
            }

            override fun togglePlayPause() {
                controller.togglePlayPause()
                updatePipAction()
            }
        }
    }

    DisposableEffect(activity, pageHost) {
        activity.registerCurrentVideoHost(pageHost)
        onDispose {

            activity.unregisterCurrentVideoHost(pageHost)
            viewModel.setPipMode(false)
        }
    }

    val currentOnNoVideoUrl by rememberUpdatedState {
        GlobalToasts.show(
            activity.getString(R.string.fail_to_get_video_link),
            level = GlobalToasts.ToastLevel.ERROR,
        )
        activity.browse(getHanimeVideoLink(route.videoCode))
    }

    val currentIsBackStackTop by rememberUpdatedState(isBackStackTop)

    LaunchedEffect(route.videoCode, route.localUri, viewModel.fromDownload) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.hanimeVideoStateFlow.collect { state ->
                when (state) {
                    is VideoLoadingState.Error -> {
                        state.throwable.localizedMessage?.let {
                            GlobalToasts.show(it, level = GlobalToasts.ToastLevel.ERROR)
                        }
                        if (state.throwable is ParseException) {
                            activity.browse(getHanimeVideoLink(route.videoCode))
                        }
                    }

                    is VideoLoadingState.Loading -> Unit

                    is VideoLoadingState.Success -> {
                        videoTitle = state.info.title
                        val history = DatabaseRepo.WatchHistory.findBy(route.videoCode)
                        controller.setMedia(
                            title = state.info.title,
                            videoUrls = state.info.videoUrls,
                            savedProgressMs = history?.progress ?: 0L,
                            posterUrl = state.info.coverUrl,
                        )
                        if (!viewModel.fromDownload) {
                            viewModel.insertWatchHistoryWithCover(
                                WatchHistoryEntity(
                                    state.info.coverUrl,
                                    state.info.title,
                                    state.info.uploadTimeMillis,
                                    Clock.System.now().toEpochMilliseconds(),
                                    route.videoCode,
                                )
                            )
                        }
                        if (route.autoPlay) {
                            controller.startVideo()
                        } else if (currentIsBackStackTop) {

                            controller.preload()
                        }
                    }

                    is VideoLoadingState.NoContent -> {
                        GlobalToasts.show(
                            activity.getString(R.string.video_might_not_exist),
                            level = GlobalToasts.ToastLevel.ERROR,
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(route.videoCode, controller) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.observeKeyframe(route.videoCode).collect {
                keyframeEntity = it
                viewModel.hKeyframes = it
            }
        }
    }

    fun markWatchedIfReachedThreshold() {
        if (viewModel.fromDownload) return
        val duration = controller.durationMs
        val position = controller.currentPositionMs
        if (duration > 0 && position * 100 >= duration * Preferences.watchedProgressThreshold) {
            scope.launch {
                DatabaseRepo.WatchHistory.markWatched(route.videoCode)
            }
        }
    }

    fun autoPlayNextIfEnabled() {
        if (viewModel.fromDownload || !Preferences.autoPlayNext) return
        val videos = viewModel.hanimeVideoFlow.value?.playlist?.video.orEmpty()
        if (videos.isEmpty()) return
        val currentIndex = videos.indexOfFirst { it.videoCode == route.videoCode }
        if (currentIndex < 0) return
        val next = videos.getOrNull(currentIndex + 1) ?: return
        if (isFullscreen) {
            exitFullscreen()
        }
        activity.navigator.replaceTop(VideoRoute(next.videoCode, autoPlay = true))
    }

    LaunchedEffect(controller) {
        controller.uiState.collect { state ->
            if (state.state == PlayerState.COMPLETED) {
                markWatchedIfReachedThreshold()
                autoPlayNextIfEnabled()
            }
        }
    }

    DisposableEffect(lifecycleOwner, activity, controller, route.videoCode) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    val progress = controller.currentPositionMs
                    scope.launch {
                        DatabaseRepo.WatchHistory.updateProgress(route.videoCode, progress)
                    }
                    markWatchedIfReachedThreshold()
                }

                Lifecycle.Event.ON_STOP -> {

                    if (!activity.isInPictureInPictureMode) {
                        controller.pause()
                    }
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(orientationManager)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(orientationManager)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(route.videoCode, route.localUri) {
        checkedQuality = null
        pendingDownloadPrompt = null
        videoTitle = null

        withContext(Dispatchers.Default) {
            checkBadGuy(activity, R.raw.akarin)
        }
        viewModel.videoCode = route.videoCode
        viewModel.getHanimeVideo(route.videoCode, route.localUri)
    }

    LaunchedEffect(route.videoCode, route.localUri) {
        if (route.localUri == null || route.videoCode == "-1") return@LaunchedEffect
        val entity = withContext(Dispatchers.IO) {
            runCatching { DownloadDatabase.instance.hanimeDownloadDao.find(route.videoCode) }.getOrNull()
        }
        while (true) {
            kotlinx.coroutines.delay(10_000)
            val pos = controller.currentPositionMs
            if (controller.isPlaying && pos >= 3_000L) {
                LocalWatchRecorder.record(
                    route.videoCode, entity?.title ?: videoTitle.orEmpty(), entity?.coverUrl.orEmpty(),
                    pos, controller.durationMs, true,
                )
            }
        }
    }

    DisposableEffect(route.videoCode, route.localUri) {
        onDispose {
            if (route.localUri != null && route.videoCode != "-1") {
                val pos = controller.currentPositionMs
                val dur = controller.durationMs
                if (pos >= 3_000L) {
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        val e = runCatching { DownloadDatabase.instance.hanimeDownloadDao.find(route.videoCode) }.getOrNull()
                        LocalWatchRecorder.record(route.videoCode, e?.title.orEmpty(), e?.coverUrl.orEmpty(), pos, dur, false)
                    }
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.modifyHKeyframeFlow.collect { (success, reason) ->
                val level = if (success) GlobalToasts.ToastLevel.SUCCESS else GlobalToasts.ToastLevel.ERROR
                GlobalToasts.show(reason, level = level)
            }
        }
    }

    LaunchedEffect(viewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.modifyMyListFlow.collect { state ->
                when (state) {
                    is WebsiteState.Error -> GlobalToasts.show(
                        activity.getString(R.string.add_failed),
                        level = GlobalToasts.ToastLevel.ERROR,
                    )

                    is WebsiteState.Success -> GlobalToasts.show(
                        activity.getString(R.string.add_success),
                        level = GlobalToasts.ToastLevel.SUCCESS,
                    )

                    WebsiteState.Loading -> Unit
                }
            }
        }
    }

    LaunchedEffect(viewModel, route.videoCode) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.loadDownloadedFlow.collect { entity ->
                val newQuality = checkedQuality ?: return@collect
                pendingDownloadPrompt = DownloadPromptState(
                    newQuality = newQuality,
                    oldQuality = entity?.quality,
                )
            }
        }
    }

    val actions = remember(activity, scope, viewModel, genres) {
        VideoRouteActions(
            context = activity,
            scope = scope,
            viewModel = viewModel,
            genres = genres,
            requestStoragePermission = { onGranted, onDenied, onPermanentlyDenied ->
                (activity as PermissionRequester).requestStoragePermission(
                    onGranted = onGranted,
                    onDenied = onDenied,
                    onPermanentlyDenied = onPermanentlyDenied,
                )
            },
            onPendingDownloadPromptChange = { pendingDownloadPrompt = it },
            getCheckedQuality = { checkedQuality },
            setCheckedQuality = { checkedQuality = it },
            onStoragePermissionDenied = { activity.navigator.goBack() },
            onRequestUnsubscribeConfirm = { artist -> unsubscribeArtist = artist },
            onRequestDownloadPermissionSettings = { showDownloadPermissionDialog = true },
        )
    }

    val videoInfoLoading =
        viewModel.hanimeVideoStateFlow.collectAsStateWithLifecycle().value is VideoLoadingState.Loading

    val isDownloadedPlayback = viewModel.fromDownload && route.videoCode != "-1"
    var queueVersion by remember { mutableIntStateOf(0) }
    val downloadQueue by produceState(
        initialValue = emptyList<HanimeDownloadEntity>(),
        route.videoCode, isDownloadedPlayback, queueVersion,
    ) {
        value = if (isDownloadedPlayback) DownloadedQueue.load(route.videoCode) else emptyList()
    }
    val previousDownload = if (isDownloadedPlayback) {
        DownloadedQueue.previousOf(downloadQueue, route.videoCode)
    } else null
    val nextDownload = if (isDownloadedPlayback) {
        DownloadedQueue.nextOf(downloadQueue, route.videoCode)
    } else null

    fun openDownloaded(entity: HanimeDownloadEntity) {
        if (isFullscreen) exitFullscreen()
        scope.launch {
            val uri = DownloadRepair.usableVideoUri(activity, entity)
            activity.navigator.replaceTop(VideoRoute(entity.videoCode, uri, autoPlay = true))
        }
    }

    @Composable
    fun PlayerSlot(modifier: Modifier, fullscreen: Boolean) {
        HanimeVideoPlayer(
            controller = controller,
            playerSurface = playerSurface,
            isFullscreen = fullscreen,
            keyframeEntity = keyframeEntity,
            modifier = modifier
                .onGloballyPositioned { playerWindowBounds = it.boundsInRoot() },
            videoInfoLoading = videoInfoLoading,
            onBack = {
                if (fullscreen) exitFullscreen()
                else activity.onBackPressedDispatcher.onBackPressed()
            },
            onGoHome = { activity.navigator.goHome() },
            onToggleFullscreen = {
                if (fullscreen) exitFullscreen() else enterFullscreen()
            },
            onNoVideoUrl = { currentOnNoVideoUrl() },
            onPrevious = previousDownload?.let { prev -> { openDownloaded(prev) } },
            onNext = nextDownload?.let { next -> { openDownloaded(next) } },
            onAddHKeyframeAt = { positionMs ->
                if (!controller.isPlaying) {
                    showAddHKeyframeDialog = positionMs to (videoTitle ?: "Untitled")
                } else {
                    GlobalToasts.show(
                        activity.getString(R.string.pause_then_long_press),
                        level = GlobalToasts.ToastLevel.WARNING,
                    )
                }
            },
            onEditKeyframe = { keyframe ->
                GlobalDialogs.show(
                    GlobalDialogs.InputRequest(
                        title = activity.getString(R.string.modify_h_keyframe),
                        fields = listOf(
                            TextInputField(
                                label = activity.getString(R.string.position_ms),
                                initialValue = keyframe.position.toString(),
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                            ),
                            TextInputField(
                                label = activity.getString(R.string.prompt),
                                initialValue = keyframe.prompt.orEmpty(),
                            ),
                        ),
                        confirmText = activity.getString(R.string.confirm),
                        dismissText = activity.getString(R.string.cancel),
                        onConfirm = { values ->
                            val pos = values.getOrElse(0) { keyframe.position.toString() }
                                .toLongOrNull() ?: keyframe.position
                            val prompt = values.getOrElse(1) { keyframe.prompt.orEmpty() }
                            activity.viewModel.modifyHKeyframe(
                                route.videoCode,
                                keyframe,
                                HKeyframeEntity.Keyframe(position = pos, prompt = prompt),
                            )
                            GlobalToasts.show(
                                activity.getString(R.string.modify_success),
                                level = GlobalToasts.ToastLevel.SUCCESS,
                            )
                        },
                    )
                )
            },
            onDeleteKeyframe = { keyframe ->
                GlobalDialogs.show(
                    GlobalDialogs.ConfirmRequest(
                        title = activity.getString(R.string.sure_to_delete),
                        message = com.yenaly.han1meviewer.ui.player.PlayerUtils
                            .stringForTime(keyframe.position),
                        confirmText = activity.getString(R.string.confirm),
                        dismissText = activity.getString(R.string.cancel),
                        onConfirm = {
                            activity.viewModel.removeHKeyframe(route.videoCode, keyframe)
                            GlobalToasts.show(
                                activity.getString(R.string.delete_success),
                                level = GlobalToasts.ToastLevel.SUCCESS,
                            )
                        },
                    )
                )
            },
        )
    }

    val configuration = LocalConfiguration.current
    val isTabletLandscape =
        useLargeVideoLayout && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val showSplitLayout = isTabletLandscape && !isInPipMode && !viewModel.fromDownload
    val relatedAsTab = !viewModel.fromDownload && Preferences.relatedVideoAsTab

    var isSidePaneCollapsed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(relatedAsTab, showSplitLayout) {
        viewModel.hideRelatedInIntro = relatedAsTab || showSplitLayout
    }

    val maxSmallWindowHeightPx = (appScreenHeight * 0.6f).toInt()
    LaunchedEffect(isInPipMode, isSidePaneCollapsed, showSplitLayout, configuration.orientation) {
        if (isInPipMode) return@LaunchedEffect
        val height = if (showSplitLayout) {
            if (isSidePaneCollapsed) 500.dpToPx else 400.dpToPx
        } else {

            minOf(maxOf(250.dpToPx, appScreenWidth * 9 / 16), maxSmallWindowHeightPx)
        }
        if (hostUiState.playerHeightDp != height) {
            viewModel.setPlayerHeightDp(height)
        }
    }
    val playerHeightPx = hostUiState.playerHeightDp

    val videoRatio = if (playerUiState.videoWidth > 0 && playerUiState.videoHeight > 0) {
        playerUiState.videoWidth.toFloat() / playerUiState.videoHeight
    } else {
        0f
    }

    LaunchedEffect(
        playerUiState.videoWidth,
        playerUiState.videoHeight,
        showSplitLayout,
        isInPipMode,
        configuration.orientation,
    ) {
        val w = playerUiState.videoWidth
        val h = playerUiState.videoHeight
        if (w > 0 && h > 0 && !showSplitLayout && !isFullscreen && !isInPipMode) {
            viewModel.setPlayerHeightDp(
                minOf(appScreenWidth * h / w, maxSmallWindowHeightPx)
            )
        }
    }

    var lastReportedExpanded by remember { mutableStateOf(true) }

    val pinEnabled = Preferences.pinVideoToTop
    val playbackActive = playerUiState.state in setOf(
        PlayerState.PREPARING, PlayerState.BUFFERING, PlayerState.PLAYING,
    )
    var headerCollapseRatio by remember {
        mutableFloatStateOf(if (viewModel.isAppBarExpanded(route.videoCode)) 0f else 1f)
    }
    var miniDismissed by remember { mutableStateOf(false) }
    LaunchedEffect(playbackActive) {
        if (!playbackActive) miniDismissed = false
    }
    val miniVisible = pinEnabled && playbackActive && headerCollapseRatio >= 0.999f &&
            !miniDismissed && !showSplitLayout && !isFullscreen && !isInPipMode &&
            !showFullscreenOverlay

    Box(modifier = Modifier.fillMaxSize()) {
        when {

            isInPipMode -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                ) {
                    playerSurface(Modifier.fillMaxSize())
                }
            }

            !showSplitLayout -> {
                VideoCollapsingHeader(
                    headerHeightPx = playerHeightPx,
                    scrollEnabled = pinEnabled || !playbackActive,
                    initialCollapsed = !viewModel.isAppBarExpanded(route.videoCode),
                    onCollapseRatioChange = { ratio ->
                        headerCollapseRatio = ratio
                        val expanded = ratio < 0.5f
                        if (expanded != lastReportedExpanded) {
                            lastReportedExpanded = expanded
                            viewModel.setAppBarExpanded(route.videoCode, expanded)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    headerContent = {
                        if (showFullscreenOverlay || miniVisible) {

                            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                        } else {
                            PlayerSlot(modifier = Modifier.fillMaxSize(), fullscreen = false)
                        }
                    },
                    content = {
                        VideoTabsHost(
                            activity = activity,
                            videoCode = route.videoCode,
                            localUri = route.localUri,
                            videoState = viewModel.hanimeVideoStateFlow.collectAsStateWithLifecycle().value,
                            viewModel = viewModel,
                            commentViewModel = commentViewModel,
                            fromDownload = viewModel.fromDownload,
                            pendingDownloadPrompt = pendingDownloadPrompt,
                            actions = actions,
                            stringLongPressShare = stringLongPressShare,
                            pageHost = pageHost,
                            onCheckedQualityChange = { checkedQuality = it },
                            onPendingDownloadPromptChange = { pendingDownloadPrompt = it },
                            onDownloadPermissionDialogRequest = { showDownloadPermissionDialog = true },
                            onOpenDownloaded = ::openDownloaded,
                            onDownloadsChanged = { queueVersion++ },
                        )
                    },
                )
            }

            else -> {
                val indicatorWidth = 28.dp
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                ) {
                    val sideWidth by animateDpAsState(
                        targetValue = if (isSidePaneCollapsed) indicatorWidth else maxWidth * 0.38f,
                        animationSpec = tween(durationMillis = 300),
                        label = "sideTabsWidth",
                    )
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (relatedAsTab) {

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (showFullscreenOverlay) {
                                    Box(modifier = Modifier.fillMaxWidth())
                                } else {
                                    PlayerSlot(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (videoRatio > 0f) {
                                                    Modifier.aspectRatio(
                                                        videoRatio,
                                                        matchHeightConstraintsFirst = videoRatio < 1f,
                                                    )
                                                } else {
                                                    Modifier
                                                }
                                            ),
                                        fullscreen = false,
                                    )
                                }
                            }
                        } else {

                            BoxWithConstraints(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            ) {

                                val playerRatio = videoRatio.takeIf { it > 0f } ?: (16f / 9f)
                                val expandedPlayerHeightPx = with(LocalDensity.current) {
                                    minOf(maxWidth / playerRatio, maxHeight * 0.65f).roundToPx()
                                }
                                VideoCollapsingHeader(
                                    headerHeightPx = expandedPlayerHeightPx,
                                    scrollEnabled = playerUiState.state !in setOf(
                                        PlayerState.PREPARING,
                                        PlayerState.BUFFERING,
                                        PlayerState.PLAYING,
                                    ),
                                    initialCollapsed = !viewModel.isAppBarExpanded(route.videoCode),
                                    onCollapseRatioChange = { ratio ->
                                        val expanded = ratio < 0.5f
                                        if (expanded != lastReportedExpanded) {
                                            lastReportedExpanded = expanded
                                            viewModel.setAppBarExpanded(route.videoCode, expanded)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    headerContent = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black),
                                        ) {
                                            if (showFullscreenOverlay) {

                                                Box(modifier = Modifier.fillMaxSize())
                                            } else {
                                                PlayerSlot(
                                                    modifier = Modifier.fillMaxSize(),
                                                    fullscreen = false,
                                                )
                                            }
                                        }
                                    },
                                    content = {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState()),
                                        ) {
                                            if (relatedItems.isNotEmpty()) {
                                                RelatedVideosSection(
                                                    videos = relatedItems,
                                                    onOpenVideo = { item ->
                                                        activity.showVideoDetailFragment(item.videoCode)
                                                    },
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .width(sideWidth)
                                .fillMaxHeight(),
                        ) {
                            RelatedCollapseIndicator(
                                collapsed = isSidePaneCollapsed,
                                onClick = { isSidePaneCollapsed = !isSidePaneCollapsed },
                                modifier = Modifier
                                    .width(indicatorWidth)
                                    .fillMaxHeight(),
                            )
                            if (!isSidePaneCollapsed) {
                                VideoTabsHost(
                                    activity = activity,
                                    videoCode = route.videoCode,
                                    localUri = route.localUri,
                                    videoState = viewModel.hanimeVideoStateFlow.collectAsStateWithLifecycle().value,
                                    viewModel = viewModel,
                                    commentViewModel = commentViewModel,
                                    fromDownload = viewModel.fromDownload,
                                    pendingDownloadPrompt = pendingDownloadPrompt,
                                    actions = actions,
                                    stringLongPressShare = stringLongPressShare,
                                    pageHost = pageHost,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    onCheckedQualityChange = { checkedQuality = it },
                                    onPendingDownloadPromptChange = { pendingDownloadPrompt = it },
                                    onDownloadPermissionDialogRequest = { showDownloadPermissionDialog = true },
                                    onOpenDownloaded = ::openDownloaded,
                                    onDownloadsChanged = { queueVersion++ },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (miniVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
                    .fillMaxWidth(0.5f)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black),
            ) {
                playerSurface(Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable {
                            miniDismissed = true
                            controller.pause()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = activity.getString(R.string.player_pin_close),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        if (showFullscreenOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = fullscreenAnim.value
                        val b = playerWindowBounds
                        val rectValid = b.width > 0 && b.height > 0 &&
                                b.right <= size.width && b.bottom <= size.height
                        val sx0 = if (rectValid) b.width / size.width else 1f
                        val sy0 = if (rectValid) b.height / size.height else 1f
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = lerp(sx0, 1f, p)
                        scaleY = lerp(sy0, 1f, p)
                        translationX = if (rectValid) lerp(b.left, 0f, p) else 0f
                        translationY = if (rectValid) lerp(b.top, 0f, p) else 0f

                        alpha = (p * 4f).coerceIn(0f, 1f)
                    }
                    .background(Color.Black),
            ) {
                PlayerSlot(modifier = Modifier.fillMaxSize(), fullscreen = true)
            }
        }
    }

    unsubscribeArtist?.let { artist ->
        ConfirmDialog(
            visible = true,
            title = activity.getString(R.string.unsubscribe_artist),
            message = activity.getString(R.string.sure_to_unsubscribe),
            confirmText = activity.getString(R.string.sure),
            dismissText = activity.getString(R.string.no),
            onConfirm = {
                unsubscribeArtist = null
                actions.confirmUnsubscribe(artist)
            },
            onDismiss = { unsubscribeArtist = null },
        )
    }

    if (showDownloadPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadPermissionDialog = false },
            title = { Text(activity.getString(R.string.permission_permanently_denied_title)) },
            text = { Text(activity.getString(R.string.storage_permission_settings_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDownloadPermissionDialog = false
                    actions.goToDownloadPermissionSettings()
                }) {
                    Text(activity.getString(R.string.go_to_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDownloadPermissionDialog = false
                    activity.navigator.goBack()
                }) {
                    Text(activity.getString(R.string.cancel))
                }
            },
        )
    }

    showAddHKeyframeDialog?.let { (currentPosition, title) ->
        ConfirmDialog(
            visible = true,
            title = activity.getString(R.string.add_to_h_keyframe),
            message = buildString {
                appendLine(activity.getString(R.string.sure_to_add_to_h_keyframe))
                append(activity.getString(R.string.current_position_d_ms, currentPosition))
            },
            confirmText = activity.getString(R.string.confirm),
            dismissText = activity.getString(R.string.cancel),
            onConfirm = {
                viewModel.appendHKeyframe(
                    route.videoCode,
                    title,
                    HKeyframeEntity.Keyframe(position = currentPosition, prompt = null),
                )
                Firebase.analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT) {
                    param(FirebaseAnalytics.Param.ITEM_ID, FirebaseConstants.H_KEYFRAMES)
                    param(FirebaseAnalytics.Param.CONTENT_TYPE, FirebaseConstants.H_KEYFRAMES)
                }
                showAddHKeyframeDialog = null
            },
            onDismiss = { showAddHKeyframeDialog = null },
        )
    }
}

@Composable
private fun VideoTabsHost(
    activity: MainActivity,
    videoCode: String,
    localUri: String?,
    videoState: VideoLoadingState<*>,
    viewModel: VideoViewModel,
    commentViewModel: CommentViewModel,
    fromDownload: Boolean,
    pendingDownloadPrompt: DownloadPromptState?,
    actions: VideoRouteActions,
    stringLongPressShare: String,
    pageHost: VideoPageHost,
    modifier: Modifier = Modifier,
    onCheckedQualityChange: (String?) -> Unit,
    onPendingDownloadPromptChange: (DownloadPromptState?) -> Unit,
    onDownloadPermissionDialogRequest: () -> Unit,
    onOpenDownloaded: ((com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity) -> Unit)? = null,
    onDownloadsChanged: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    Box(modifier = modifier) {
        VideoRouteContent(
        videoCode = videoCode,
        videoState = videoState,
        videoViewModel = viewModel,
        commentViewModel = commentViewModel,
        fromDownload = fromDownload,
        pendingDownloadPrompt = pendingDownloadPrompt,
        onPendingDownloadPromptChange = onPendingDownloadPromptChange,
        onRetry = { viewModel.getHanimeVideo(videoCode, localUri) },
        onOpenVideo = { item -> activity.showVideoDetailFragment(item.videoCode) },
        onOpenArtist = actions::openArtistSearch,
        onNavigateToSearch = actions::openTagSearch,
        onToggleSubscribe = actions::toggleArtistSubscription,
        onToggleFavorite = actions::toggleFavorite,
        onRateVideo = actions::rateVideo,
        onManageMyList = actions::updateMyListSelection,
        onQuickCheckIn = { record ->
            val normalizedRecord = if (record.sideDishes.contains("\u001E")) {
                record
            } else {
                record.copy(sideDishes = "${record.sideDishes}\u001E${videoCode}")
            }
            scope.launch(Dispatchers.IO) {
                CheckInRecordDatabase.getDatabase(activity).checkInDao()
                    .insert(normalizedRecord)
                withContext(Dispatchers.Main) {
                    GlobalToasts.show(
                        activity.getString(R.string.checkin),
                        level = GlobalToasts.ToastLevel.SUCCESS,
                    )
                }
            }
        },
        onPrepareDownload = { quality, video ->
            onCheckedQualityChange(quality)
            video?.let(actions::startDownloadFlow)
        },
        onConfirmDownloadPrompt = { video, autoGroupName ->
            video?.let {
                actions.confirmPendingDownload(it, pendingDownloadPrompt, autoGroupName)
            }
        },
        onRequestOpenOfficialDownloadPage = actions::openOfficialDownloadPage,
        onRequestOpenDownloadPermissionSettings = onDownloadPermissionDialogRequest,
        onOpenWebPage = actions::openVideoWebPage,
        onOpenOriginalComic = actions::openOriginalComic,
        onOpenShare = { content, title ->
            ShareCompat.IntentBuilder(activity)
                .setType("text/plain")
                .setText(content)
                .setChooserTitle(title)
                .startChooser()
        },
        onCopyText = {
            it.copyToClipboard()
            GlobalToasts.show(
                activity.getString(R.string.copy_to_clipboard),
                level = GlobalToasts.ToastLevel.INFO,
            )
        },
        onIntroductionLinkClick = actions::openIntroductionLink,
        stringLongPressShare = stringLongPressShare,
        pageHost = pageHost,
        onOpenDownloaded = onOpenDownloaded,
        onDownloadsChanged = onDownloadsChanged,
        )
    }
}

@Composable
private fun RelatedCollapseIndicator(
    collapsed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (collapsed) {
                Icons.AutoMirrored.Filled.KeyboardArrowLeft
            } else {
                Icons.AutoMirrored.Filled.KeyboardArrowRight
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
        )
    }
}
