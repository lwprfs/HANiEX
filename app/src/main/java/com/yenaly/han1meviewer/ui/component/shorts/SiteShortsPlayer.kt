package com.yenaly.han1meviewer.ui.component.shorts

import android.net.Uri
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionDisabled
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.openSiteWebPage
import com.yenaly.han1meviewer.ui.component.player.SitePlayerSettings
import com.yenaly.han1meviewer.ui.component.shareSiteText
import com.yenaly.han1meviewer.ui.screen.RetryableImage
import com.yenaly.han1meviewer.util.compactCount
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private var rememberedMuted = false

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun SiteShortsPlayer(
    source: SiteShortsSource,
    items: List<SiteShort>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    hasMore: Boolean = false,
    onLoadMore: () -> Unit = {},
    owner: SiteShortOwner? = null,
    onToggleSubscribe: () -> Unit = {},
    initialMetas: Map<String, SiteShortMeta> = emptyMap(),
    /** When set, the short's tags are shown as tappable chips and this is called on tap. */
    onOpenTag: ((SiteShortTag) -> Unit)? = null,
    /** When set, tapping the uploader/channel (that has a [SiteShortOwner.path]) calls this. */
    onOpenOwner: ((SiteShortOwner) -> Unit)? = null,
) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    val config = remember(source.site) { SitePlayerSettings.config(source.site) }
    val imageLoader = remember(source) { source.imageLoader(context) }
    val scrim = Color.Black.copy(alpha = config.controlsOpacity.coerceIn(0.35f, 0.6f))
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, items.lastIndex),
        pageCount = { items.size },
    )

    val failedMsg = stringResource(R.string.site_shorts_play_failed)
    val loginRequired = stringResource(R.string.site_shorts_login_required)
    val actionFailed = stringResource(R.string.site_shorts_action_failed)
    val captchaMsg = stringResource(R.string.xh_comment_captcha)
    val postedMsg = stringResource(R.string.xh_comment_posted)

    var failed by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }
    var showHeart by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(false) }
    var heartTick by remember { mutableIntStateOf(0) }
    var muted by remember { mutableStateOf(rememberedMuted) }
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    // Real width/height of the playing video (0 = not known yet); used to letterbox instead of stretch.
    var videoRatio by remember { mutableFloatStateOf(0f) }
    var subtitleLang by remember { mutableStateOf<String?>(null) }
    var cueText by remember { mutableStateOf("") }
    var showSubtitlePicker by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var commentsFor by remember { mutableStateOf<SiteShort?>(null) }
    var replyTo by remember { mutableStateOf<SiteShortComment?>(null) }
    var sendingComment by remember { mutableStateOf(false) }
    val metas = remember { mutableStateMapOf<String, SiteShortMeta>().also { it.putAll(initialMetas) } }
    val busy = remember { mutableStateMapOf<String, Boolean>() }
    val attempted = remember { mutableSetOf<String>() }
    val commentStates = remember { mutableStateMapOf<String, ShortsCommentsState>() }

    val player = remember {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = config.autoPlay
                volume = if (rememberedMuted) 0f else 1f
            }
    }
    val dataSourceFactory = remember(source) {
        val client = source.streamClient
        if (client != null) DefaultDataSource.Factory(context, OkHttpDataSource.Factory(client))
        else DefaultDataSource.Factory(context)
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                failed = true
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                videoRatio = if (videoSize.width > 0 && videoSize.height > 0) {
                    videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                } else 0f
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onCues(cueGroup: CueGroup) {
                cueText = cueGroup.cues.mapNotNull { it.text?.toString() }.joinToString("\n")
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        var resume = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    resume = player.playWhenReady
                    player.pause()
                }

                Lifecycle.Event.ON_RESUME -> if (resume) player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(heartTick) {
        if (heartTick > 0) {
            showHeart = true
            delay(650)
            showHeart = false
        }
    }

    LaunchedEffect(muted) {
        rememberedMuted = muted
        player.volume = if (muted) 0f else 1f
    }

    LaunchedEffect(player) {
        while (true) {
            val duration = player.duration
            progress = if (duration > 0) (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f
            delay(250)
        }
    }

    val currentItems by rememberUpdatedState(items)
    var loadedShortId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pagerState.currentPage, subtitleLang) {
        val page = pagerState.currentPage
        val short = currentItems.getOrNull(page) ?: return@LaunchedEffect
        val sameShort = loadedShortId == short.id
        val resumeAt = if (sameShort) player.currentPosition else 0L
        if (!sameShort) {
            player.pause()
            player.stop()
            player.clearMediaItems()
            failed = false
            progress = 0f
            cueText = ""
            videoRatio = 0f
        }

        if (metas[short.id] == null) {
            val batch = currentItems.drop(page).take(6).filter { it.id !in metas && it.id !in attempted }
            if (batch.isNotEmpty()) {
                attempted += batch.map { it.id }
                source.loadMetas(batch)
                    .onSuccess { metas.putAll(it) }
                    .onFailure { attempted -= batch.map { b -> b.id }.toSet() }
            }
        }

        val meta = metas[short.id]
        val url = meta?.streamUrl ?: short.videoUrl
        val track = subtitleLang?.let { lang -> meta?.subtitles?.firstOrNull { it.lang == lang } }
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .apply {
                if (url.contains(".m3u8")) setMimeType(MimeTypes.APPLICATION_M3U8)
                if (track != null) {
                    setSubtitleConfigurations(
                        listOf(
                            MediaItem.SubtitleConfiguration.Builder(Uri.parse(track.url))
                                .setMimeType(MimeTypes.TEXT_VTT)
                                .setLanguage(track.lang)
                                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                                .build()
                        )
                    )
                }
            }
            .build()
        player.volume = if (muted) 0f else 1f
        player.setMediaSource(DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem))
        player.prepare()
        if (resumeAt > 0) player.seekTo(resumeAt)
        if (started || config.autoPlay) player.play() else player.pause()
        started = true
        loadedShortId = short.id
    }
    LaunchedEffect(pagerState.currentPage, items.size, hasMore) {
        if (hasMore && pagerState.currentPage >= items.size - 3) onLoadMore()
    }
    LaunchedEffect(failed) {
        if (failed) GlobalToasts.show(failedMsg, level = GlobalToasts.ToastLevel.ERROR)
    }

    fun toastError(t: Throwable) {
        GlobalToasts.show(
            if (t.message == SiteShortsSource.LOGIN_REQUIRED) loginRequired else (t.message ?: actionFailed),
            level = GlobalToasts.ToastLevel.ERROR,
        )
    }

    fun withMeta(short: SiteShort, key: String, block: suspend (SiteShortMeta) -> Unit) {
        if (busy[key + short.id] == true) return
        busy[key + short.id] = true
        scope.launch {
            block(metas[short.id] ?: SiteShortMeta())
            busy[key + short.id] = false
        }
    }

    fun likeShort(short: SiteShort, onlyLike: Boolean) {
        withMeta(short, "like") { cur ->
            if (onlyLike && cur.liked) return@withMeta
            val result = if (cur.liked) source.undoRate(short) else source.rate(short, true)
            result.onSuccess {
                metas[short.id] = if (cur.liked) cur.copy(
                    liked = false, likes = (cur.likes - 1).coerceAtLeast(0),
                ) else cur.copy(
                    liked = true, likes = cur.likes + 1,
                    disliked = false,
                    dislikes = if (cur.disliked) (cur.dislikes - 1).coerceAtLeast(0) else cur.dislikes,
                )
            }.onFailure(::toastError)
        }
    }

    fun dislikeShort(short: SiteShort) {
        withMeta(short, "dislike") { cur ->
            val result = if (cur.disliked) source.undoRate(short) else source.rate(short, false)
            result.onSuccess {
                metas[short.id] = if (cur.disliked) cur.copy(
                    disliked = false, dislikes = (cur.dislikes - 1).coerceAtLeast(0),
                ) else cur.copy(
                    disliked = true, dislikes = cur.dislikes + 1,
                    liked = false,
                    likes = if (cur.liked) (cur.likes - 1).coerceAtLeast(0) else cur.likes,
                )
            }.onFailure(::toastError)
        }
    }

    fun favoriteShort(short: SiteShort) {
        withMeta(short, "love") { cur ->
            source.setFavorite(short, !cur.favorite)
                .onSuccess { metas[short.id] = cur.copy(favorite = !cur.favorite) }
                .onFailure(::toastError)
        }
    }

    fun loadComments(short: SiteShort, more: Boolean) {
        val current = commentStates[short.id] ?: ShortsCommentsState()
        if (current.loading) return
        if (more && current.cursor == null) return
        commentStates[short.id] = current.copy(loading = true, error = null)
        scope.launch {
            source.comments(short, if (more) current.cursor else null)
                .onSuccess { page ->
                    val known = if (more) current.items else emptyList()
                    commentStates[short.id] = ShortsCommentsState(
                        items = (known + page.items).distinctBy { it.id },
                        cursor = page.nextCursor,
                        loaded = true,
                        total = page.total,
                    )
                }
                .onFailure {
                    commentStates[short.id] = current.copy(
                        loading = false, loaded = true, error = it.message ?: actionFailed,
                    )
                }
        }
    }

    fun sendComment(short: SiteShort, text: String) {
        if (sendingComment) return
        sendingComment = true
        val target = replyTo
        scope.launch {
            when (val result = source.postComment(short, text, target)) {
                SiteShortPostResult.Posted -> {
                    GlobalToasts.show(postedMsg, level = GlobalToasts.ToastLevel.SUCCESS)
                    replyTo = null
                    metas[short.id]?.let { metas[short.id] = it.copy(commentsCount = it.commentsCount + 1) }
                    commentStates.remove(short.id)
                    loadComments(short, false)
                }

                SiteShortPostResult.LoginRequired ->
                    GlobalToasts.show(loginRequired, level = GlobalToasts.ToastLevel.ERROR)

                SiteShortPostResult.CaptchaRequired ->
                    GlobalToasts.show(captchaMsg, level = GlobalToasts.ToastLevel.WARNING)

                is SiteShortPostResult.Rejected ->
                    GlobalToasts.show(result.message ?: actionFailed, level = GlobalToasts.ToastLevel.ERROR)
            }
            sendingComment = false
        }
    }

    fun likeComment(short: SiteShort, c: SiteShortComment) {
        if (!source.isLoggedIn) {
            GlobalToasts.show(loginRequired, level = GlobalToasts.ToastLevel.ERROR)
        } else if (!c.likedByMe) scope.launch {
            source.likeComment(short, c).onSuccess {
                val cur = commentStates[short.id] ?: return@onSuccess
                commentStates[short.id] = cur.copy(
                    items = cur.items.map {
                        if (it.id == c.id) it.copy(likedByMe = true, likes = it.likes + 1) else it
                    },
                )
            }.onFailure(::toastError)
        }
    }

    fun deleteComment(short: SiteShort, c: SiteShortComment) {
        scope.launch {
            source.deleteComment(short, c).onSuccess {
                val cur = commentStates[short.id] ?: return@onSuccess
                commentStates[short.id] = cur.copy(items = cur.items.filter { it.id != c.id })
            }.onFailure(::toastError)
        }
    }

    Dialog(
        onDismissRequest = {
            when {
                commentsFor != null -> commentsFor = null
                fullscreen -> fullscreen = false
                else -> onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val dialogView = LocalView.current
        val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
        DisposableEffect(dialogWindow, fullscreen) {
            if (dialogWindow != null) {
                val controller = WindowCompat.getInsetsController(dialogWindow, dialogView)
                if (fullscreen) {
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }
            onDispose {
                dialogWindow?.let {
                    WindowCompat.getInsetsController(it, dialogView)
                        .show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 0,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    pagerSnapDistance = PagerSnapDistance.atMost(1),
                ),
                userScrollEnabled = commentsFor == null,
            ) { page ->
                val short = items[page]
                val meta = metas[short.id]
                val pageOwner = owner ?: meta?.owner
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    RetryableImage(
                        model = short.coverUrl,
                        modifier = Modifier.fillMaxSize(),
                        placeholder = painterResource(R.drawable.h_chan_loading),
                        error = painterResource(R.drawable.h_chan_load_failed),
                        contentScale = ContentScale.Fit,
                        imageLoader = imageLoader,
                    )
                    if (pagerState.currentPage == page) {
                        // Gestures live on the full-screen wrapper so the letterbox bars are tappable too,
                        // while the video surface itself keeps its real aspect ratio (never stretched).
                        Box(
                            Modifier.fillMaxSize().pointerInput(short.id) {
                                detectTapGestures(
                                    onTap = { if (player.isPlaying) player.pause() else player.play() },
                                    onDoubleTap = {
                                        if (source.supportsRating) {
                                            heartTick++
                                            likeShort(short, onlyLike = true)
                                        }
                                    },
                                )
                            },
                            contentAlignment = Alignment.Center,
                        ) {
                            AndroidView(
                                modifier = if (videoRatio > 0f) {
                                    Modifier.aspectRatio(videoRatio, matchHeightConstraintsFirst = false)
                                } else Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    TextureView(ctx).also {
                                        it.keepScreenOn = config.keepScreenOn
                                        player.setVideoTextureView(it)
                                    }
                                },
                                onRelease = { player.clearVideoTextureView(it) },
                            )
                        }
                        AnimatedVisibility(
                            visible = !isPlaying && !fullscreen,
                            enter = fadeIn() + scaleIn(initialScale = 0.8f),
                            exit = fadeOut(),
                        ) {
                            Box(
                                Modifier.size(68.dp).background(Color.Black.copy(alpha = 0.38f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_baseline_play_arrow_24), null,
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp),
                                )
                            }
                        }
                        AnimatedVisibility(
                            visible = heartTick > 0 && showHeart,
                            enter = scaleIn(initialScale = 0.4f) + fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_baseline_favorite_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(110.dp),
                            )
                        }
                        if (cueText.isNotBlank()) {
                            Text(
                                cueText,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(
                                        start = 24.dp,
                                        end = if (fullscreen) 24.dp else 84.dp,
                                        bottom = if (fullscreen) 48.dp else 200.dp,
                                    )
                                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }

                    if (!fullscreen) {
                        Box(
                            Modifier.align(Alignment.TopCenter).fillMaxWidth().height(120.dp).background(
                                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent))
                            ),
                        )
                        Box(
                            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(280.dp).background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.55f to Color.Black.copy(alpha = 0.35f),
                                    1f to Color.Black.copy(alpha = 0.78f),
                                )
                            ),
                        )

                        Column(
                            Modifier.align(Alignment.BottomStart)
                                .padding(start = 14.dp, end = 84.dp, bottom = 22.dp)
                                .navigationBarsPadding(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (pageOwner != null) {
                                ShortsOwnerRow(
                                    owner = pageOwner,
                                    imageLoader = imageLoader,
                                    scrim = scrim,
                                    onSubscribe = onToggleSubscribe,
                                    onClick = if (onOpenOwner != null && pageOwner.path != null) {
                                        { onOpenOwner(pageOwner) }
                                    } else null,
                                )
                            }
                            if (short.title.isNotBlank()) {
                                Text(
                                    short.title, color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        shadow = ShortsBadgeDefaults.TextShadow,
                                    ),
                                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                                )
                            }
                            val views = short.views.compactCount()
                            if (short.isGif || views != null || short.durationSeconds != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    // Same badges as the grid thumbnails, so the two always match.
                                    if (short.isGif) ShortsGifBadge()
                                    ShortsViewsBadge(views)
                                    ShortsDurationBadge(short.durationSeconds)
                                }
                            }
                            val tags = meta?.tags.orEmpty()
                            if (onOpenTag != null && tags.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    contentPadding = PaddingValues(end = 12.dp),
                                ) {
                                    tags.forEach { tag ->
                                        item(key = tag.path) {
                                            ShortsTagChip(tag, onClick = { onOpenTag(tag) })
                                        }
                                    }
                                }
                            }
                        }

                        ShortsActionRail(
                            meta = meta,
                            canRate = source.supportsRating,
                            canFavorite = source.supportsFavorite,
                            canComment = source.supportsComments,
                            canShare = short.pageUrl.isNotBlank(),
                            scrim = scrim,
                            onLike = { likeShort(short, onlyLike = false) },
                            onDislike = { dislikeShort(short) },
                            onFavorite = { favoriteShort(short) },
                            onComments = {
                                commentsFor = short
                                if (commentStates[short.id]?.loaded != true) loadComments(short, false)
                            },
                            onShare = { shareSiteText(context, "${short.title}\n${short.pageUrl}", short.title) },
                            modifier = Modifier.align(Alignment.BottomEnd)
                                .padding(end = 8.dp, bottom = 22.dp)
                                .navigationBarsPadding(),
                        )
                    }
                }
            }

            val currentShort = items.getOrNull(pagerState.currentPage)
            if (!fullscreen) Row(
                Modifier.align(Alignment.TopStart).fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ShortsCircleButton(onClick = onDismiss, scrim = scrim) {
                    Icon(
                        painterResource(R.drawable.ic_baseline_arrow_back_24),
                        stringResource(R.string.back), tint = Color.White,
                    )
                }
                Spacer(Modifier.weight(1f))
                val subtitles = currentShort?.let { metas[it.id] }?.subtitles.orEmpty()
                if (subtitles.isNotEmpty()) {
                    ShortsCircleButton(onClick = { showSubtitlePicker = true }, scrim = scrim) {
                        Icon(
                            if (subtitleLang != null) Icons.Filled.ClosedCaption else Icons.Filled.ClosedCaptionDisabled,
                            stringResource(R.string.site_shorts_subtitles), tint = Color.White,
                        )
                    }
                }
                if (currentShort != null && source.reportUrl(currentShort) != null) {
                    ShortsCircleButton(onClick = { showReport = true }, scrim = scrim) {
                        Icon(
                            painterResource(R.drawable.ic_baseline_report_24),
                            stringResource(R.string.site_shorts_report_title), tint = Color.White,
                        )
                    }
                }
                ShortsCircleButton(onClick = { fullscreen = true }, scrim = scrim) {
                    Icon(
                        Icons.Filled.Fullscreen,
                        stringResource(R.string.site_fullscreen), tint = Color.White,
                    )
                }
                if (currentShort?.isGif != true) {
                    ShortsCircleButton(onClick = { muted = !muted }, scrim = scrim) {
                        Icon(
                            if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            stringResource(
                                if (muted) R.string.site_shorts_unmute else R.string.site_shorts_mute
                            ),
                            tint = Color.White,
                        )
                    }
                }
            }

            if (!fullscreen) Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .navigationBarsPadding().height(2.dp)
                    .background(Color.White.copy(alpha = 0.18f)),
            ) {
                Box(
                    Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f))
                        .background(Color.White),
                )
            }

            commentsFor?.let { short ->
                val state = commentStates[short.id] ?: ShortsCommentsState()
                val meta = metas[short.id]
                ShortsCommentsSheet(
                    state = state,
                    fallbackCount = meta?.commentsCount ?: state.items.size.toLong(),
                    commentsDisabled = meta?.commentsDisabled == true,
                    canPost = source.isLoggedIn,
                    replyingTo = replyTo?.author,
                    sending = sendingComment,
                    imageLoader = imageLoader,
                    onClose = { commentsFor = null },
                    onRetry = { commentStates.remove(short.id); loadComments(short, false) },
                    onLoadMore = { loadComments(short, true) },
                    onReply = { replyTo = it },
                    onCancelReply = { replyTo = null },
                    onSend = { sendComment(short, it) },
                    onLikeComment = { likeComment(short, it) },
                    onDeleteComment = { deleteComment(short, it) },
                    onReportComment = { showReport = true },
                    onNeedLogin = {
                        GlobalToasts.show(loginRequired, level = GlobalToasts.ToastLevel.ERROR)
                    },
                )
            }
        }
    }

    if (showSubtitlePicker) {
        val tracks = items.getOrNull(pagerState.currentPage)?.let { metas[it.id] }?.subtitles.orEmpty()
        ShortsSubtitleDialog(
            tracks = tracks,
            selectedLang = subtitleLang,
            onSelect = { subtitleLang = it; showSubtitlePicker = false },
            onDismiss = { showSubtitlePicker = false },
        )
    }

    if (showReport) {
        ShortsReportDialog(
            onConfirm = {
                showReport = false
                items.getOrNull(pagerState.currentPage)
                    ?.let { source.reportUrl(it) }
                    ?.let { openSiteWebPage(context, it) }
            },
            onDismiss = { showReport = false },
        )
    }
}
