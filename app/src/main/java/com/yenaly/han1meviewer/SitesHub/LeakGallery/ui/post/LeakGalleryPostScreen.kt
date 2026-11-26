package com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.post

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
import androidx.compose.foundation.lazy.grid.items
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
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryImageLoader
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryConstants
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryDetail
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.list.leakGalleryDuration
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryTerm
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote.LeakGalleryNetwork
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.repository.LeakGalleryHistoryRepo
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGallerySettings
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.list.LEAKGALLERY_CARD_RATIO
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

private const val PLAYER_TAG = "LeakGalleryPlayer"
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
    val http = OkHttpDataSource.Factory(LeakGalleryNetwork.sharedOkHttpClient)
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
fun LeakGalleryPostScreen(
    slug: String,
    onBack: () -> Unit,
    onNavigateToPost: (String) -> Unit,
    onNavigateToTerm: (LeakGalleryTerm) -> Unit,
    onOpenPhotos: () -> Unit = {},
    resumePosition: Long = 0L,
    viewModel: LeakGalleryPostViewModel = viewModel(),
    onOpenShorts: (mode: String, startId: String) -> Unit = { _, _ -> },
    onLogin: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findHostActivity() }

    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val referer = viewModel.referer
    val origin = viewModel.origin

    var pendingResume by remember { mutableLongStateOf(0L) }
    var resumeApplied by remember { mutableStateOf(false) }
    var showResumeDialog by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

    val exoPlayer = remember { mutableStateOf<ExoPlayer?>(null) }
    val currentMediaUrl = remember { mutableStateOf<String?>(null) }

    LeakGalleryHistoryRepo.init(context)

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

    val playerConfig = SitePlayerSettings.config(SiteType.LEAKGALLERY)
    val inPip = PipSupport.inPip
    SitePipEffect(site = SiteType.LEAKGALLERY, isPlaying = playerState.isPlaying)
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
        val history = withContext(Dispatchers.IO) { LeakGalleryHistoryRepo.getByVideoCode(slug) }
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
        val detail = (detailState as? LeakGalleryDetailState.Success)?.detail ?: return@LaunchedEffect
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
                        LeakGalleryHistoryRepo.upsertWatchProgress(
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

    val detail = (detailState as? LeakGalleryDetailState.Success)?.detail

    @Composable
    fun Player(fullscreen: Boolean, modifier: Modifier) {
        LeakGalleryVideoPlayer(
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
            is LeakGalleryDetailState.Loading -> SiteLoadingContent(Modifier.padding(paddingValues))
            is LeakGalleryDetailState.Error -> SiteErrorContent(
                message = state.message,
                onRetry = { viewModel.load(slug, force = true) },
                modifier = Modifier.padding(paddingValues),
            )
            is LeakGalleryDetailState.Success -> {
                val info = state.detail
                val content: @Composable () -> Unit = {
                    LeakGalleryPostContent(
                        info = info,
                        slug = slug,
                        gridState = gridState,
                        playerState = playerState,
                        viewModel = viewModel,
                        onOpenPhotos = onOpenPhotos,
                        onNavigateToTerm = onNavigateToTerm,
                        onNavigateToPost = onNavigateToPost,
                        onOpenShorts = onOpenShorts,
                        onLogin = onLogin,
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
private fun LeakGalleryPostContent(
    info: LeakGalleryDetail,
    slug: String,
    gridState: LazyGridState,
    playerState: LeakGalleryPlayerState,
    viewModel: LeakGalleryPostViewModel,
    onOpenPhotos: () -> Unit,
    onNavigateToTerm: (LeakGalleryTerm) -> Unit,
    onNavigateToPost: (String) -> Unit,
    onOpenShorts: (mode: String, startId: String) -> Unit,
    onLogin: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageLoader = LeakGalleryImageLoader.get(context)
    val imageGate = rememberFapFolderImageGate()
    val relatedColumns = rememberVideoGridColumns().coerceAtLeast(2)
    var savingAll by remember { mutableStateOf(false) }
    val savedMsg = stringResource(R.string.fap_photo_saved)

    fun saveAll() {
        if (savingAll || info.photos.isEmpty()) return
        imageGate {
            savingAll = true
            scope.launch {
                val result = SiteImageSaver.saveBatch(
                    context = context,
                    urls = info.photos,
                    segments = listOf("LeakGallery", leakgallerySafeName(info.creator?.name.orEmpty().ifBlank { info.title })),
                    baseName = "photo",
                    client = LeakGalleryImageLoader.downloadClient(),
                    headers = LeakGalleryImageLoader.downloadHeaders("${LeakGalleryConstants.BASE_URL}/"),
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
        columns = GridCells.Fixed(relatedColumns),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp, top = 12.dp, end = 12.dp,
            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {

        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                Text(info.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val statLine = listOf(
                    if (info.views > 0) "${info.views} views" else null,
                    info.postedAt.takeIf { it.isNotBlank() },
                ).filterNotNull().joinToString("  ·  ")
                if (statLine.isNotBlank()) {
                    Text(
                        statLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    info.creator?.let { creator ->
                        FilterChip(
                            selected = true,
                            onClick = { onNavigateToTerm(creator) },
                            label = { Text(creator.name) },
                        )
                    }
                    info.tags.forEach { tag ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                onNavigateToTerm(
                                    LeakGalleryTerm(
                                        slug = tag,
                                        name = "#$tag",
                                        customPath = LeakGalleryConstants.tagPath(tag),
                                    )
                                )
                            },
                            label = { Text("#$tag") },
                        )
                    }
                }
                if (info.caption.isNotBlank() && info.caption != info.title) {
                    Text(info.caption, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item(key = "social", span = { GridItemSpan(maxLineSpan) }) {
            LeakGalleryPostSocialRow(info = info, onOpenShorts = onOpenShorts, onLogin = onLogin)
        }

        item(key = "actions", span = { GridItemSpan(maxLineSpan) }) {
            SiteVideoActionBar(
                site = SiteType.LEAKGALLERY,
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

        if (info.hasPhotos) {
            item(key = "photo", span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenPhotos() },
                ) {
                    coil3.compose.AsyncImage(
                        model = info.photos.first(),
                        imageLoader = imageLoader,
                        contentDescription = info.title,
                        contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (info.similar.isNotEmpty()) {
            item(key = "similar_header", span = { GridItemSpan(maxLineSpan) }) {
                SectionBanner(
                    icon = Icons.Default.PhotoLibrary,
                    title = stringResource(R.string.leakgallery_similar),
                    container = MaterialTheme.colorScheme.surfaceContainerHigh,
                    onContainer = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            items(info.similar, key = { "sim_${it.slug}" }) { card ->
                SiteVideoCard(
                    title = card.title,
                    coverUrl = card.coverUrl,
                    onClick = { onNavigateToPost(card.slug) },
                    aspectRatio = LEAKGALLERY_CARD_RATIO,
                    duration = if (card.isVideo) leakGalleryDuration(card.durationSec) else null,
                    subtitle = card.username.ifBlank { null },
                    topEndBadge = if (card.isVideo) null else "PHOTO",
                    imageLoader = imageLoader,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            }
        }

        if (info.similarCreators.isNotEmpty()) {
            item(key = "creators_header", span = { GridItemSpan(maxLineSpan) }) {
                SectionBanner(
                    icon = Icons.Default.PhotoLibrary,
                    title = stringResource(R.string.leakgallery_related),
                    container = MaterialTheme.colorScheme.surfaceContainerHigh,
                    onContainer = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            items(info.similarCreators, key = { "cr_${it.slug}" }) { term ->
                SiteVideoCard(
                    title = term.name,
                    coverUrl = term.thumbUrl,
                    onClick = { onNavigateToTerm(term) },
                    aspectRatio = LEAKGALLERY_CARD_RATIO,
                    imageLoader = imageLoader,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            }
        }
    }
}

private fun Modifier.androidxAspect(ratio: Float): Modifier =
    this.then(Modifier.aspectRatio(ratio))

private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
private fun lcm(a: Int, b: Int): Int = a / gcd(a, b) * b
