package com.yenaly.han1meviewer.ui.component.shorts

import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.view.TextureView
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionDisabled
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
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
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.size.Size
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.openSiteWebPage
import com.yenaly.han1meviewer.ui.component.player.SitePlayerSettings
import com.yenaly.han1meviewer.ui.component.shareSiteText
import com.yenaly.han1meviewer.ui.screen.RetryableImage
import com.yenaly.han1meviewer.util.compactCount
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private var rememberedMuted = false

private const val META_WAIT_MS = 8_000L
private const val FIRST_FRAME_FADE_MS = 180
private const val PROGRESS_TICK_MS = 100L

@Stable
private class ShortItemState(val id: String?) {

    var loaded by mutableStateOf(false)
    var failed by mutableStateOf(false)

    var rendered by mutableStateOf(false)
    var videoRatio by mutableFloatStateOf(0f)
    var progress by mutableFloatStateOf(0f)
    var cue by mutableStateOf("")
    var exoState by mutableIntStateOf(Player.STATE_IDLE)
    var isPlaying by mutableStateOf(false)

    var userPaused = false
}

private class ShortsSessionFlags {
    var firstId: String? = null
    var swiped = false
    var handlesAudioFocus: Boolean? = null
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Suppress("DEPRECATION")
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

    onOpenTag: ((SiteShortTag) -> Unit)? = null,

