package com.yenaly.han1meviewer.SitesHub.Strawberry.ui.user

import com.yenaly.han1meviewer.ui.component.social.SocialChip
import com.yenaly.han1meviewer.ui.component.social.SocialProfileHeader
import com.yenaly.han1meviewer.ui.component.social.SocialStat
import com.yenaly.han1meviewer.ui.component.social.SocialTone
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.local.StrawberryProfileCache
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.model.StrawberryUserProfile
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.model.StrawberryVideoCard
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote.StrawberryNetworkRepo
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.components.StrawberryVideoCardItem
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.components.rememberStrawberryWatched
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.home.InfiniteScrollFooter
import com.yenaly.han1meviewer.logic.state.VideoLoadingState
import com.yenaly.han1meviewer.ui.component.lazy.LazyVerticalGrid
import com.yenaly.han1meviewer.ui.screen.rememberVideoGridColumns
import com.yenaly.han1meviewer.ui.theme.SpacingNormal
import com.yenaly.han1meviewer.util.applicationContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrawberryUserScreen(
    username: String,
    onBack: () -> Unit,
    onOpenVideo: (String) -> Unit,
    onLoadFailure: (String) -> Unit = {},
) {
    var state by remember {
        mutableStateOf<VideoLoadingState<StrawberryUserProfile>>(VideoLoadingState.Loading)
    }
    var videos by remember { mutableStateOf<List<StrawberryVideoCard>>(emptyList()) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var pendingPage by remember { mutableIntStateOf(0) }
    var reloadTick by remember { mutableIntStateOf(0) }
    val gridState = rememberLazyGridState()
    val watchedIds = rememberStrawberryWatched()
    val columns = rememberVideoGridColumns()

    LaunchedEffect(username, reloadTick) {
        state = VideoLoadingState.Loading
        videos = emptyList()
        currentPage = 1
        totalPages = 1
        pendingPage = 0
        isLoadingMore = false

        StrawberryNetworkRepo.getUserProfile(username, 1).collect { s ->
            state = s
            if (s is VideoLoadingState.Success) {
                videos = s.info.videos
                totalPages = s.info.pageInfo?.total ?: 1
                if (s.info.isCurrentUser) {
                    StrawberryProfileCache.setCurrentUsername(s.info.username)
                    StrawberryProfileCache.put(s.info.username, s.info)
                }
            }
        }
    }

    LaunchedEffect(gridState, isLoadingMore, totalPages, state, currentPage) {
        snapshotFlow {
            val info = gridState.layoutInfo
            val total = info.totalItemsCount
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            total to last
        }.distinctUntilChanged().collect { (total, last) ->
            val s = state
            if (s is VideoLoadingState.Success &&
                total > 4 && last >= total - 4 &&
                !isLoadingMore && currentPage < totalPages &&
                pendingPage == 0
            ) {
                pendingPage = currentPage + 1
            }
        }
    }

    LaunchedEffect(pendingPage) {
        if (pendingPage <= 0) return@LaunchedEffect
        isLoadingMore = true
        StrawberryNetworkRepo.getUserProfile(username, pendingPage).collect { ns ->
            if (ns is VideoLoadingState.Success) {
                val incoming = ns.info
                videos = (videos + incoming.videos).distinctBy { it.videoId }
                totalPages = incoming.pageInfo?.total ?: totalPages
                currentPage = pendingPage
                if (incoming.isCurrentUser) {
                    val merged = incoming.copy(videos = videos)
                    StrawberryProfileCache.put(merged.username, merged)
                    state = VideoLoadingState.Success(merged)
                } else {
                    val s = state
                    if (s is VideoLoadingState.Success) {
                        state = VideoLoadingState.Success(s.info.copy(videos = videos))
                    }
                }
            }
            isLoadingMore = false
            pendingPage = 0
        }
    }

    LaunchedEffect(state) {
        val s = state
        if (s is VideoLoadingState.Error) {
            onLoadFailure(s.throwable.message ?: applicationContext.getString(R.string.strawberry_user_profile_failed))
        }
    }

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text("@$username", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (val s = state) {
                is VideoLoadingState.Loading -> SiteLoadingContent()
                is VideoLoadingState.Error -> SiteErrorContent(
                    message = s.throwable.message ?: stringResource(R.string.site_failed),
                    onRetry = { reloadTick++ },
                )
                is VideoLoadingState.NoContent -> SiteErrorContent(
                    message = stringResource(R.string.strawberry_user_not_found),
                    onRetry = { reloadTick++ },
                )
                is VideoLoadingState.Success -> {
                    val profile = s.info
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(SpacingNormal),
                        horizontalArrangement = Arrangement.spacedBy(SpacingNormal),
                        verticalArrangement = Arrangement.spacedBy(SpacingNormal),
                        enableItemAnimation = false,
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            UserHeader(profile = profile)
                        }
                        items(videos, key = { it.videoId }) { card ->
                            StrawberryVideoCardItem(
                                card = card,
                                onClick = { onOpenVideo(card.videoId) },
                                isWatched = card.videoId in watchedIds,
                            )
                        }
                        if (videos.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VideoLibrary,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(40.dp),
                                    )
                                    Text(
                                        text = stringResource(R.string.strawberry_no_uploads_yet),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                InfiniteScrollFooter(isLoadingMore = isLoadingMore)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserHeader(profile: StrawberryUserProfile) {
    val youLabel = stringResource(R.string.strawberry_you)
    val followingLabel = stringResource(R.string.site_following)
    val chips = buildList {
        if (profile.isCurrentUser) add(SocialChip(youLabel, SocialTone.Accent))
        if (profile.isFollowing) add(SocialChip(followingLabel, SocialTone.Positive))
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SocialProfileHeader(
            name = profile.displayName.ifBlank { profile.username },
            username = "@${profile.username}",
            avatarUrl = profile.avatarUrl,
            chips = chips,
            bio = profile.bio,
            stats = listOf(
                SocialStat(stringResource(R.string.site_followers), profile.followerCount?.toLong()),
                SocialStat(stringResource(R.string.site_following), profile.followingCount?.toLong()),
                SocialStat(stringResource(R.string.fap_tab_videos), profile.videoCount?.toLong()),
                SocialStat(stringResource(R.string.drawer_pictures), profile.pictureCount?.toLong()),
            ),
        )
        Text(
            text = stringResource(R.string.strawberry_uploads),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
        )
    }
}
