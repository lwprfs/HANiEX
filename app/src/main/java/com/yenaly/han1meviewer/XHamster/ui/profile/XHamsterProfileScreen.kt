package com.yenaly.han1meviewer.XHamster.ui.profile

import com.yenaly.han1meviewer.ui.component.social.SocialActionButton
import com.yenaly.han1meviewer.ui.component.social.SocialButtonStyle
import com.yenaly.han1meviewer.ui.component.social.SocialChip
import com.yenaly.han1meviewer.ui.component.social.SocialDefaults
import com.yenaly.han1meviewer.ui.component.social.SocialProfileHeader
import com.yenaly.han1meviewer.ui.component.social.SocialStat
import com.yenaly.han1meviewer.ui.component.social.SocialTabRow
import com.yenaly.han1meviewer.ui.component.social.SocialTagFlow
import com.yenaly.han1meviewer.ui.component.social.SocialTone
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterOption
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.media3.exoplayer.hls.HlsMediaSource
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShortMeta
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.XHamster.common.XHamsterConstants
import com.yenaly.han1meviewer.XHamster.common.XHamsterFormat
import com.yenaly.han1meviewer.XHamster.common.XHamsterImageLoader
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterEntityProfile
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShort
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterUploader
import com.yenaly.han1meviewer.XHamster.data.remote.XHamsterNetwork
import com.yenaly.han1meviewer.XHamster.data.remote.XHamsterRepo
import com.yenaly.han1meviewer.XHamster.ui.home.XHamsterListViewModel
import com.yenaly.han1meviewer.XHamster.ui.home.XHamsterSource
import com.yenaly.han1meviewer.XHamster.ui.home.XHamsterVideoGrid
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteAvatar
import com.yenaly.han1meviewer.ui.component.SiteSubscribeButton
import com.yenaly.han1meviewer.ui.component.SiteVerifiedBadge
import com.yenaly.han1meviewer.ui.component.SiteEmptyContent
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import com.yenaly.han1meviewer.ui.screen.RetryableImage
import com.yenaly.han1meviewer.XHamster.ui.shorts.XHamsterShortsPlayer
import com.yenaly.han1meviewer.XHamster.ui.shorts.toSiteShort
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortOwner
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortsGrid
import kotlinx.coroutines.launch

private enum class ProfileTab { VIDEOS, SHORTS }

