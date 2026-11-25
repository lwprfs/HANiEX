package com.yenaly.han1meviewer.SitesHub.FapFolder.ui.group

import com.yenaly.han1meviewer.ui.component.social.SocialActionButton
import com.yenaly.han1meviewer.ui.component.social.SocialButtonStyle
import com.yenaly.han1meviewer.ui.component.social.SocialChip
import com.yenaly.han1meviewer.ui.component.social.SocialDefaults
import com.yenaly.han1meviewer.ui.component.social.SocialLinkFlow
import com.yenaly.han1meviewer.ui.component.social.SocialProfileHeader
import com.yenaly.han1meviewer.ui.component.social.SocialStat
import com.yenaly.han1meviewer.ui.component.social.SocialTabRow
import com.yenaly.han1meviewer.ui.component.social.SocialTagFlow
import com.yenaly.han1meviewer.ui.component.social.SocialTone
import com.yenaly.han1meviewer.ui.screen.rememberVideoGridColumns
import com.yenaly.han1meviewer.SitesHub.FapFolder.ui.video.FapFolderVideoPlaylist
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.SitesHub.FapFolder.data.model.FapFolderFeedItem
import com.yenaly.han1meviewer.SitesHub.FapFolder.data.model.FapFolderMedia
import com.yenaly.han1meviewer.SitesHub.FapFolder.data.model.FapFolderPost
import com.yenaly.han1meviewer.SitesHub.FapFolder.data.model.FapFolderUserRow
import com.yenaly.han1meviewer.SitesHub.FapFolder.data.model.GroupMembership
import com.yenaly.han1meviewer.SitesHub.FapFolder.data.model.GroupVisibility
import com.yenaly.han1meviewer.SitesHub.FapFolder.data.parser.FapFolderProfileParser
import com.yenaly.han1meviewer.SitesHub.FapFolder.ui.components.FapFolderImageLightboxDialog
import com.yenaly.han1meviewer.SitesHub.FapFolder.ui.components.FapFolderInteractivePostCard
import com.yenaly.han1meviewer.SitesHub.FapFolder.ui.components.FapFolderUserRowCard
import com.yenaly.han1meviewer.util.DownloadLayout
import com.yenaly.han1meviewer.util.FapOwnerKind
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.screen.RetryableImage
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FapFolderGroupScreen(
    slug: String,
    initialTab: String,
    onBack: () -> Unit,
    onOpenPost: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenHashtag: (String) -> Unit = {},
    onOpenVideoPost: (String) -> Unit = onOpenPost,
    viewModel: FapFolderGroupViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var photoLightboxIndex by remember { mutableStateOf<Int?>(null) }
    var lightboxPayload by remember {
        mutableStateOf<Pair<List<FapFolderMedia.Image>, Int>?>(null)
    }

    LaunchedEffect(slug, initialTab) {
        viewModel.load(slug, initialTab)
    }

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = {
                    Text(
                        text = uiState.group?.displayName ?: slug,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = { SiteBackButton(onClick = onBack) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.site_refresh),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                uiState.isLoading && uiState.group == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }

                uiState.headerError != null && uiState.group == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.fap_failed_to_load_group),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = uiState.headerError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))
                            TextButton(onClick = { viewModel.refresh() }) { Text(stringResource(R.string.retry)) }
                        }
                    }
                }

                else -> {
                    when (uiState.activeTab) {
                        FapFolderGroupTab.PHOTOS,
                        FapFolderGroupTab.VIDEOS,
                        -> GroupMediaTab(
                            uiState = uiState,
                            onSwitchTab = { viewModel.switchTab(it) },
                            onToggleMembership = { viewModel.joinGroup() },
                            onOpenImage = { _, idx ->
                                if (uiState.activeTab == FapFolderGroupTab.PHOTOS) {
                                    photoLightboxIndex = idx
                                }
                            },
                            onOpenVideoBySlug = { id ->
                                FapFolderVideoPlaylist.set(
                                    uiState.videoTiles.map {
                                        it.postId?.toString()
                                            ?: it.postUrl.trimEnd('/').substringAfterLast('/')
                                    }
                                )
                                onOpenVideoPost(id)
                            },
                            onLoadMore = { viewModel.loadMoreItems() },
                            onRetry = { viewModel.retryLoadMoreItems() },
                        )

                        FapFolderGroupTab.POSTS -> GroupPostsTab(
                            uiState = uiState,
                            onSwitchTab = { viewModel.switchTab(it) },
                            onToggleMembership = { viewModel.joinGroup() },
                            onOpenPost = { id ->
                                FapFolderVideoPlaylist.set(groupVideoIds(uiState))
                                onOpenPost(id)
                            },
                            onOpenHashtag = onOpenHashtag,
                            onOpenProfile = onOpenProfile,
                            onOpenVideo = { post, _ ->
                                FapFolderVideoPlaylist.set(groupVideoIds(uiState))
                                onOpenVideoPost(post.slug ?: post.postId.toString())
                            },
                            onOpenImage = { images, idx -> lightboxPayload = images to idx },
                            onLoadMore = { viewModel.loadMoreItems() },
                            onRetry = { viewModel.retryLoadMoreItems() },
                        )
                    }
                }
            }
        }
    }

    photoLightboxIndex?.let { startIndex ->
        FapFolderImageLightboxDialog(
            images = uiState.mediaImages,
            initialIndex = startIndex,
            onDismiss = { photoLightboxIndex = null },
            hasMore = uiState.hasMore,
            isLoadingMore = uiState.isLoadingMore,
            onLoadMore = { viewModel.loadMoreItems() },
            saveSegments = DownloadLayout.fapFolderPhotoSegments(
                FapOwnerKind.GROUP, uiState.group?.displayName ?: slug, null,
            ),
        )
    }

    lightboxPayload?.let { (images, startIndex) ->
        FapFolderImageLightboxDialog(
            images = images,
            initialIndex = startIndex,
            onDismiss = { lightboxPayload = null },
            saveSegments = DownloadLayout.fapFolderPhotoSegments(
                FapOwnerKind.GROUP, uiState.group?.displayName ?: slug, null,
            ),
        )
    }
}

