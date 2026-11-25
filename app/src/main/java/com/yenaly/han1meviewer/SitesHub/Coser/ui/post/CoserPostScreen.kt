package com.yenaly.han1meviewer.SitesHub.Coser.ui.post

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.yenaly.han1meviewer.SitesHub.Coser.common.CoserImageLoader
import com.yenaly.han1meviewer.SitesHub.Coser.data.model.CoserCard
import com.yenaly.han1meviewer.SitesHub.Coser.data.model.CoserDetail
import com.yenaly.han1meviewer.SitesHub.Coser.data.model.CoserTerm
import com.yenaly.han1meviewer.SitesHub.Coser.data.remote.CoserNetwork
import com.yenaly.han1meviewer.SitesHub.Coser.data.repository.CoserHistoryRepo
import com.yenaly.han1meviewer.SitesHub.Coser.common.CoserSettings
import com.yenaly.han1meviewer.SitesHub.Coser.ui.list.COSER_CARD_RATIO
import com.yenaly.han1meviewer.SitesHub.Coser.ui.list.coserBadges
import com.yenaly.han1meviewer.SitesHub.FapFolder.ui.components.rememberFapFolderImageGate
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.component.CollapsingPlayerLayout
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.SiteBackWithHomeButton
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import com.yenaly.han1meviewer.ui.component.SiteVideoActionBar
import com.yenaly.han1meviewer.ui.component.SiteVideoCard
import com.yenaly.han1meviewer.ui.component.player.SitePlayerSettings
import com.yenaly.han1meviewer.ui.component.player.PipSupport
import com.yenaly.han1meviewer.ui.component.player.SitePipEffect
import com.yenaly.han1meviewer.ui.component.player.createSitePlayer
import com.yenaly.han1meviewer.ui.screen.RetryableImage
import com.yenaly.han1meviewer.ui.screen.rememberVideoGridColumns
import com.yenaly.han1meviewer.util.PlayerOrientation
import com.yenaly.han1meviewer.util.SiteImageSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val PLAYER_TAG = "CoserPlayer"
private const val RESUME_THRESHOLD_MS = 5_000L
private const val RESUME_CLAMP_BACKOFF_MS = 5_000L
private const val SAVE_INTERVAL_MS = 2_000L
private const val DELTA_GUARD_MS = 10_000L

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