@Composable
fun XHamsterProfileScreen(
    path: String,
    title: String,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onOpenPath: (path: String, title: String) -> Unit,
) {
    var profile by remember(path) { mutableStateOf<XHamsterEntityProfile?>(null) }
    var error by remember(path) { mutableStateOf<String?>(null) }
    var loading by remember(path) { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(path, reload) {
        loading = true
        error = null
        XHamsterRepo.entityProfile(path)
            .onSuccess { profile = it }
            .onFailure { error = it.message ?: it.javaClass.simpleName }
        loading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = { Text(profile?.name ?: title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val current = profile
            when {
                loading && current == null -> SiteLoadingContent()
                current == null -> SiteErrorContent(message = error, onRetry = { reload++ })
                else -> ProfileContent(current, onNavigateToVideo, onOpenPath)
            }
        }
    }
}

@Composable
private fun ProfileContent(
    profile: XHamsterEntityProfile,
    onNavigateToVideo: (String) -> Unit,
    onOpenPath: (String, String) -> Unit,
) {
    val base = XHamsterConstants.relativePath(profile.path)
    val shortsPath = profile.tabs.firstOrNull { it.path.endsWith("/shorts") }?.path ?: "$base/shorts"
    val videosPath = if (profile.kind == XHamsterEntityProfile.Kind.CREATOR) "$base/newest"
    else profile.tabs.firstOrNull { !it.path.endsWith("/shorts") }?.path ?: "$base/videos"
    val hasShorts = (profile.shortsCount ?: 0L) > 0L || profile.shorts.isNotEmpty() ||
            profile.kind == XHamsterEntityProfile.Kind.USER
    val tabs = if (hasShorts) listOf(ProfileTab.VIDEOS, ProfileTab.SHORTS) else listOf(ProfileTab.VIDEOS)
    var selected by remember(profile.path) { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()
    var subscribed by remember(profile.path) { mutableStateOf(profile.subscribed) }
    var busy by remember(profile.path) { mutableStateOf(false) }
    val loginRequired = stringResource(R.string.xhamster_login_required)
    val canSubscribe = profile.entityId > 0L && !profile.subscriptionModel.isNullOrBlank() && !profile.isMine
    val toggleSubscribe: () -> Unit = {
        if (!busy) {
            busy = true
            scope.launch {
                XHamsterRepo.setSubscribed(
                    uploader = XHamsterUploader(
                        name = profile.name, path = profile.path, avatarUrl = profile.avatarUrl,
                        subscribers = profile.subscribers, verified = profile.verified,
                        isChannel = profile.kind == XHamsterEntityProfile.Kind.CHANNEL,
                        subscriptionId = profile.entityId, subscriptionModel = profile.subscriptionModel,
                        subscribed = subscribed,
                    ),
                    videoCode = "",
                    subscribe = !subscribed,
                    refererUrl = XHamsterConstants.BASE_URL + XHamsterConstants.relativePath(profile.path),
                ).onSuccess { subscribed = it }
                    .onFailure {
                        GlobalToasts.show(
                            if (it.message == "LOGIN_REQUIRED") loginRequired else (it.message ?: "Failed"),
                            level = GlobalToasts.ToastLevel.ERROR,
                        )
                    }
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
            ProfileHeader(profile, subscribed, busy, canSubscribe, toggleSubscribe, onOpenPath)
        }
        val tabLabels = tabs.map { tab ->
            val count = if (tab == ProfileTab.VIDEOS) profile.videoCount else profile.shortsCount
            stringResource(
                if (tab == ProfileTab.VIDEOS) R.string.xhamster_tab_videos
                else R.string.xhamster_tab_shorts
            ) + (XHamsterFormat.count(count)?.let { " $it" } ?: "")
        }
        SocialTabRow(labels = tabLabels, selectedIndex = selected, onSelect = { selected = it })
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tabs.getOrElse(selected) { ProfileTab.VIDEOS }) {
                ProfileTab.VIDEOS -> ProfileVideos(videosPath, profile.tags, profile.sorts, onNavigateToVideo)
                ProfileTab.SHORTS -> ProfileShorts(
                    path = shortsPath,
                    preview = profile.shorts,
                    owner = SiteShortOwner(
                        name = profile.name,
                        avatarUrl = profile.avatarUrl,
                        verified = profile.verified,
                        subscribers = profile.subscribers,
                        isChannel = profile.kind == XHamsterEntityProfile.Kind.CHANNEL,
                        canSubscribe = canSubscribe,
                        subscribed = subscribed,
                        subscribeBusy = busy,
                    ),
                    pageUrl = XHamsterConstants.BASE_URL + XHamsterConstants.relativePath(profile.path) + "/shorts",
                    onToggleSubscribe = toggleSubscribe,
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: XHamsterEntityProfile,
    subscribed: Boolean,
    busy: Boolean,
    canSubscribe: Boolean,
    onToggleSubscribe: () -> Unit,
    onOpenPath: (String, String) -> Unit,
) {
    val imageLoader = XHamsterImageLoader.get(LocalContext.current)
    val privateLabel = stringResource(R.string.xhamster_profile_private)
    val chips = buildList {
        profile.country?.takeIf { it.isNotBlank() }?.let { add(SocialChip(it)) }
        if (!profile.isPublic) add(SocialChip(privateLabel, SocialTone.Warning))
    }
    val extraMeta = listOfNotNull(
        XHamsterFormat.count(profile.photosCount)?.let { stringResource(R.string.xhamster_n_photos, it) },
        XHamsterFormat.count(profile.favoritesCount)?.takeIf { profile.favoritesCount != 0L }
            ?.let { stringResource(R.string.xhamster_n_favorites, it) },
    ).joinToString(" · ")
    SocialProfileHeader(
        name = profile.name,
        avatarUrl = profile.avatarUrl,
        avatarShape = if (profile.kind == XHamsterEntityProfile.Kind.CHANNEL)
            SocialDefaults.CommunityAvatarShape else CircleShape,
        verified = profile.verified,
        chips = chips,
        bio = profile.aliases?.takeIf { it.isNotBlank() },
        metaLines = listOf(extraMeta),
        imageLoader = imageLoader,
        actions = if (canSubscribe) {
            {
                SocialActionButton(
                    label = stringResource(
                        if (subscribed) R.string.site_subscribed else R.string.site_subscribe
                    ),
                    onClick = onToggleSubscribe,
                    style = if (subscribed) SocialButtonStyle.Tonal else SocialButtonStyle.Primary,
                    loading = busy,
                    modifier = Modifier.weight(1f),
                )
            }
        } else null,
        stats = listOf(
            SocialStat(stringResource(R.string.social_stat_subscribers), profile.subscribers),
            SocialStat(stringResource(R.string.social_stat_videos), profile.videoCount),
            SocialStat(stringResource(R.string.social_stat_shorts), profile.shortsCount),
            SocialStat(stringResource(R.string.social_stat_views), profile.viewsCount),
        ),
        extra = if (profile.channels.isEmpty()) null else {
            {
                SocialTagFlow(
                    tags = profile.channels.map { it.name },
                    onClick = { chName ->
                        profile.channels.firstOrNull { it.name == chName }
                            ?.let { onOpenPath(it.path, it.name) }
                    },
                )
            }
        },
    )
}

@Composable
private fun ProfileVideos(
    basePath: String,
    tags: List<XHamsterOption>,
    sorts: List<XHamsterOption>,
    onNavigateToVideo: (String) -> Unit,
) {
    var tag by remember(basePath) { mutableStateOf<String?>(null) }
    val defaultSort = sorts.firstOrNull { it.selected }?.path
    var sort by remember(basePath) { mutableStateOf(defaultSort) }

    val path = remember(basePath, tag, sort) {
        val start = tag ?: basePath
        if (sort != null && sort != defaultSort) {
            start + (if (start.contains('?')) "&" else "?") + "sort=$sort"
        } else start
    }
    val vm: XHamsterListViewModel = viewModel(key = "xh-profile-videos-$path")
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(path) { vm.setSource(XHamsterSource.Path(path)) }

    Column(Modifier.fillMaxSize()) {
        if (sorts.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sorts, key = { "sort-" + it.path }) { option ->
                    FilterChip(
                        selected = sort == option.path,
                        onClick = { sort = option.path },
                        label = { Text(option.name) },
                    )
                }
            }
        }
        if (tags.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(tags, key = { "tag-" + it.path }) { option ->
                    FilterChip(
                        selected = tag == option.path,
                        onClick = { tag = if (tag == option.path) null else option.path },
                        label = { Text(option.name) },
                    )
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            XHamsterVideoGrid(
                state = state,
                onRetry = { vm.refresh() },
                onLoadMore = { vm.loadMore() },
                onOpenVideo = onNavigateToVideo,
            )
        }
    }
}

@Composable
internal fun ProfileShorts(
    path: String,
    preview: List<XHamsterShort>,
    owner: SiteShortOwner? = null,
    pageUrl: String = "",
    onToggleSubscribe: () -> Unit = {},
) {
    val imageLoader = XHamsterImageLoader.get(LocalContext.current)
    val scope = rememberCoroutineScope()
    var items by remember(path) { mutableStateOf(preview.map { it.toSiteShort() }) }
    var nextUrl by remember(path) { mutableStateOf<String?>(null) }
    var loading by remember(path) { mutableStateOf(true) }
    var loadingMore by remember(path) { mutableStateOf(false) }
    var error by remember(path) { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var playing by remember(path) { mutableStateOf<Int?>(null) }

    LaunchedEffect(path, reload) {
        loading = true
        error = null
        XHamsterRepo.shorts(path)
            .onSuccess { page ->
                if (page.items.isNotEmpty()) items = page.items.map { it.toSiteShort() }
                nextUrl = page.nextUrl
            }
            .onFailure { error = it.message ?: it.javaClass.simpleName }
        loading = false
    }

    fun loadMore() {
        val next = nextUrl ?: return
        if (loadingMore) return
        loadingMore = true
        scope.launch {
            XHamsterRepo.shorts(next)
                .onSuccess { page ->
                    val known = items.map { it.id }.toHashSet()
                    items = items + page.items.map { it.toSiteShort() }.filter { it.id !in known }
                    nextUrl = page.nextUrl?.takeIf { it != next }
                }
                .onFailure { nextUrl = null }
            loadingMore = false
        }
    }

    when {
        loading && items.isEmpty() -> SiteLoadingContent()
        items.isEmpty() && error != null -> SiteErrorContent(message = error, onRetry = { reload++ })
        items.isEmpty() -> SiteEmptyContent(hint = stringResource(R.string.xhamster_shorts_empty))
        else -> SiteShortsGrid(
            site = com.yenaly.han1meviewer.SiteType.XHAMSTER,
            items = items,
            onOpen = { playing = it },
            imageLoader = imageLoader,
            hasMore = nextUrl != null,
            loadingMore = loadingMore,
            onLoadMore = { loadMore() },
        )
    }

    playing?.let { start ->
        XHamsterShortsPlayer(
            items = items,
            initialIndex = start,
            hasMore = nextUrl != null,
            onLoadMore = { loadMore() },
            onDismiss = { playing = null },
            referer = pageUrl,
            owner = owner,
            onToggleSubscribe = onToggleSubscribe,
        )
    }
}