    onOpenOwner: ((SiteShortOwner) -> Unit)? = null,
) {
    val shorts = remember(items) { dedupeShorts(items) }
    if (shorts.isEmpty()) return
    val context = LocalContext.current
    val config = remember(source.site) { SitePlayerSettings.config(source.site) }
    val imageLoader = remember(source) { source.imageLoader(context) }
    val scrim = Color.Black.copy(alpha = config.controlsOpacity.coerceIn(0.35f, 0.6f))
    val scope = rememberCoroutineScope()
    val startIndex = remember { resolveInitialIndex(items.map { it.id }, initialIndex) }
    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, shorts.lastIndex),
        pageCount = { shorts.size },
    )
    val currentItems by rememberUpdatedState(shorts)

    val failedMsg = stringResource(R.string.site_shorts_play_failed)
    val loginRequired = stringResource(R.string.site_shorts_login_required)
    val actionFailed = stringResource(R.string.site_shorts_action_failed)
    val downloadSaved = stringResource(R.string.site_shorts_download_saved)
    val captchaMsg = stringResource(R.string.xh_comment_captcha)
    val postedMsg = stringResource(R.string.xh_comment_posted)
    val loadingDesc = stringResource(R.string.site_shorts_loading)
    val playDesc = stringResource(R.string.site_shorts_play)
    val pauseDesc = stringResource(R.string.site_shorts_pause)
    val retryDesc = stringResource(R.string.retry)

    var showHeart by remember { mutableStateOf(false) }
    var heartShortId by remember { mutableStateOf<String?>(null) }
    var fullscreen by remember { mutableStateOf(false) }
    var heartTick by remember { mutableIntStateOf(0) }
    var muted by remember { mutableStateOf(rememberedMuted) }
    var subtitleLang by remember { mutableStateOf<String?>(null) }
    var showSubtitlePicker by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var commentsFor by remember { mutableStateOf<SiteShort?>(null) }
    var replyTo by remember { mutableStateOf<SiteShortComment?>(null) }
    var sendingComment by remember { mutableStateOf(false) }
    val metas = remember { mutableStateMapOf<String, SiteShortMeta>().also { it.putAll(initialMetas) } }
    val busy = remember { mutableStateMapOf<String, Boolean>() }
    val attempted = remember { mutableSetOf<String>() }
    val inflight = remember { HashMap<String, Job>() }
    val commentStates = remember { mutableStateMapOf<String, ShortsCommentsState>() }
    val retryTokens = remember { mutableStateMapOf<String, Int>() }
    val handledRetries = remember { HashMap<String, Int>() }
    val flags = remember { ShortsSessionFlags() }

    val activeId = shorts.getOrNull(pagerState.currentPage)?.id
    val itemState = remember(activeId) { ShortItemState(activeId) }
    val itemStateRef = rememberUpdatedState(itemState)

    val audioAttributes = remember {
        AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
    }
    val dataSourceFactory = remember(source) {
        val client = source.streamClient
        if (client != null) DefaultDataSource.Factory(context, OkHttpDataSource.Factory(client))
        else DefaultDataSource.Factory(context)
    }
    val mediaSourceFactory = remember(dataSourceFactory) { DefaultMediaSourceFactory(dataSourceFactory) }
    val player = remember(source) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = false
                volume = if (rememberedMuted) 0f else 1f
            }
    }

    DisposableEffect(player) {

        val listener = object : Player.Listener {
            private fun active(): ShortItemState? = itemStateRef.value.takeIf { it.loaded }

            override fun onPlayerError(error: PlaybackException) {
                active()?.let { it.failed = true; it.isPlaying = false }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                active()?.exoState = playbackState
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                active()?.isPlaying = isPlaying
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                active()?.videoRatio = if (videoSize.width > 0 && videoSize.height > 0) {
                    videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                } else 0f
            }

            override fun onRenderedFirstFrame() {
                val st = active() ?: return
                if (player.currentMediaItem?.mediaId == st.id &&
                    player.playbackState != Player.STATE_IDLE
                ) st.rendered = true
            }

            override fun onCues(cueGroup: CueGroup) {
                active()?.cue = cueGroup.cues.mapNotNull { it.text?.toString() }.joinToString("\n")
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.stop()
            player.release()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        var resume = false
        var paused = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> if (!paused) {
                    paused = true
                    resume = player.playWhenReady
                    player.pause()
                }

                Lifecycle.Event.ON_RESUME -> if (paused) {
                    paused = false
                    if (resume && itemStateRef.value.let { it.loaded && !it.failed }) player.play()
                }

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

    LaunchedEffect(muted, player) {
        rememberedMuted = muted
        player.volume = if (muted) 0f else 1f
    }

    LaunchedEffect(itemState, itemState.isPlaying, itemState.loaded) {
        val st = itemState
        fun update() {
            val duration = player.duration
            st.progress = if (st.loaded && duration > 0) {
                (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
            } else 0f
        }
        update()
        while (isActive && st.isPlaying) {
            delay(PROGRESS_TICK_MS)
            update()
        }
    }

    fun requestMetas(from: Int) {
        val batch = currentItems.drop(from).take(6)
            .filter { it.id !in metas && it.id !in attempted && it.id !in inflight }
        if (batch.isEmpty()) return

        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                source.loadMetas(batch).onSuccess {
                    metas.putAll(it)
                    attempted += batch.map { b -> b.id }
                }
            } finally {
                batch.forEach { inflight.remove(it.id) }
            }
        }
        batch.forEach { inflight[it.id] = job }
        job.start()
    }

    val retryNonce = activeId?.let { retryTokens[it] } ?: 0
    LaunchedEffect(activeId, subtitleLang, retryNonce) {
        val id = activeId ?: return@LaunchedEffect
        val st = itemState
        if (st.id != id) return@LaunchedEffect
        val short = currentItems.firstOrNull { it.id == id } ?: return@LaunchedEffect
        if (flags.firstId == null) flags.firstId = id else if (id != flags.firstId) flags.swiped = true

        val sameMedia = st.loaded && !st.failed
        val resumeAt: Long
        var play: Boolean
        if (sameMedia) {

            resumeAt = player.currentPosition
            play = player.playWhenReady
        } else {

            player.pause()
            player.stop()
            player.clearMediaItems()
            st.loaded = false
            st.rendered = false
            st.videoRatio = 0f
            st.progress = 0f
            st.exoState = Player.STATE_IDLE
            st.isPlaying = false
            resumeAt = 0L
            play = false
        }
        st.cue = ""
        st.failed = false
        heartShortId = heartShortId?.takeIf { it == id }

        if (flags.handlesAudioFocus != !short.isGif) {
            flags.handlesAudioFocus = !short.isGif
            player.setAudioAttributes(audioAttributes, !short.isGif)
        }

        val idx = currentItems.indexOfFirst { it.id == id }
        if (retryNonce > (handledRetries[id] ?: 0)) {

            handledRetries[id] = retryNonce
            attempted.remove(id)
            withTimeoutOrNull(META_WAIT_MS) {
                source.loadMetas(listOf(short)).onSuccess {
                    metas.putAll(it)
                    attempted += id
                }
            }
        } else {
            requestMetas(idx.coerceAtLeast(0))
            if (metas[id] == null) {
                inflight[id]?.let { job -> withTimeoutOrNull(META_WAIT_MS) { job.join() } }
            }
        }

        if (!sameMedia) {
            play = shouldAutoPlayItem(config.autoPlay, flags.swiped) && !st.userPaused
        }

        val meta = metas[id]
        val url = (meta?.streamUrl ?: short.videoUrl).trim()
        val mediaItem = if (url.isEmpty()) null else runCatching {
            val track = subtitleLang?.let { lang -> meta?.subtitles?.firstOrNull { it.lang == lang } }
            MediaItem.Builder()
                .setMediaId(id)
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
        }.getOrNull()
        if (mediaItem == null) {
            st.failed = true
            return@LaunchedEffect
        }

        player.volume = if (muted) 0f else 1f
        player.setMediaItem(mediaItem)
        player.playWhenReady = play
        player.prepare()
        if (resumeAt > 0) player.seekTo(resumeAt)

        st.exoState = player.playbackState
        st.isPlaying = player.isPlaying
        st.loaded = true
    }

    val coverLoader = remember(imageLoader) { imageLoader ?: SingletonImageLoader.get(context) }
    val prefetchedCovers = remember { HashSet<String>() }
    LaunchedEffect(activeId) {
        val idx = currentItems.indexOfFirst { it.id == activeId }
        if (idx < 0) return@LaunchedEffect
        val metrics = context.resources.displayMetrics
        for (offset in intArrayOf(1, -1, 2)) {
            val url = currentItems.getOrNull(idx + offset)?.coverUrl.orEmpty()
            if (url.isBlank() || !prefetchedCovers.add(url)) continue
            coverLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(url)
                    .size(Size(metrics.widthPixels, metrics.heightPixels))
                    .memoryCacheKey("$url#r0")
                    .diskCacheKey("$url#r0")
                    .build()
            )
        }
    }

    var lastLoadMoreSize by remember { mutableIntStateOf(-1) }
    LaunchedEffect(pagerState.currentPage, shorts.size, hasMore) {
        if (pagerState.currentPage < shorts.size - 3) lastLoadMoreSize = -1
        if (shouldRequestLoadMore(hasMore, pagerState.currentPage, shorts.size, lastLoadMoreSize)) {
            lastLoadMoreSize = shorts.size
            onLoadMore()
        }
    }
    LaunchedEffect(itemState, itemState.failed) {
        if (itemState.failed) GlobalToasts.show(failedMsg, level = GlobalToasts.ToastLevel.ERROR)
    }

    fun retryActive() {
        val id = activeId ?: return
        retryTokens[id] = (retryTokens[id] ?: 0) + 1
    }

    fun togglePlayback() {
        val st = itemStateRef.value
        when {
            st.failed -> retryActive()
            !st.loaded -> st.userPaused = !st.userPaused
            player.playbackState == Player.STATE_ENDED -> {
                player.seekToDefaultPosition()
                player.play()
            }

            player.playWhenReady -> player.pause()
            else -> player.play()
        }
    }

    val handleBack: () -> Unit = {
        when {
            commentsFor != null -> commentsFor = null
            fullscreen -> fullscreen = false
            else -> onDismiss()
        }
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

    fun downloadCurrent(short: SiteShort) {
        val creator = metas[short.id]?.owner?.name
        scope.launch {
            source.downloadShort(context, short, creator)
                .onSuccess { GlobalToasts.show(downloadSaved, level = GlobalToasts.ToastLevel.SUCCESS) }
                .onFailure { toastError(it) }
        }
    }

    fun followOwner(short: SiteShort) {
        val cur = metas[short.id]?.owner ?: return
        if (cur.subscribeBusy) return
        if (!source.isLoggedIn) {
            GlobalToasts.show(loginRequired, level = GlobalToasts.ToastLevel.ERROR)
            return
        }
        val want = !cur.subscribed
        fun updateOwner(transform: (SiteShortOwner) -> SiteShortOwner) {
            metas.keys.toList().forEach { k ->
                val m = metas[k]
                val o = m?.owner
                if (m != null && o != null && o.name == cur.name) metas[k] = m.copy(owner = transform(o))
            }
        }
        updateOwner { it.copy(subscribeBusy = true) }
        scope.launch {
            source.setOwnerFollowed(short, cur, want)
                .onSuccess {
                    val delta = if (want) 1L else -1L
                    updateOwner { o ->
                        o.copy(
                            subscribed = want,
                            subscribeBusy = false,
                            subscribers = o.subscribers?.let { s -> (s + delta).coerceAtLeast(0L) },
                        )
                    }
                }
                .onFailure {
                    updateOwner { o -> o.copy(subscribeBusy = false) }
                    toastError(it)
                }
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

    val cutoutSides = WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)

    Dialog(
        onDismissRequest = handleBack,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val dialogView = LocalView.current
        val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window

        DisposableEffect(dialogWindow) {
            dialogWindow?.let { w ->
                w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                w.setBackgroundDrawable(ColorDrawable(android.graphics.Color.BLACK))
                WindowCompat.setDecorFitsSystemWindows(w, false)
                w.statusBarColor = android.graphics.Color.TRANSPARENT
                w.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    w.attributes = w.attributes.also {
                        it.layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    w.isNavigationBarContrastEnforced = false
                    w.isStatusBarContrastEnforced = false
                }
                WindowCompat.getInsetsController(w, dialogView).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
            onDispose { }
        }
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
        val fling = PagerDefaults.flingBehavior(
            state = pagerState,
            pagerSnapDistance = PagerSnapDistance.atMost(1),
        )
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSize = PageSize.Fill,

                beyondViewportPageCount = 1,
                key = { index -> shorts.getOrNull(index)?.id ?: index },
                flingBehavior = fling,
                userScrollEnabled = commentsFor == null,
            ) { page ->
                val short = shorts.getOrNull(page) ?: return@VerticalPager
                val meta = metas[short.id]
                val pageOwner = owner ?: meta?.owner
                val isActive = pagerState.currentPage == page
                val st = itemState.takeIf { isActive && it.id == short.id }
                val videoAlpha = animateFloatAsState(
                    targetValue = if (st != null && st.rendered) 1f else 0f,
                    animationSpec = tween(FIRST_FRAME_FADE_MS),
                    label = "shortVideoAlpha",
                )
                Box(
                    Modifier.fillMaxSize().background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {

                    if (short.coverUrl.isNotBlank()) {
                        RetryableImage(
                            model = short.coverUrl,
                            modifier = Modifier.fillMaxSize().graphicsLayer {
                                alpha = if (videoAlpha.value >= 1f) 0f else 1f
                            },
                            placeholder = painterResource(R.drawable.h_chan_loading),
                            error = painterResource(R.drawable.h_chan_load_failed),
                            contentScale = ContentScale.Fit,
                            imageLoader = imageLoader,
                        )
                    }
                    if (st != null) {
                        val ui = resolveShortsPlaybackUi(st.loaded, st.failed, st.exoState, st.isPlaying)
                        var spinnerVisible by remember(st) { mutableStateOf(true) }
                        LaunchedEffect(st, ui, st.rendered) {
                            if (ui == ShortsPlaybackUi.Loading) {
                                val wait = shortsSpinnerDelayMs(st.rendered)
                                if (wait > 0) delay(wait)
                                spinnerVisible = true
                            } else {
                                spinnerVisible = false
                            }
                        }
                        val stateDesc = when {
                            ui == ShortsPlaybackUi.Loading -> loadingDesc
                            ui == ShortsPlaybackUi.Failed -> failedMsg
                            ui == ShortsPlaybackUi.Playing -> pauseDesc
                            else -> playDesc
                        }

                        Box(
                            Modifier.fillMaxSize()
                                .semantics(mergeDescendants = true) {
                                    contentDescription = stateDesc
                                    onClick(label = if (ui == ShortsPlaybackUi.Failed) retryDesc else stateDesc) {
                                        togglePlayback()
                                        true
                                    }
                                }
                                .pointerInput(short.id) {
                                    detectTapGestures(
                                        onTap = { if (!pagerState.isScrollInProgress) togglePlayback() },
                                        onDoubleTap = {
                                            if (source.supportsRating && !pagerState.isScrollInProgress) {
                                                heartShortId = short.id
                                                heartTick++
                                                likeShort(short, onlyLike = true)
                                            }
                                        },
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {

                            if (st.loaded) {
                                AndroidView(
                                    modifier = (if (st.videoRatio > 0f) {
                                        Modifier.aspectRatio(st.videoRatio, matchHeightConstraintsFirst = false)
                                    } else Modifier.fillMaxSize()).graphicsLayer {
                                        alpha = videoAlpha.value
                                    },
                                    factory = { ctx ->
                                        TextureView(ctx).also {
                                            it.keepScreenOn = config.keepScreenOn
                                            player.setVideoTextureView(it)
                                        }
                                    },
                                    onRelease = { player.clearVideoTextureView(it) },
                                )
                            }
                        }
                        AnimatedVisibility(
                            visible = spinnerVisible && ui == ShortsPlaybackUi.Loading,
                            enter = fadeIn(tween(120)),
                            exit = fadeOut(tween(120)),
                        ) {
                            Box(
                                Modifier.size(68.dp)
                                    .background(Color.Black.copy(alpha = 0.38f), CircleShape)
                                    .semantics { contentDescription = loadingDesc },
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = Color.White,
                                    trackColor = Color.Transparent,
                                    strokeWidth = 3.dp,
                                )
                            }
                        }
                        AnimatedVisibility(
                            visible = (ui == ShortsPlaybackUi.Paused || ui == ShortsPlaybackUi.Ended) &&
                                    !spinnerVisible && !fullscreen,
                            enter = fadeIn() + scaleIn(initialScale = 0.8f),
                            exit = fadeOut(),
                        ) {
                            Box(
                                Modifier.size(68.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.38f))
                                    .clickable(role = Role.Button, onClickLabel = playDesc) { togglePlayback() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_baseline_play_arrow_24), playDesc,
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp),
                                )
                            }
                        }
                        AnimatedVisibility(
                            visible = ui == ShortsPlaybackUi.Failed,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    Modifier.size(68.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.38f))
                                        .clickable(role = Role.Button, onClickLabel = retryDesc) { retryActive() },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Filled.Refresh, retryDesc,
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp),
                                    )
                                }
                                Text(
                                    failedMsg,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        shadow = ShortsBadgeDefaults.TextShadow,
                                    ),
                                )
                            }
                        }
                        AnimatedVisibility(
                            visible = showHeart && heartShortId == short.id,
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
                        if (st.cue.isNotBlank()) {
                            Text(
                                st.cue,
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
                                .navigationBarsPadding()
                                .windowInsetsPadding(cutoutSides),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (pageOwner != null) {
                                ShortsOwnerRow(
                                    owner = pageOwner,
                                    imageLoader = imageLoader,
                                    scrim = scrim,
                                    onSubscribe = { if (owner != null) onToggleSubscribe() else followOwner(short) },
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
                            canDislike = source.supportsDislike,
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
                                .navigationBarsPadding()
                                .windowInsetsPadding(cutoutSides),
                        )
                    }
                }
            }

            val currentShort = shorts.getOrNull(pagerState.currentPage)
            if (!fullscreen) Row(
                Modifier.align(Alignment.TopStart).fillMaxWidth().statusBarsPadding()
                    .windowInsetsPadding(cutoutSides)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ShortsCircleButton(onClick = handleBack, scrim = scrim) {
                    Icon(
                        painterResource(R.drawable.ic_baseline_arrow_back_24),
                        stringResource(R.string.back), tint = Color.White,
                    )
                }
                Spacer(Modifier.weight(1f))
                val subtitles = currentShort?.let { metas[it.id] }?.subtitles.orEmpty()
                if (subtitles.isNotEmpty()) {
                    val subtitleOn = subtitleLang != null && subtitles.any { it.lang == subtitleLang }
                    ShortsCircleButton(onClick = { showSubtitlePicker = true }, scrim = scrim) {
                        Icon(
                            if (subtitleOn) Icons.Filled.ClosedCaption else Icons.Filled.ClosedCaptionDisabled,
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
                if (currentShort != null && source.supportsDownload) {
                    ShortsCircleButton(onClick = { downloadCurrent(currentShort) }, scrim = scrim) {
                        Icon(
                            Icons.Filled.Download,
                            stringResource(R.string.site_shorts_download), tint = Color.White,
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
                    Modifier.fillMaxHeight().fillMaxWidth()
                        .graphicsLayer {
                            scaleX = itemState.progress.coerceIn(0f, 1f)
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        }
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
        val tracks = shorts.getOrNull(pagerState.currentPage)?.let { metas[it.id] }?.subtitles.orEmpty()
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
                shorts.getOrNull(pagerState.currentPage)
                    ?.let { source.reportUrl(it) }
                    ?.let { openSiteWebPage(context, it) }
            },
            onDismiss = { showReport = false },
        )
    }
}