private fun buildMediaSource(context: Context, url: String, referer: String, origin: String): MediaSource {
    val http = OkHttpDataSource.Factory(CoserNetwork.sharedOkHttpClient)
        .setDefaultRequestProperties(hashMapOf("Referer" to referer, "Origin" to origin))
    val dsFactory = DefaultDataSource.Factory(context, http)
    val item = MediaItem.Builder().setUri(url).build()
    return if (url.substringBefore('?').contains(".m3u8", ignoreCase = true)) {
        HlsMediaSource.Factory(dsFactory).createMediaSource(item)
    } else {
        ProgressiveMediaSource.Factory(dsFactory).createMediaSource(item)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoserPostScreen(
    slug: String,
    onBack: () -> Unit,
    onNavigateToPost: (String) -> Unit,
    onNavigateToTerm: (CoserTerm) -> Unit,
    onOpenPhotos: () -> Unit = {},
    resumePosition: Long = 0L,
    viewModel: CoserPostViewModel = viewModel(),
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findHostActivity() }

    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val streamState by viewModel.streamState.collectAsStateWithLifecycle()
    val selectedVideo by viewModel.selectedVideo.collectAsStateWithLifecycle()
    val selectedMirror by viewModel.selectedMirror.collectAsStateWithLifecycle()
    val referer by viewModel.referer.collectAsStateWithLifecycle()
    val origin by viewModel.origin.collectAsStateWithLifecycle()

    var pendingResume by remember { mutableLongStateOf(0L) }
    var resumeApplied by remember { mutableStateOf(false) }
    var showResumeDialog by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

    val exoPlayer = remember { mutableStateOf<ExoPlayer?>(null) }
    val currentMediaUrl = remember { mutableStateOf<String?>(null) }

    CoserHistoryRepo.init(context)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !PipSupport.inPip && viewModel.playerState.value.isPlaying) {
                viewModel.setPlaying(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (viewModel.playerState.value.isFullscreen) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    activity?.window?.insetsController?.show(
                        android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                }
                viewModel.setFullscreen(false)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d(PLAYER_TAG, "Screen disposing: releasing player")
            exoPlayer.value?.release()
            exoPlayer.value = null
            currentMediaUrl.value = null
        }
    }

    LaunchedEffect(playerState.url) {
        val url = playerState.url
        if (!playerState.isReady || url.isBlank()) return@LaunchedEffect
        val existing = exoPlayer.value
        if (existing != null && currentMediaUrl.value == url) return@LaunchedEffect
        if (existing == null) {
            val player = createSitePlayer(context)
            player.setMediaSource(buildMediaSource(context, url, referer, origin))
            player.prepare()
            player.setPlaybackSpeed(playerState.speed)
            exoPlayer.value = player
        } else {
            val wasPlaying = existing.playWhenReady
            val pos = existing.currentPosition
            existing.setMediaSource(buildMediaSource(context, url, referer, origin))
            existing.prepare()
            existing.seekTo(pos)
            existing.playWhenReady = wasPlaying
        }
        currentMediaUrl.value = url
    }

    val playerConfig = SitePlayerSettings.config(SiteType.COSER)
    val inPip = PipSupport.inPip
    SitePipEffect(site = SiteType.COSER, isPlaying = playerState.isPlaying)
    LaunchedEffect(playerConfig.defaultSpeed) { viewModel.setSpeed(playerConfig.defaultSpeed) }
    LaunchedEffect(playerState.isReady, playerState.url, playerConfig.autoPlay) {
        if (playerConfig.autoPlay && playerState.isReady && playerState.url.isNotBlank()) {
            delay(500)
            if (!showResumeDialog && !viewModel.playerState.value.isPlaying) viewModel.setPlaying(true)
        }
    }

    LaunchedEffect(slug) {
        viewModel.load(slug)
        resumeApplied = false
        pendingResume = 0L
    }

    LaunchedEffect(slug, resumePosition) {
        val history = withContext(Dispatchers.IO) { CoserHistoryRepo.getByVideoCode(slug) }
        pendingResume = when {
            resumePosition > 0L -> resumePosition
            history != null && history.lastPosition > RESUME_THRESHOLD_MS &&
                    (history.totalDuration <= 0L || history.lastPosition < history.totalDuration - RESUME_CLAMP_BACKOFF_MS) ->
                history.lastPosition
            else -> 0L
        }
        if (pendingResume > 0L && playerConfig.resumePlayback) showResumeDialog = true
    }

    LaunchedEffect(playerState.duration, pendingResume, exoPlayer.value) {
        if (!resumeApplied && pendingResume > 0L && playerState.duration > 0L) {
            val maxSeek = (playerState.duration - RESUME_CLAMP_BACKOFF_MS).coerceAtLeast(0L)
            exoPlayer.value?.seekTo(pendingResume.coerceAtMost(maxSeek))
            resumeApplied = true
        }
    }

    LaunchedEffect(playerState.isPlaying, detailState) {
        val detail = (detailState as? CoserDetailState.Success)?.detail ?: return@LaunchedEffect
        if (!playerState.isPlaying) return@LaunchedEffect
        var lastSaved = playerState.position
        while (isActive) {
            delay(SAVE_INTERVAL_MS)
            val pos = playerState.position
            val delta = pos - lastSaved
            lastSaved = pos
            val total = playerState.duration
            if (total > 0L && pos > 0L && delta in 0L..DELTA_GUARD_MS) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        CoserHistoryRepo.upsertWatchProgress(
                            videoCode = slug,
                            title = detail.title,
                            coverUrl = detail.coverUrl,
                            currentPosition = pos,
                            totalDuration = total,
                            isPlaying = true,
                            wasPlayed = pos >= total - RESUME_CLAMP_BACKOFF_MS,
                        )
                    }
                }
            }
        }
    }

    fun enterFullscreen() {
        activity?.requestedOrientation = PlayerOrientation.fullscreenRequest()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity?.window?.insetsController?.hide(
                android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars()
            )
        } else {
            @Suppress("DEPRECATION")
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        viewModel.setFullscreen(true)
    }

    fun exitFullscreen() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity?.window?.insetsController?.show(
                android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars()
            )
        } else {
            @Suppress("DEPRECATION")
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        viewModel.setFullscreen(false)
    }

    BackHandler(enabled = playerState.isFullscreen) { exitFullscreen() }

    val detail = (detailState as? CoserDetailState.Success)?.detail

    @Composable
    fun Player(fullscreen: Boolean, modifier: Modifier) {
        CoserVideoPlayer(
            state = playerState,
            exoPlayer = exoPlayer.value,
            coverUrl = detail?.coverUrl.orEmpty(),
            isFullscreen = fullscreen,
            onPlayPause = { viewModel.setPlaying(!playerState.isPlaying) },
            onSeek = { pos -> exoPlayer.value?.seekTo(pos) },
            onSkip = { delta ->
                val current = exoPlayer.value?.currentPosition ?: 0L
                exoPlayer.value?.seekTo((current + delta).coerceAtLeast(0L))
            },
            onToggleFullscreen = { if (fullscreen) exitFullscreen() else enterFullscreen() },
            onPositionUpdate = { pos, dur -> viewModel.setPosition(pos, dur) },
            modifier = modifier,
            showResumeButton = !fullscreen && showResumeDialog,
            savedPosition = pendingResume,
            onResumeFromSaved = {
                exoPlayer.value?.seekTo(pendingResume)
                showResumeDialog = false
            },
            onStartFromBeginning = {
                exoPlayer.value?.seekTo(0)
                showResumeDialog = false
            },
            title = detail?.title.orEmpty(),
            onBack = { if (fullscreen) exitFullscreen() else onBack() },
            onSpeedSelect = { viewModel.setSpeed(it) },
            onQualitySelect = { viewModel.setQuality(it) },
            referer = referer,
        )
    }

    if (playerState.isFullscreen) {
        Dialog(
            onDismissRequest = { exitFullscreen() },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) { Player(fullscreen = true, modifier = Modifier.fillMaxSize()) }
    }

    Scaffold(
        topBar = {
            if (!inPip) SiteTopAppBar(
                title = {
                    Text(
                        text = detail?.title ?: stringResource(R.string.video),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = { SiteBackWithHomeButton(onBack = onBack) },
            )
        },
    ) { paddingValues ->
        when (val state = detailState) {
            is CoserDetailState.Loading -> SiteLoadingContent(Modifier.padding(paddingValues))
            is CoserDetailState.Error -> SiteErrorContent(
                message = state.message,
                onRetry = { viewModel.load(slug, force = true) },
                modifier = Modifier.padding(paddingValues),
            )
            is CoserDetailState.Success -> {
                val info = state.detail
                val content: @Composable () -> Unit = {
                    CoserPostContent(
                        info = info,
                        slug = slug,
                        gridState = gridState,
                        selectedVideo = selectedVideo,
                        selectedMirror = selectedMirror,
                        streamState = streamState,
                        playerState = playerState,
                        viewModel = viewModel,
                        onOpenPhotos = onOpenPhotos,
                        onNavigateToPost = onNavigateToPost,
                        onNavigateToTerm = onNavigateToTerm,
                    )
                }
                if (info.hasVideo) {
                    CollapsingPlayerLayout(
                        pip = inPip,
                        pinEnabled = playerConfig.pinVideo,
                        pinActive = playerState.isPlaying,
                        onPinClose = { viewModel.setPlaying(false) },
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        player = {
                            if (!playerState.isFullscreen) Player(fullscreen = false, modifier = Modifier.fillMaxWidth())
                        },
                        content = content,
                    )
                } else {
                    Box(Modifier.fillMaxSize().padding(paddingValues)) { content() }
                }
            }
        }
    }
}

@Composable
private fun SectionBanner(
    icon: ImageVector,
    title: String,
    container: androidx.compose.ui.graphics.Color,
    onContainer: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(color = container, contentColor = onContainer, shape = RoundedCornerShape(16.dp), modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            trailing?.invoke()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoserPostContent(
    info: CoserDetail,
    slug: String,
    gridState: LazyGridState,
    selectedVideo: Int,
    selectedMirror: Int,
    streamState: CoserStreamState,
    playerState: CoserPlayerState,
    viewModel: CoserPostViewModel,
    onOpenPhotos: () -> Unit,
    onNavigateToPost: (String) -> Unit,
    onNavigateToTerm: (CoserTerm) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageLoader = CoserImageLoader.get(context)
    val imageGate = rememberFapFolderImageGate()
    val columns = remember { CoserSettings.photoColumns }
    val relatedColumns = rememberVideoGridColumns().coerceAtLeast(2)
    var savingAll by remember { mutableStateOf(false) }
    val savedMsg = stringResource(R.string.fap_photo_saved)
    val gridColumns = lcm(columns, relatedColumns)

    fun saveAll() {
        if (savingAll || info.photos.isEmpty()) return
        imageGate {
            savingAll = true
            scope.launch {
                val result = SiteImageSaver.saveBatch(
                    context = context,
                    urls = info.photos,
                    segments = listOf("Coser", coserSafeName(info.title)),
                    baseName = "photo",
                    client = CoserImageLoader.downloadClient(),
                    headers = CoserImageLoader.downloadHeaders(info.pageUrl),
                    onProgress = null,
                )
                savingAll = false
                when (result) {
                    is SiteImageSaver.SaveResult.Error ->
                        GlobalToasts.show(result.message, level = GlobalToasts.ToastLevel.ERROR)
                    is SiteImageSaver.SaveResult.Saved -> GlobalToasts.show(
                        "$savedMsg (${result.count})", level = GlobalToasts.ToastLevel.SUCCESS,
                    )
                }
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp, top = 12.dp, end = 12.dp,
            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {

        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                Text(info.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val statLine = listOf(
                    info.views.takeIf { it.isNotBlank() }?.let { "$it views" },
                    info.duration.takeIf { it.isNotBlank() },
                    info.resolution.takeIf { it.isNotBlank() },
                    info.postedAt.takeIf { it.isNotBlank() },
                ).filterNotNull().joinToString("  ·  ")
                if (statLine.isNotBlank()) {
                    Text(
                        statLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (info.categories.isNotEmpty() || info.tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        info.categories.forEach { term ->
                            FilterChip(selected = true, onClick = { onNavigateToTerm(term) }, label = { Text(term.name) })
                        }
                        info.tags.forEach { term ->
                            AssistChip(onClick = { onNavigateToTerm(term) }, label = { Text(term.name) })
                        }
                    }
                }
            }
        }

        item(key = "actions", span = { GridItemSpan(maxLineSpan) }) {
            SiteVideoActionBar(
                site = SiteType.COSER,
                videoCode = slug,
                title = info.title,
                coverUrl = info.coverUrl,
                webUrl = info.pageUrl,
                downloadUrl = playerState.url.takeIf { it.isNotBlank() },
                downloadQuality = playerState.quality,
                resolveDownloadUrl = if (info.hasVideo) {
                    {
                        viewModel.playerState.value.url.takeIf { it.isNotBlank() }
                            ?: withTimeoutOrNull(20_000L) {
                                viewModel.playerState.first { it.url.isNotBlank() }.url
                            }
                    }
                } else null,
                onDownloadClick = if (!info.hasVideo) ({ saveAll() }) else null,
            )
        }

        if (info.hasVideo) {
            item(key = "video_section", span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    SectionBanner(
                        icon = Icons.Default.Videocam,
                        title = stringResource(R.string.coser_section_video, info.videos.size),
                        container = MaterialTheme.colorScheme.primaryContainer,
                        onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    if (info.videos.size > 1) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            info.videos.forEachIndexed { i, _ ->
                                FilterChip(
                                    selected = i == selectedVideo,
                                    onClick = { viewModel.selectVideo(i) },
                                    label = { Text(stringResource(R.string.coser_video_n, i + 1)) },
                                )
                            }
                        }
                    }
                    val mirrors = info.videos.getOrNull(selectedVideo)?.mirrors.orEmpty()
                    if (mirrors.size > 1) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            mirrors.forEachIndexed { i, m ->
                                FilterChip(
                                    selected = i == selectedMirror,
                                    onClick = { viewModel.selectMirror(i) },
                                    label = { Text(m.provider.replaceFirstChar { c -> c.uppercase() }) },
                                )
                            }
                        }
                    }
                    when (streamState) {
                        is CoserStreamState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.site_fetching_video_link), style = MaterialTheme.typography.bodySmall)
                        }
                        is CoserStreamState.Failed -> Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    stringResource(R.string.coser_err_no_stream),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Row {
                                    TextButton(onClick = { viewModel.retry() }) { Text(stringResource(R.string.retry)) }
                                    TextButton(onClick = {
                                        val target = info.videos.getOrNull(selectedVideo)?.url ?: info.pageUrl
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, target.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        }
                                    }) { Text(stringResource(R.string.jump_to_webpage)) }
                                }
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }

        if (info.hasPhotos) {
            item(key = "photos_entry", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.padding(top = 8.dp).clip(RoundedCornerShape(16.dp)).clickable { onOpenPhotos() }) {
                    SectionBanner(
                        icon = Icons.Default.PhotoLibrary,
                        title = stringResource(R.string.coser_section_photos, info.photos.size),
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
                        trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    )
                }
            }
        }

        if (info.related.isNotEmpty()) {
            item(key = "related_header", span = { GridItemSpan(maxLineSpan) }) {
                SectionBanner(
                    icon = Icons.Default.PhotoLibrary,
                    title = stringResource(R.string.coser_related),
                    container = MaterialTheme.colorScheme.surfaceContainerHigh,
                    onContainer = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            val relSpan = gridColumns / relatedColumns
            itemsIndexed(info.related, key = { _, c -> "rel_${c.slug}" }, span = { _, _ -> GridItemSpan(relSpan) }) { _, card ->
                RelatedCard(card, imageLoader, onNavigateToPost)
            }
        }
    }
}

@Composable
private fun RelatedCard(
    card: CoserCard,
    imageLoader: coil3.ImageLoader,
    onOpen: (String) -> Unit,
) {
    val (top, bottom) = coserBadges(card)
    SiteVideoCard(
        title = card.title,
        coverUrl = card.coverUrl,
        onClick = { onOpen(card.slug) },
        aspectRatio = COSER_CARD_RATIO,
        topEndBadge = top,
        bottomStartBadge = bottom,
        imageLoader = imageLoader,
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
    )
}

private fun Modifier.androidxAspect(ratio: Float): Modifier =
    this.then(Modifier.aspectRatio(ratio))

private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
private fun lcm(a: Int, b: Int): Int = a / gcd(a, b) * b
