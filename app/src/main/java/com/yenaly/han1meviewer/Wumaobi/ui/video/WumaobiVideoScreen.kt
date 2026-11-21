package com.yenaly.han1meviewer.Wumaobi.ui.video

import com.yenaly.han1meviewer.ui.screen.rememberVideoGridColumns
import com.yenaly.han1meviewer.ui.component.SiteBackWithHomeButton
import com.yenaly.han1meviewer.util.PlayerOrientation
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.SiteType
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.yenaly.han1meviewer.MissAV.ui.video.SubtitleCue
import com.yenaly.han1meviewer.MissAV.ui.video.SubtitleParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import com.yenaly.han1meviewer.ui.component.SiteVideoActionBar
import com.yenaly.han1meviewer.ui.component.CollapsingPlayerLayout
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.yenaly.han1meviewer.ui.theme.VideoSimplifiedCardMinWidth
import com.yenaly.han1meviewer.ui.theme.SpacingNormal
import com.yenaly.han1meviewer.ui.component.SiteVideoSectionHeader
import com.yenaly.han1meviewer.ui.component.SiteVideoCard
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.MediaSource
import com.yenaly.han1meviewer.ui.component.player.SitePlayerSettings
import com.yenaly.han1meviewer.ui.component.player.PipSupport
import com.yenaly.han1meviewer.ui.component.player.SitePipEffect
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.yenaly.han1meviewer.Wumaobi.data.local.WumaobiHistoryEntity
import com.yenaly.han1meviewer.Wumaobi.data.model.WumaobiDetail
import com.yenaly.han1meviewer.Wumaobi.data.remote.WumaobiNetwork
import com.yenaly.han1meviewer.Wumaobi.data.repository.WumaobiHistoryRepo
import com.yenaly.han1meviewer.Wumaobi.common.WumaobiConstants
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.collectAsState
import com.yenaly.han1meviewer.ui.component.VideoCardItem
import com.yenaly.han1meviewer.ui.screen.rememberCardResponsiveWidth
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.player.createSitePlayer

private const val PLAYER_TAG = "WumaobiPlayer"
private const val RESUME_THRESHOLD_MS = 5_000L
private const val RESUME_CLAMP_BACKOFF_MS = 5_000L
private const val SAVE_INTERVAL_MS = 2_000L
private const val DELTA_GUARD_MS = 10_000L

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

