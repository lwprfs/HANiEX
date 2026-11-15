package com.yenaly.han1meviewer.Buondua.ui.album

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Build
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.yenaly.han1meviewer.Buondua.common.BuonduaDownloads
import com.yenaly.han1meviewer.Buondua.common.BuonduaImageLoader
import com.yenaly.han1meviewer.Buondua.data.remote.BuonduaNetwork
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.component.CollapsingPlayerLayout
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import com.yenaly.han1meviewer.ui.component.SiteVideoActionBar
import com.yenaly.han1meviewer.ui.component.SiteVideoCard
import com.yenaly.han1meviewer.ui.component.player.SitePlayerSettings
import com.yenaly.han1meviewer.ui.component.player.createSitePlayer
import com.yenaly.han1meviewer.ui.screen.rememberVideoGridColumns
import com.yenaly.han1meviewer.util.PlayerOrientation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

private fun buildVideoSource(context: Context, url: String): MediaSource {
    val http = OkHttpDataSource.Factory(BuonduaNetwork.streamClient)
        .setDefaultRequestProperties(BuonduaNetwork.mediaHeaders())
    val dsFactory = DefaultDataSource.Factory(context, http)
    return ProgressiveMediaSource.Factory(dsFactory)
        .createMediaSource(MediaItem.Builder().setUri(url).build())
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuonduaAlbumScreen(
    code: String,
    onBack: () -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToTag: (url: String, title: String) -> Unit,
    onOpenPhotos: () -> Unit = {},
    viewModel: BuonduaAlbumViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val selectedVideo by viewModel.selectedVideo.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findHostActivity() }
    val scope = rememberCoroutineScope()
    val imageLoader = BuonduaImageLoader.get(context)

    val exoPlayer = remember { mutableStateOf<ExoPlayer?>(null) }
    val currentMediaUrl = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(code) { viewModel.load(code) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && viewModel.playerState.value.isPlaying) {
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
            player.setMediaSource(buildVideoSource(context, url))
            player.prepare()
            player.setPlaybackSpeed(playerState.speed)
            exoPlayer.value = player
        } else {
            existing.setMediaSource(buildVideoSource(context, url))
            existing.prepare()
            existing.playWhenReady = false
        }
        currentMediaUrl.value = url
    }

    val playerConfig = SitePlayerSettings.config(SiteType.BUONDUA)
    LaunchedEffect(playerConfig.defaultSpeed) { viewModel.setSpeed(playerConfig.defaultSpeed) }
    LaunchedEffect(playerState.isReady, playerState.url, playerConfig.autoPlay) {
        if (playerConfig.autoPlay && playerState.isReady && playerState.url.isNotBlank()) {
            delay(500)
            if (!viewModel.playerState.value.isPlaying) viewModel.setPlaying(true)
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

    @Composable
    fun Player(fullscreen: Boolean, modifier: Modifier) {
        BuonduaVideoPlayer(
            state = playerState,
            exoPlayer = exoPlayer.value,
            coverUrl = state.coverUrl,
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
            title = state.title,
            onBack = { if (fullscreen) exitFullscreen() else onBack() },
            onSpeedSelect = { viewModel.setSpeed(it) },
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
            SiteTopAppBar(
                title = { Text(state.title.ifBlank { stringResource(R.string.buondua_album) }, maxLines = 1) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> SiteLoadingContent()
                state.error != null -> SiteErrorContent(
                    message = state.error.orEmpty(),
                    onRetry = { viewModel.retry() },
                )
                else -> {
                    val content: @Composable () -> Unit = {
                        AlbumContent(
                            state = state,
                            selectedVideo = selectedVideo,
                            playerUrl = playerState.url,
                            imageLoader = imageLoader,
                            onSelectVideo = { viewModel.selectVideo(it) },
                            onOpenPhotos = onOpenPhotos,
                            onDownloadPhotos = {
                                scope.launch {
                                    BuonduaDownloads.downloadAllPhotos(
                                        context = context,
                                        code = state.code,
                                        title = state.title,
                                        loadedUrls = state.photos.map { it.url },
                                        totalPages = state.totalPages,
                                    )
                                }
                            },
                            onNavigateToAlbum = onNavigateToAlbum,
                            onNavigateToTag = onNavigateToTag,
                        )
                    }
                    if (state.videos.isNotEmpty()) {
                        CollapsingPlayerLayout(
                            modifier = Modifier.fillMaxSize(),
                            player = {
                                if (!playerState.isFullscreen) Player(fullscreen = false, modifier = Modifier.fillMaxWidth())
                            },
                            content = content,
                        )
                    } else {
                        content()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlbumContent(
    state: BuonduaAlbumUiState,
    selectedVideo: Int,
    playerUrl: String,
    imageLoader: coil3.ImageLoader,
    onSelectVideo: (Int) -> Unit,
    onOpenPhotos: () -> Unit,
    onDownloadPhotos: () -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToTag: (url: String, title: String) -> Unit,
) {
    val context = LocalContext.current
    val columns = rememberVideoGridColumns().coerceAtLeast(2)
    val hasVideos = state.videos.isNotEmpty()
    val photoCount = state.photoCount ?: state.photos.size

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp, top = 12.dp, end = 12.dp,
            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                val counts = buildList {
                    state.photoCount?.let { add("$it photos") }
                    state.videoCount?.let { add("$it videos") }
                }.joinToString(" + ")
                if (counts.isNotBlank()) {
                    Text(
                        text = counts,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        item(key = "actions", span = { GridItemSpan(maxLineSpan) }) {
            SiteVideoActionBar(
                site = SiteType.BUONDUA,
                videoCode = state.code,
                title = state.title,
                coverUrl = state.coverUrl,
                webUrl = state.pageUrl,
                onDownloadClick = {
                    val video = state.videos.getOrNull(selectedVideo)
                    if (video != null) BuonduaDownloads.enqueueVideo(context, state.code, video, state.coverUrl)
                    else onDownloadPhotos()
                },
            )
        }

        if (state.tags.isNotEmpty()) {
            item(key = "tags", span = { GridItemSpan(maxLineSpan) }) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    state.tags.forEach { tag ->
                        AssistChip(
                            onClick = { onNavigateToTag(tag.url, tag.title) },
                            label = { Text(tag.title) },
                        )
                    }
                }
            }
        }

        if (hasVideos) {
            item(key = "videos", span = { GridItemSpan(maxLineSpan) }) {
                BuonduaVideoControls(
                    albumCode = state.code,
                    coverUrl = state.coverUrl,
                    videos = state.videos,
                    selected = selectedVideo,
                    onSelect = onSelectVideo,
                )
            }
        }

        if (photoCount > 0 || state.photos.isNotEmpty()) {
            item(key = "photos_entry", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.padding(top = 4.dp).clip(RoundedCornerShape(16.dp)).clickable { onOpenPhotos() }) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "${stringResource(R.string.buondua_section_photos)} ($photoCount)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                }
            }
        }

        if (state.related.isNotEmpty()) {
            item(key = "related_header", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.buondua_section_related),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            items(state.related, key = { "related_" + it.videoCode }) { rel ->
                SiteVideoCard(
                    title = rel.title,
                    coverUrl = rel.coverUrl,
                    onClick = { onNavigateToAlbum(rel.videoCode) },
                    aspectRatio = 3f / 4f,
                    topEndBadge = rel.duration,
                    imageLoader = imageLoader,
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}