@Composable
private fun GroupHeaderBlock(
    group: com.yenaly.han1meviewer.SitesHub.FapFolder.data.model.FapFolderGroup,
    joining: Boolean,
    joinError: String?,
    onToggleMembership: () -> Unit,
    onSelectTab: (FapFolderGroupTab) -> Unit = {},
) {
    val privateLabel = stringResource(R.string.fap_private)
    val secretLabel = stringResource(R.string.fap_secret)
    val chips = buildList {
        group.category?.name?.takeIf { it.isNotBlank() }
            ?.let { add(SocialChip(it, SocialTone.Accent)) }
        when (group.visibility) {
            GroupVisibility.PRIVATE -> add(SocialChip(privateLabel, SocialTone.Warning))
            GroupVisibility.SECRET -> add(SocialChip(secretLabel, SocialTone.Warning))
            else -> Unit
        }
    }
    val followers = group.followerCount ?: group.memberCount
    val links = remember(group.externalLinks, group.socialLinks) {
        val fromExternal = group.externalLinks.map { it.label to it.url }
        val known = fromExternal.map { it.second }.toSet()
        fromExternal + group.socialLinks.entries
            .filter { it.value.isNotBlank() && it.value !in known }
            .map { it.key to it.value }
    }
    val allTags = remember(group.tags, group.bioTags) {
        (group.tags + group.bioTags).filter { it.isNotBlank() }.distinct()
    }
    SocialProfileHeader(
        name = group.displayName,
        coverUrl = group.bannerUrl,
        avatarUrl = group.avatarUrl,
        avatarShape = SocialDefaults.CommunityAvatarShape,
        isCommunity = true,
        chips = chips,
        bio = group.bio,
        errorText = joinError,
        actions = {
            MembershipButton(
                membership = group.membership,
                loading = joining,
                onClick = onToggleMembership,
                modifier = Modifier.weight(1f),
            )
        },
        stats = listOf(
            SocialStat(stringResource(R.string.site_followers), followers?.toLong()),
            SocialStat(
                stringResource(R.string.fap_tab_posts), group.postCount?.toLong(),
                onClick = { onSelectTab(FapFolderGroupTab.POSTS) },
            ),
            SocialStat(
                stringResource(R.string.fap_tab_photos), group.photoCount?.toLong(),
                onClick = { onSelectTab(FapFolderGroupTab.PHOTOS) },
            ),
            SocialStat(
                stringResource(R.string.fap_tab_videos), group.videoCount?.toLong(),
                onClick = { onSelectTab(FapFolderGroupTab.VIDEOS) },
            ),
        ),
        extra = if (allTags.isEmpty() && links.isEmpty()) null else {
            {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SocialTagFlow(allTags, prefix = "#")
                    SocialLinkFlow(links)
                }
            }
        },
    )
}