private fun buildWumaobiMediaSource(context: Context, url: String, referer: String): MediaSource {
    val base = referer.ifBlank { WumaobiNetwork.baseUrl + "/" }
    val origin = base.trimEnd('/')
    val http = OkHttpDataSource.Factory(WumaobiNetwork.sharedOkHttpClient)
        .setDefaultRequestProperties(
            hashMapOf(
                "Referer" to base,
                "Origin" to origin,
            )
        )
    val dsFactory = DefaultDataSource.Factory(context, http)
    val mediaItem = MediaItem.Builder().setUri(url).build()
    return if (url.substringBefore('?').contains(".m3u8", ignoreCase = true)) {
        HlsMediaSource.Factory(dsFactory).createMediaSource(mediaItem)
    } else {
        ProgressiveMediaSource.Factory(dsFactory).createMediaSource(mediaItem)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WumaobiVideoScreen(
    videoCode: String,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToList: (String, String) -> Unit,
    onNavigateToAccount: () -> Unit,
    resumePosition: Long = 0L,
    viewModel: WumaobiVideoViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = remember(context) { context.findHostActivity() }

    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val streamState by viewModel.streamState.collectAsStateWithLifecycle()
    val referer by viewModel.referer.collectAsStateWithLifecycle()

    var pendingResume by remember { mutableLongStateOf(0L) }
    var subtitleCues by remember { mutableStateOf<List<SubtitleCue>>(emptyList()) }
    var subtitlesVisible by remember { mutableStateOf(true) }
    var resumeApplied by remember { mutableStateOf(false) }
    var showResumeDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val subtitleText = remember(subtitleCues, subtitlesVisible, playerState.position) {
        if (!subtitlesVisible || subtitleCues.isEmpty()) ""
        else subtitleCues.findLast { playerState.position >= it.startTime }
            ?.takeIf { playerState.position <= it.endTime }?.text.orEmpty()
    }

    fun loadSubtitle(uri: Uri) {
        scope.launch {
            try {
                val cues = withContext(Dispatchers.IO) { SubtitleParser.parseSRT(uri, context) }
                if (cues.isNotEmpty()) {
                    subtitleCues = cues
                    subtitlesVisible = true
                    Toast.makeText(
                        context, context.getString(R.string.site_subtitle_loaded, cues.size),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    subtitleCues = emptyList()
                    Toast.makeText(
                        context, context.getString(R.string.site_subtitle_none), Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                subtitleCues = emptyList()
                Toast.makeText(
                    context,
                    context.getString(R.string.site_subtitle_load_failed, e.message.orEmpty()),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun removeSubtitle() {
        subtitleCues = emptyList()
        Toast.makeText(
            context, context.getString(R.string.site_subtitle_removed), Toast.LENGTH_SHORT
        ).show()
    }

    val subtitlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) loadSubtitle(uri) }
    val exoPlayer = remember { mutableStateOf<ExoPlayer?>(null) }
    val currentMediaUrl = remember { mutableStateOf<String?>(null) }

    WumaobiHistoryRepo.init(context)

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
                        android.view.WindowInsets.Type.statusBars() or
                                android.view.WindowInsets.Type.navigationBars()
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
            player.setMediaSource(buildWumaobiMediaSource(context, url, referer))
            player.prepare()
            player.setPlaybackSpeed(playerState.speed)
            exoPlayer.value = player
            currentMediaUrl.value = url
        } else {
            val wasPlaying = existing.playWhenReady
            val pos = existing.currentPosition
            existing.setMediaSource(buildWumaobiMediaSource(context, url, referer))
            existing.prepare()
            existing.seekTo(pos)
            existing.playWhenReady = wasPlaying
            currentMediaUrl.value = url
        }
    }

    val playerConfig = SitePlayerSettings.config(SiteType.WUMAOBI)
    val inPip = PipSupport.inPip
    SitePipEffect(site = SiteType.WUMAOBI, isPlaying = playerState.isPlaying)
    LaunchedEffect(playerConfig.defaultSpeed) {
        viewModel.setSpeed(playerConfig.defaultSpeed)
    }
    LaunchedEffect(playerState.isReady, playerState.url, playerConfig.autoPlay) {
        if (playerConfig.autoPlay && playerState.isReady && playerState.url.isNotBlank()) {
            delay(500)
            if (!showResumeDialog && !viewModel.playerState.value.isPlaying) {
                viewModel.setPlaying(true)
            }
        }
    }

    LaunchedEffect(videoCode) {
        viewModel.load(videoCode)
        resumeApplied = false
        pendingResume = 0L
    }

    LaunchedEffect(videoCode, resumePosition) {
        val history = withContext(Dispatchers.IO) { WumaobiHistoryRepo.getByVideoCode(videoCode) }
        pendingResume = when {
            resumePosition > 0L -> resumePosition
            history != null && history.lastPosition > RESUME_THRESHOLD_MS &&
                    (history.totalDuration <= 0L ||
                            history.lastPosition < history.totalDuration - RESUME_CLAMP_BACKOFF_MS) ->
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
        val detail = (detailState as? WumaobiDetailState.Success)?.detail ?: return@LaunchedEffect
        if (!playerState.isPlaying) return@LaunchedEffect
        var lastSavedPosition = playerState.position
        while (isActive) {
            delay(SAVE_INTERVAL_MS)
            val pos = playerState.position
            val delta = pos - lastSavedPosition
            lastSavedPosition = pos
            val total = playerState.duration
            if (total > 0L && pos > 0L && delta in 0L..DELTA_GUARD_MS) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        WumaobiHistoryRepo.upsertWatchProgress(
                            videoCode = videoCode,
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
                android.view.WindowInsets.Type.statusBars() or
                        android.view.WindowInsets.Type.navigationBars()
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
                android.view.WindowInsets.Type.statusBars() or
                        android.view.WindowInsets.Type.navigationBars()
            )
        } else {
            @Suppress("DEPRECATION")
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        viewModel.setFullscreen(false)
    }

    BackHandler(enabled = playerState.isFullscreen) { exitFullscreen() }

    val detail = (detailState as? WumaobiDetailState.Success)?.detail

    @Composable
    fun Player(fullscreen: Boolean, modifier: Modifier) {
        WumaobiVideoPlayer(
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
            subtitleText = subtitleText,
        )
    }

    if (playerState.isFullscreen) {
        Dialog(
            onDismissRequest = { exitFullscreen() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            Player(fullscreen = true, modifier = Modifier.fillMaxSize())
        }
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
            is WumaobiDetailState.Loading -> SiteLoadingContent(modifier = Modifier.padding(paddingValues))
            is WumaobiDetailState.Error -> SiteErrorContent(
                message = state.message,
                onRetry = { viewModel.load(videoCode, force = true) },
                modifier = Modifier.padding(paddingValues),
            )

            is WumaobiDetailState.Success -> {
                val info = state.detail
                CollapsingPlayerLayout(
                    pip = inPip,
                    pinEnabled = playerConfig.pinVideo,
                    pinActive = playerState.isPlaying,
                    onPinClose = { viewModel.setPlaying(false) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    player = {
                        if (!playerState.isFullscreen) {
                            Player(fullscreen = false, modifier = Modifier.fillMaxWidth())
                        }
                    },
                    content = {
                        WumaobiVideoContent(
                            info = info,
                            listState = listState,
                            streamState = streamState,
                            playerState = playerState,
                            videoCode = videoCode,
                            viewModel = viewModel,
                            onNavigateToVideo = onNavigateToVideo,
                            onNavigateToSearch = onNavigateToSearch,
                            onNavigateToList = onNavigateToList,
                            onNavigateToAccount = onNavigateToAccount,
                            subtitleCount = subtitleCues.size,
                            subtitlesVisible = subtitlesVisible,
                            onToggleSubtitles = { subtitlesVisible = it },
                            onPickSubtitleFile = { subtitlePicker.launch("*/*") },
                            onRemoveSubtitle = { removeSubtitle() },
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WumaobiVideoContent(
    info: WumaobiDetail,
    listState: androidx.compose.foundation.lazy.LazyListState,
    streamState: WumaobiStreamState,
    playerState: WumaobiPlayerState,
    videoCode: String,
    viewModel: WumaobiVideoViewModel,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToList: (String, String) -> Unit,
    onNavigateToAccount: () -> Unit,
    subtitleCount: Int,
    subtitlesVisible: Boolean,
    onToggleSubtitles: (Boolean) -> Unit,
    onPickSubtitleFile: () -> Unit,
    onRemoveSubtitle: () -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            top = 12.dp,
            end = 12.dp,
            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "quality") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (playerState.qualityOptions.isNotEmpty()) {
                    SiteVideoSectionHeader(title = stringResource(R.string.wumaobi_quality_header))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        playerState.qualityOptions.keys.forEach { label ->
                            FilterChip(
                                selected = label == playerState.quality,
                                onClick = { viewModel.setQuality(label) },
                                label = { Text(label) },
                            )
                        }
                    }
                }
                if (streamState is WumaobiStreamState.Failed) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = streamState.reason,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            TextButton(onClick = { viewModel.retry() }) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                }
            }
        }

        item(key = "actions") {
            SiteVideoActionBar(
                site = SiteType.WUMAOBI,
                videoCode = videoCode,
                title = info.title,
                coverUrl = info.coverUrl,
                webUrl = info.pageUrl,
                downloadUrl = playerState.url.takeIf { it.isNotBlank() },
                downloadQuality = playerState.quality,
                resolveDownloadUrl = {
                    viewModel.playerState.value.url.takeIf { it.isNotBlank() }
                        ?: kotlinx.coroutines.withTimeoutOrNull(20_000L) {
                            viewModel.playerState.first { it.url.isNotBlank() }.url
                        }
                },
            )
        }

        item(key = "favorite") {
            WumaobiFavoriteControl(
                info = info,
                viewModel = viewModel,
                onNavigateToAccount = onNavigateToAccount,
            )
        }

        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                val meta = listOfNotNull(
                    info.date.takeIf { it.isNotBlank() },
                    info.durationSeconds.takeIf { it > 0 }?.let { secs ->
                        val h = secs / 3600
                        val m = (secs % 3600) / 60
                        val sec = secs % 60
                        if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
                    },
                ).joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        val sourceLinks = listOfNotNull(info.channel, info.network)
        if (sourceLinks.isNotEmpty()) {
            item(key = "sources") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    sourceLinks.forEach { link ->
                        AssistChip(
                            onClick = { onNavigateToList(link.title, link.path) },
                            label = { Text(link.title) },
                        )
                    }
                }
            }
        }

        if (info.models.isNotEmpty()) {
            item(key = "models") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SiteVideoSectionHeader(title = stringResource(R.string.wumaobi_models))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        info.models.forEach { model ->
                            AssistChip(
                                onClick = { onNavigateToList(model.title, model.path) },
                                label = { Text(model.title) },
                            )
                        }
                    }
                }
            }
        }

        if (info.categories.isNotEmpty()) {
            item(key = "tags") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    info.categories.forEach { tag ->
                        AssistChip(
                            onClick = { onNavigateToList(tag.title, tag.path) },
                            label = { Text(tag.title) },
                        )
                    }
                }
            }
        }

        item(key = "subtitles") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SiteVideoSectionHeader(title = stringResource(R.string.wumaobi_subtitles))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FilterChip(
                        selected = subtitlesVisible && subtitleCount > 0,
                        onClick = { onToggleSubtitles(!subtitlesVisible) },
                        enabled = subtitleCount > 0,
                        label = { Text(if (subtitleCount > 0) stringResource(R.string.wumaobi_subtitle_show, subtitleCount) else stringResource(R.string.wumaobi_subtitle_none)) },
                    )
                    AssistChip(onClick = onPickSubtitleFile, label = { Text(stringResource(R.string.wumaobi_subtitle_load_srt)) })
                    if (subtitleCount > 0) {
                        AssistChip(onClick = onRemoveSubtitle, label = { Text(stringResource(R.string.remove)) })
                    }
                }
            }
        }

        if (info.related.isNotEmpty()) {
            item(key = "related_header") {
                SiteVideoSectionHeader(title = stringResource(R.string.wumaobi_related))
            }
            item(key = "related_grid") {
                val relatedColumns = rememberVideoGridColumns()
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = relatedColumns,
                ) {
                    info.related.forEach { item ->
                        SiteVideoCard(
                            modifier = Modifier.weight(1f),
                            title = item.title,
                            coverUrl = item.coverUrl,
                            onClick = { onNavigateToVideo(item.videoCode) },
                            aspectRatio = 16f / 9f,
                            duration = item.duration,
                            views = item.views,
                            imageLoader = com.yenaly.han1meviewer.Wumaobi.common.WumaobiImageLoader
                                .get(LocalContext.current),
                        )
                    }
                    repeat((relatedColumns - info.related.size % relatedColumns) % relatedColumns) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
