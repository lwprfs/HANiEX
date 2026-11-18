package com.yenaly.han1meviewer.XHamster.ui.shorts

import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionDisabled
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.text.CueGroup
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.XHamster.common.XHamsterFormat
import com.yenaly.han1meviewer.XHamster.common.XHamsterImageLoader
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShort
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShortComment
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShortMeta
import com.yenaly.han1meviewer.XHamster.data.remote.XHamsterMomentsRepo
import com.yenaly.han1meviewer.XHamster.data.remote.XHamsterNetwork
import com.yenaly.han1meviewer.XHamster.data.remote.XHamsterRepo
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.screen.RetryableImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class CommentsState(
    val items: List<XHamsterShortComment> = emptyList(),
    val cursor: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val loaded: Boolean = false,
)

@Composable
private fun RailButton(
    onClick: () -> Unit,
    label: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(46.dp).background(Color.Black.copy(alpha = 0.45f), CircleShape),
        ) { content() }
        if (label != null) {
            Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun XHamsterShortsPlayer(
    items: List<XHamsterShort>,
    initialIndex: Int,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit,
    referer: String,
    ownerName: String?,
    ownerAvatar: String?,
    canSubscribe: Boolean = false,
    subscribed: Boolean = false,
    onToggleSubscribe: () -> Unit = {},
    initialMetas: Map<Long, XHamsterShortMeta> = emptyMap(),
) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    val imageLoader = XHamsterImageLoader.get(context)
    val pagerState: PagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, items.lastIndex),
        pageCount = { items.size },
    )
    var failed by remember { mutableStateOf(false) }
    val failedMsg = stringResource(R.string.xhamster_shorts_play_failed)
    val loginRequired = stringResource(R.string.xhamster_login_required)
    val scope = rememberCoroutineScope()

    var muted by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val metas = remember { mutableStateMapOf<Long, XHamsterShortMeta>().also { it.putAll(initialMetas) } }
    val busy = remember { mutableStateMapOf<String, Boolean>() }

    var subtitleLang by remember { mutableStateOf<String?>(null) }
    var cueText by remember { mutableStateOf("") }
    var showSubtitlePicker by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var commentsFor by remember { mutableStateOf<XHamsterShort?>(null) }
    val commentStates = remember { mutableStateMapOf<Long, CommentsState>() }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }
    val dsFactory = remember {
        DefaultDataSource.Factory(context, OkHttpDataSource.Factory(XHamsterNetwork.streamClient))
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                failed = true
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

    LaunchedEffect(muted) { player.volume = if (muted) 0f else 1f }

    LaunchedEffect(player) {
        while (true) {
            val duration = player.duration
            progress = if (duration > 0) (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f
            delay(250)
        }
    }

    var loadedShortId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(pagerState.currentPage, items.size, subtitleLang) {
        val page = pagerState.currentPage
        val short = items.getOrNull(page) ?: return@LaunchedEffect
        val sameShort = loadedShortId == short.id
        val resumeAt = if (sameShort) player.currentPosition else 0L
        if (!sameShort) {
            failed = false
            progress = 0f
            cueText = ""
        }

        if (metas[short.id] == null) {
            val batch = items.drop(page).take(6).map { it.id }.filter { it !in metas }
            XHamsterRepo.shortMetas(batch, referer).onSuccess { metas.putAll(it) }
        }

        val meta = metas[short.id]
        val stream = meta?.streamUrl
        val mediaItem = MediaItem.fromUri(stream ?: short.videoUrl)
        val video: MediaSource = if (stream != null && stream.contains(".m3u8")) {
            HlsMediaSource.Factory(dsFactory).createMediaSource(mediaItem)
        } else {
            ProgressiveMediaSource.Factory(dsFactory).createMediaSource(mediaItem)
        }
        val track = subtitleLang?.let { lang -> meta?.subtitles?.firstOrNull { it.lang == lang } }
        val source: MediaSource = if (track != null) {
            val subtitle = SingleSampleMediaSource.Factory(dsFactory).createMediaSource(
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(track.url))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage(track.lang)
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build(),
                C.TIME_UNSET,
            )
            MergingMediaSource(video, subtitle)
        } else video
        player.setMediaSource(source)
        player.prepare()
        if (resumeAt > 0) player.seekTo(resumeAt)
        player.play()
        loadedShortId = short.id
        if (hasMore && page >= items.size - 3) onLoadMore()
    }
    LaunchedEffect(failed) { if (failed) GlobalToasts.show(failedMsg, level = GlobalToasts.ToastLevel.ERROR) }

    fun toastError(t: Throwable) {
        GlobalToasts.show(
            if (t.message == "LOGIN_REQUIRED") loginRequired else (t.message ?: "Failed"),
            level = GlobalToasts.ToastLevel.ERROR,
        )
    }

    fun loadComments(short: XHamsterShort, more: Boolean) {
        val current = commentStates[short.id] ?: CommentsState()
        if (current.loading) return
        if (more && current.cursor == null) return
        commentStates[short.id] = current.copy(loading = true, error = null)
        scope.launch {
            XHamsterMomentsRepo.comments(short.id, if (more) current.cursor else null, short.pageUrl.ifBlank { referer })
                .onSuccess { page ->
                    val known = if (more) current.items else emptyList()
                    commentStates[short.id] = CommentsState(
                        items = (known + page.items).distinctBy { it.id },
                        cursor = page.nextCursor,
                        loaded = true,
                    )
                }
                .onFailure {
                    commentStates[short.id] = current.copy(
                        loading = false, loaded = true, error = it.message ?: "Failed",
                    )
                }
        }
    }

    fun withMeta(short: XHamsterShort, key: String, block: suspend (XHamsterShortMeta) -> Unit) {
        if (busy[key + short.id] == true) return
        busy[key + short.id] = true
        scope.launch {
            block(metas[short.id] ?: XHamsterShortMeta(null, 0L, false))
            busy[key + short.id] = false
        }
    }

    BackHandler {
        if (commentsFor != null) commentsFor = null else onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 0,
                userScrollEnabled = commentsFor == null,
            ) { page ->
                val short = items[page]
                val meta = metas[short.id]
                val owner = meta?.ownerName ?: ownerName
                val avatar = meta?.ownerAvatar ?: ownerAvatar
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
                        AndroidView(
                            modifier = Modifier.fillMaxSize().clickable {
                                if (player.isPlaying) player.pause() else player.play()
                            },
                            factory = { ctx -> TextureView(ctx).also { player.setVideoTextureView(it) } },
                            onRelease = { player.clearVideoTextureView(it) },
                        )
                        if (!isPlaying) {
                            Icon(
                                Icons.Filled.PlayArrow, null, tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(72.dp),
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
                                    .padding(start = 24.dp, end = 80.dp, bottom = 150.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }

                    // Bottom gradient-ish scrim for legibility
                    Box(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(180.dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                                )
                            ),
                    )

                    Column(
                        Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 88.dp)
                            .padding(bottom = 32.dp).navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (!owner.isNullOrBlank()) {
                            Text(owner, color = Color.White, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (short.title.isNotBlank()) Text(short.title, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        XHamsterFormat.count(short.views)?.let {
                            Text(stringResource(R.string.xhamster_n_views, it), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Column(
                        Modifier.align(Alignment.BottomEnd).padding(end = 12.dp)
                            .padding(bottom = 32.dp).navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (!avatar.isNullOrBlank()) {
                            Box(contentAlignment = Alignment.BottomCenter) {
                                RetryableImage(
                                    model = avatar,
                                    modifier = Modifier.size(48.dp).clip(CircleShape)
                                        .clickable(enabled = canSubscribe && !subscribed, onClick = onToggleSubscribe),
                                    placeholder = painterResource(R.drawable.h_chan_loading),
                                    error = painterResource(R.drawable.h_chan_load_failed),
                                    contentScale = ContentScale.Crop,
                                    imageLoader = imageLoader,
                                )
                                if (canSubscribe && !subscribed) {
                                    Icon(
                                        Icons.Filled.Add, stringResource(R.string.xhamster_subscribe),
                                        tint = Color.White,
                                        modifier = Modifier.offset(y = 8.dp).size(20.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                            .padding(2.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }

                        // Like (thumbs up)
                        RailButton(
                            onClick = {
                                withMeta(short, "like") { cur ->
                                    val result = if (cur.liked) XHamsterMomentsRepo.undoRate(short.id, short.pageUrl.ifBlank { referer })
                                    else XHamsterMomentsRepo.rate(short.id, true, short.pageUrl.ifBlank { referer })
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
                            },
                            label = meta?.likes?.let { XHamsterFormat.count(it) } ?: "",
                        ) {
                            Icon(
                                Icons.Filled.ThumbUp, stringResource(R.string.xhamster_short_like),
                                tint = if (meta?.liked == true) Color(0xFF4DA3FF) else Color.White,
                            )
                        }

                        // Dislike (thumbs down)
                        RailButton(
                            onClick = {
                                withMeta(short, "dislike") { cur ->
                                    val result = if (cur.disliked) XHamsterMomentsRepo.undoRate(short.id, short.pageUrl.ifBlank { referer })
                                    else XHamsterMomentsRepo.rate(short.id, false, short.pageUrl.ifBlank { referer })
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
                            },
                            label = null,
                        ) {
                            Icon(
                                Icons.Filled.ThumbDown, "Dislike",
                                tint = if (meta?.disliked == true) Color(0xFFFFB84D) else Color.White,
                            )
                        }

                        // Love (favourite)
                        RailButton(
                            onClick = {
                                withMeta(short, "love") { cur ->
                                    XHamsterMomentsRepo.setFavorite(short.id, !cur.favorite, short.pageUrl.ifBlank { referer })
                                        .onSuccess { metas[short.id] = cur.copy(favorite = !cur.favorite) }
                                        .onFailure(::toastError)
                                }
                            },
                            label = null,
                        ) {
                            Icon(
                                if (meta?.favorite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                "Love",
                                tint = if (meta?.favorite == true) Color(0xFFFF4D6D) else Color.White,
                            )
                        }

                        // Comments
                        RailButton(
                            onClick = {
                                commentsFor = short
                                if (commentStates[short.id]?.loaded != true) loadComments(short, false)
                            },
                            label = meta?.commentsCount?.takeIf { it > 0 }?.let { XHamsterFormat.count(it) } ?: "",
                        ) {
                            Icon(Icons.Filled.ChatBubble, "Comments", tint = Color.White)
                        }
                    }
                }
            }

            // Top bar: back, subtitles, report, mute
            Row(
                Modifier.align(Alignment.TopStart).fillMaxWidth().statusBarsPadding().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.55f), CircleShape),
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White) }
                Spacer(Modifier.weight(1f))
                val currentMeta = items.getOrNull(pagerState.currentPage)?.let { metas[it.id] }
                if (currentMeta?.subtitles?.isNotEmpty() == true) {
                    IconButton(
                        onClick = { showSubtitlePicker = true },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.55f), CircleShape),
                    ) {
                        Icon(
                            if (subtitleLang != null) Icons.Filled.ClosedCaption else Icons.Filled.ClosedCaptionDisabled,
                            "Subtitles", tint = Color.White,
                        )
                    }
                }
                IconButton(
                    onClick = { showReport = true },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.55f), CircleShape),
                ) { Icon(Icons.Filled.Flag, "Report", tint = Color.White) }
                IconButton(
                    onClick = { muted = !muted },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.55f), CircleShape),
                ) {
                    Icon(
                        if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        stringResource(if (muted) R.string.xhamster_short_unmute else R.string.xhamster_short_mute),
                        tint = Color.White,
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().height(3.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f),
            )

            // Comments panel
            commentsFor?.let { short ->
                val state = commentStates[short.id] ?: CommentsState()
                val listState = rememberLazyListState()
                val nearEnd by remember(listState) {
                    derivedStateOf {
                        val info = listState.layoutInfo
                        val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                        info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
                    }
                }
                LaunchedEffect(nearEnd, state.items.size, state.cursor) {
                    if (nearEnd && state.cursor != null && !state.loading) loadComments(short, true)
                }
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
                        .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { commentsFor = null },
                )
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.62f),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(Modifier.navigationBarsPadding()) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val count = metas[short.id]?.commentsCount ?: state.items.size.toLong()
                            Text("Comments  $count", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { commentsFor = null }) { Icon(Icons.Filled.Close, "Close") }
                        }
                        when {
                            state.loading && state.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }

                            state.error != null && state.items.isEmpty() -> Column(
                                Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(state.error, color = MaterialTheme.colorScheme.error)
                                TextButton(onClick = { commentStates.remove(short.id); loadComments(short, false) }) { Text("Retry") }
                            }

                            state.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    if (metas[short.id]?.commentsDisabled == true) "Comments are disabled for this video"
                                    else "No comments yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }

                            else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                                items(state.items, key = { it.id }) { c ->
                                    Row(
                                        Modifier.fillMaxWidth().padding(
                                            start = if (c.isReply) 48.dp else 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp,
                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        RetryableImage(
                                            model = c.avatarUrl,
                                            modifier = Modifier.size(32.dp).clip(CircleShape),
                                            placeholder = painterResource(R.drawable.h_chan_loading),
                                            error = painterResource(R.drawable.h_chan_load_failed),
                                            contentScale = ContentScale.Crop,
                                            imageLoader = imageLoader,
                                        )
                                        Column(Modifier.weight(1f)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(c.author, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                                c.createdSeconds?.let {
                                                    Text(
                                                        DateUtils.getRelativeTimeSpanString(
                                                            it * 1000, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                                                        ).toString(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                            }
                                            Text(c.text, style = MaterialTheme.typography.bodyMedium)
                                            if (c.likes > 0) {
                                                Text(
                                                    "👍 ${c.likes}", style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                                if (state.loading) item {
                                    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSubtitlePicker) {
        val tracks = items.getOrNull(pagerState.currentPage)?.let { metas[it.id] }?.subtitles.orEmpty()
        AlertDialog(
            onDismissRequest = { showSubtitlePicker = false },
            title = { Text("Subtitles") },
            text = {
                Column {
                    Row(
                        Modifier.fillMaxWidth().clickable { subtitleLang = null; showSubtitlePicker = false },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = subtitleLang == null, onClick = { subtitleLang = null; showSubtitlePicker = false })
                        Text("Off")
                    }
                    tracks.forEach { t ->
                        Row(
                            Modifier.fillMaxWidth().clickable { subtitleLang = t.lang; showSubtitlePicker = false },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = subtitleLang == t.lang, onClick = { subtitleLang = t.lang; showSubtitlePicker = false })
                            Text(t.label)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showSubtitlePicker = false }) { Text("Close") } },
        )
    }

    if (showReport) {
        val short = items.getOrNull(pagerState.currentPage)
        AlertDialog(
            onDismissRequest = { showReport = false },
            title = { Text("Report this video") },
            text = { Text("The site's report form will open in your browser, pre-filled with this video.") },
            confirmButton = {
                TextButton(onClick = {
                    showReport = false
                    if (short != null) {
                        val url = XHamsterMomentsRepo.reportUrl(short.pageUrl.ifBlank { referer })
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                }) { Text("Open report form") }
            },
            dismissButton = { TextButton(onClick = { showReport = false }) { Text("Cancel") } },
        )
    }
}