@Composable
private fun GroupTabRow(
    activeTab: FapFolderGroupTab,
    onSelect: (FapFolderGroupTab) -> Unit,
) {
    val tabs = FapFolderGroupTab.entries
    SocialTabRow(
        labels = tabs.map { it.label },
        selectedIndex = tabs.indexOf(activeTab),
        onSelect = { onSelect(tabs[it]) },
        showSingle = true,
        community = true,
    )
}

private fun LazyListScope.groupHeaderItems(
    uiState: FapFolderGroupUiState,
    onSwitchTab: (FapFolderGroupTab) -> Unit,
    onToggleMembership: () -> Unit,
) {
    uiState.group?.let { group ->
        item(key = "group_header") {
            GroupHeaderBlock(
                group = group,
                joining = uiState.joining,
                joinError = uiState.joinError,
                onToggleMembership = onToggleMembership,
                onSelectTab = onSwitchTab,
            )
        }
        item(key = "group_tabs") {
            GroupTabRow(
                activeTab = uiState.activeTab,
                onSelect = onSwitchTab,
            )
        }
    }
}

private fun LazyGridScope.groupHeaderItems(
    uiState: FapFolderGroupUiState,
    onSwitchTab: (FapFolderGroupTab) -> Unit,
    onToggleMembership: () -> Unit,
) {
    uiState.group?.let { group ->
        item(
            key = "group_header",
            span = { GridItemSpan(maxLineSpan) },
        ) {
            GroupHeaderBlock(
                group = group,
                joining = uiState.joining,
                joinError = uiState.joinError,
                onToggleMembership = onToggleMembership,
                onSelectTab = onSwitchTab,
            )
        }
        item(
            key = "group_tabs",
            span = { GridItemSpan(maxLineSpan) },
        ) {
            GroupTabRow(
                activeTab = uiState.activeTab,
                onSelect = onSwitchTab,
            )
        }
    }
}

private fun groupVideoIds(uiState: FapFolderGroupUiState): List<String> =
    uiState.posts.filter { it.primaryVideo != null }.map { it.slug ?: it.postId.toString() }

@Composable
private fun GroupPostsTab(
    uiState: FapFolderGroupUiState,
    onSwitchTab: (FapFolderGroupTab) -> Unit,
    onToggleMembership: () -> Unit,
    onOpenPost: (String) -> Unit,
    onOpenHashtag: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenVideo: (FapFolderPost, FapFolderMedia.Video) -> Unit,
    onOpenImage: (List<FapFolderMedia.Image>, Int) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
    val listState: LazyListState = rememberLazyListState()

    LaunchedEffect(listState, uiState.hasMore, uiState.isLoadingMore, uiState.isLoading) {
        snapshotFlow {
            val info = listState.layoutInfo
            info.totalItemsCount to (info.visibleItemsInfo.lastOrNull()?.index ?: -1)
        }
            .distinctUntilChanged()
            .collect { (total, last) ->
                if (total > 4 &&
                    last >= total - 4 &&
                    uiState.hasMore &&
                    !uiState.isLoadingMore &&
                    !uiState.isLoading
                ) {
                    onLoadMore()
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        groupHeaderItems(
            uiState = uiState,
            onSwitchTab = onSwitchTab,
            onToggleMembership = onToggleMembership,
        )

        when {
            uiState.isLoading && uiState.items.isEmpty() -> item(key = "items_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }

            uiState.itemsError != null && uiState.items.isEmpty() -> item(key = "items_error") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.fap_failed_to_load_posts),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = uiState.itemsError ,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                    }
                }
            }

            uiState.items.isEmpty() -> item(key = "items_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.fap_nothing_here_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                items(uiState.items, key = { it.stableKey }) { item ->
                    when (item) {
                        is FapFolderFeedItem.Post -> {
                            val post = item.post
                            FapFolderInteractivePostCard(
                                post = post,
                                modifier = Modifier.padding(horizontal = 8.dp),
                                onClickPost = {
                                    val slugOrId = post.slug ?: post.postId.toString()
                                    onOpenPost(slugOrId)
                                },
                                onClickHashtag = onOpenHashtag,
                                onClickAuthor = { author ->
                                    val groupSlug = author.groupSlug
                                    val username = author.username
                                    when {
                                        !groupSlug.isNullOrBlank() -> onOpenProfile(groupSlug)
                                        !username.isNullOrBlank() -> onOpenProfile(username)
                                    }
                                },
                                onClickVideo = { video -> onOpenVideo(post, video) },
                                onClickImage = { img, all ->
                                    val idx = all.indexOfFirst {
                                        it.photoId == img.photoId
                                    }.coerceAtLeast(0)
                                    onOpenImage(all, idx)
                                },
                                onClickPoll = { },
                                onClickLink = { },
                                onLongClickPost = { },
                            )
                        }
                        is FapFolderFeedItem.User -> FapFolderUserRowCard(
                            user = item.user,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            onClick = {
                                if (item.user.username.isNotBlank()) {
                                    onOpenProfile(item.user.username)
                                }
                            },
                        )
                        is FapFolderFeedItem.Community -> Unit
                    }
                }
                if (uiState.isLoadingMore) {
                    item(key = "items_more_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupMediaTab(
    uiState: FapFolderGroupUiState,
    onSwitchTab: (FapFolderGroupTab) -> Unit,
    onToggleMembership: () -> Unit,
    onOpenImage: (List<FapFolderMedia.Image>, Int) -> Unit,
    onOpenVideoBySlug: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
    val isPhotos = uiState.activeTab == FapFolderGroupTab.PHOTOS
    val gridState: LazyGridState = rememberLazyGridState()
    val videoColumns = rememberVideoGridColumns()
    val columns = if (isPhotos) 3 else videoColumns

    LaunchedEffect(
        gridState,
        uiState.hasMore,
        uiState.isLoadingMore,
        uiState.isLoading,
        uiState.photoTiles.size,
        uiState.videoTiles.size,
    ) {
        snapshotFlow {
            val info = gridState.layoutInfo
            info.totalItemsCount to (info.visibleItemsInfo.lastOrNull()?.index ?: -1)
        }
            .distinctUntilChanged()
            .conflate()
            .collect { (total, last) ->
                if (total > 6 &&
                    last >= total - 6 &&
                    uiState.hasMore &&
                    !uiState.isLoadingMore &&
                    !uiState.isLoading
                ) {
                    onLoadMore()
                }
            }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 6.dp,
            end = 6.dp,
            top = 6.dp,
            bottom = 24.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        groupHeaderItems(
            uiState = uiState,
            onSwitchTab = onSwitchTab,
            onToggleMembership = onToggleMembership,
        )

        if (isPhotos) {
            when {
                uiState.isLoading && uiState.photoTiles.isEmpty() ->
                    item(
                        key = "photos_loading",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }
                    }

                uiState.itemsError != null && uiState.photoTiles.isEmpty() ->
                    item(
                        key = "photos_error",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.fap_failed_to_load_photos),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    text = uiState.itemsError ,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                            }
                        }
                    }

                uiState.photoTiles.isEmpty() ->
                    item(
                        key = "photos_empty",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.fap_no_photos_yet),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                else -> {
                    val images = uiState.mediaImages
                    gridItems(
                        items = uiState.photoTiles,
                        key = { "p_${it.photoId ?: it.fullUrl.hashCode()}" },
                    ) { tile ->
                        val idx = uiState.photoTiles.indexOf(tile).coerceAtLeast(0)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onOpenImage(images, idx) },
                        ) {
                            RetryableImage(
                                model = tile.previewUrl ?: tile.fullUrl,
                                contentDescription = tile.alt,
                                modifier = Modifier.fillMaxSize(),
                                placeholder = painterResource(R.drawable.h_chan_loading_small),
                                error = painterResource(R.drawable.h_chan_load_failed_small),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                    if (uiState.isLoadingMore) {
                        item(
                            key = "photos_more_loading",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            when {
                uiState.isLoading && uiState.videoTiles.isEmpty() ->
                    item(
                        key = "videos_loading",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }
                    }

                uiState.itemsError != null && uiState.videoTiles.isEmpty() ->
                    item(
                        key = "videos_error",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.fap_failed_to_load_videos),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    text = uiState.itemsError ,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                            }
                        }
                    }

                uiState.videoTiles.isEmpty() ->
                    item(
                        key = "videos_empty",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.fap_no_videos_yet),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                else -> {
                    gridItems(
                        items = uiState.videoTiles,
                        key = { "v_${it.postId ?: it.postUrl.hashCode()}" },
                    ) { tile ->
                        VideoTileBox(
                            tile = tile,
                            onClick = {
                                val slugOrId = tile.postId?.toString()
                                    ?: tile.postUrl.trimEnd('/').substringAfterLast('/')
                                onOpenVideoBySlug(slugOrId)
                            },
                        )
                    }
                    if (uiState.isLoadingMore) {
                        item(
                            key = "videos_more_loading",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoTileBox(
    tile: FapFolderProfileParser.VideoTile,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        val poster = tile.thumbnailUrl
        if (!poster.isNullOrBlank()) {
            RetryableImage(
                model = poster,
                contentDescription = tile.title,
                modifier = Modifier.fillMaxSize(),
                placeholder = painterResource(R.drawable.h_chan_loading_small),
                error = painterResource(R.drawable.h_chan_load_failed_small),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.site_play),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun MembershipButton(
    membership: GroupMembership,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when (membership) {
        GroupMembership.NOT_MEMBER, GroupMembership.UNKNOWN -> stringResource(R.string.fap_join)
        GroupMembership.PENDING -> stringResource(R.string.creator_status_pending)
        GroupMembership.MEMBER -> stringResource(R.string.site_following)
        GroupMembership.ADMIN -> stringResource(R.string.fap_admin)
        GroupMembership.OWNER -> stringResource(R.string.fap_owner)
    }
    val enabled = membership != GroupMembership.ADMIN && membership != GroupMembership.OWNER
    val style = when (membership) {
        GroupMembership.MEMBER, GroupMembership.PENDING -> SocialButtonStyle.Outlined
        GroupMembership.ADMIN, GroupMembership.OWNER -> SocialButtonStyle.Tonal
        else -> SocialButtonStyle.Primary
    }
    SocialActionButton(
        label = label,
        onClick = onClick,
        modifier = modifier,
        style = style,
        loading = loading,
        enabled = enabled,
    )
}

private fun memberKey(user: FapFolderUserRow): String =
    "member_${user.uid ?: user.username}"
