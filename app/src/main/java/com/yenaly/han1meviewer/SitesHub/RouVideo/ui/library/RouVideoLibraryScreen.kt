package com.yenaly.han1meviewer.SitesHub.RouVideo.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SitesHub.RouVideo.common.RouVideoConstants.FavoriteOrder
import com.yenaly.han1meviewer.SitesHub.RouVideo.common.RouVideoImageLoader
import com.yenaly.han1meviewer.SitesHub.RouVideo.common.RouVideoSettings
import com.yenaly.han1meviewer.SitesHub.RouVideo.common.RouVideoTagLabels
import com.yenaly.han1meviewer.util.translation.AppTranslation
import com.yenaly.han1meviewer.SitesHub.RouVideo.data.model.RouVideoFollowRow
import com.yenaly.han1meviewer.SitesHub.RouVideo.data.remote.RouVideoAuth
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.account.RouVideoSignInPrompt
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.home.RouVideoListViewModel
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.home.RouVideoSource
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.home.RouVideoVideoGrid
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.login.rememberRouVideoLoginLauncher
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SiteEmptyContent
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

enum class RouVideoLibraryTab(val labelRes: Int) {
    FOLLOWS(R.string.rouvideo_my_follows),
    FAVORITES(R.string.rouvideo_my_favorites),
    HISTORY(R.string.rouvideo_cloud_history),
}

@Composable
fun RouVideoLibraryScreen(
    initialTab: RouVideoLibraryTab,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    listViewModel: RouVideoListViewModel = viewModel(),
    followsViewModel: RouVideoFollowsViewModel = viewModel(),
) {
    val auth by RouVideoAuth.state.collectAsStateWithLifecycle()
    var tabName by rememberSaveable { mutableStateOf(initialTab.name) }
    val tab = runCatching { RouVideoLibraryTab.valueOf(tabName) }.getOrDefault(initialTab)
    var orderName by rememberSaveable { mutableStateOf(FavoriteOrder.SAVED.name) }
    val order = runCatching { FavoriteOrder.valueOf(orderName) }.getOrDefault(FavoriteOrder.SAVED)
    var onlyNew by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    val listState by listViewModel.state.collectAsStateWithLifecycle()
    val followsState by followsViewModel.state.collectAsStateWithLifecycle()

    var reloadKey by remember { mutableStateOf(0) }
    val launchLogin = rememberRouVideoLoginLauncher { reloadKey++ }

    LaunchedEffect(Unit) {
        if (RouVideoAuth.isLoggedIn) RouVideoAuth.refresh()
    }

    LaunchedEffect(tab, order, onlyNew, auth.loggedIn, reloadKey) {
        if (!auth.loggedIn) return@LaunchedEffect
        when (tab) {
            RouVideoLibraryTab.FOLLOWS -> followsViewModel.load(onlyNew, force = reloadKey > 0)
            RouVideoLibraryTab.FAVORITES ->
                listViewModel.setSource(RouVideoSource.Favorites(order), force = reloadKey > 0)

            RouVideoLibraryTab.HISTORY ->
                listViewModel.setSource(RouVideoSource.CloudHistory, force = reloadKey > 0)
        }
    }

    LaunchedEffect(followsState.message) {
        followsState.message?.let {
            snackbar.showSnackbar(it)
            followsViewModel.messageShown()
        }
    }

    val tabLabels = RouVideoLibraryTab.entries.associateWith { stringResource(it.labelRes) }
    val orderLabels = FavoriteOrder.entries.associateWith { stringResource(it.labelRes) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.rouvideo_my_library)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!auth.loggedIn) {
                RouVideoSignInPrompt(
                    onSignIn = { launchLogin(false) },
                    onRegister = { launchLogin(true) },
                )
                return@Column
            }

            SiteChipRow(
                options = RouVideoLibraryTab.entries.toList(),
                selected = tab,
                label = { tabLabels.getValue(it) },
                onSelect = { tabName = it.name },
            )

            when (tab) {
                RouVideoLibraryTab.FOLLOWS -> FollowsContent(
                    state = followsState,
                    onlyNew = onlyNew,
                    onOnlyNewChange = { onlyNew = it },
                    onRetry = { followsViewModel.refresh() },
                    onLoadMore = { followsViewModel.loadMore() },
                    onOpen = onNavigateToVideo,
                    onUnfollow = followsViewModel::unfollow,
                    onSeen = followsViewModel::markSeen,
                    onAllSeen = followsViewModel::markAllSeen,
                    onSignIn = { launchLogin(false) },
                    onRegister = { launchLogin(true) },
                )

                RouVideoLibraryTab.FAVORITES, RouVideoLibraryTab.HISTORY -> {
                    if (tab == RouVideoLibraryTab.FAVORITES) {
                        SiteChipRow(
                            options = FavoriteOrder.entries.toList(),
                            selected = order,
                            label = { orderLabels.getValue(it) },
                            onSelect = { orderName = it.name },
                        )
                    }
                    if (listState.needsSignIn) {
                        RouVideoSignInPrompt(
                            onSignIn = { launchLogin(false) },
                            onRegister = { launchLogin(true) },
                            message = stringResource(R.string.rouvideo_account_session_expired),
                        )
                    } else {
                        RouVideoVideoGrid(
                            state = listState,
                            onRetry = { listViewModel.refresh() },
                            onLoadMore = { listViewModel.loadMore() },
                            onOpenVideo = onNavigateToVideo,
                            emptyHint = stringResource(
                                if (tab == RouVideoLibraryTab.FAVORITES) R.string.rouvideo_no_favorites
                                else R.string.rouvideo_no_cloud_history
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowsContent(
    state: RouVideoFollowsUiState,
    onlyNew: Boolean,
    onOnlyNewChange: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpen: (String) -> Unit,
    onUnfollow: (RouVideoFollowRow) -> Unit,
    onSeen: (RouVideoFollowRow) -> Unit,
    onAllSeen: () -> Unit,
    onSignIn: () -> Unit,
    onRegister: () -> Unit,
) {
    val allLabel = stringResource(R.string.rouvideo_follow_filter_all)
    val newLabel = stringResource(R.string.rouvideo_follow_filter_new)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SiteChipRow(
            options = listOf(false, true),
            selected = onlyNew,
            label = { if (it) newLabel else allLabel },
            onSelect = onOnlyNewChange,
            modifier = Modifier.weight(1f),
        )
        if (state.newCount > 0) {
            TextButton(onClick = onAllSeen) {
                Text(stringResource(R.string.rouvideo_mark_all_seen))
            }
        }
    }

    when {
        state.needsSignIn -> RouVideoSignInPrompt(
            onSignIn = onSignIn,
            onRegister = onRegister,
            message = stringResource(R.string.rouvideo_account_session_expired),
        )

        state.isLoading && state.rows.isEmpty() -> SiteLoadingContent()
        state.error != null && state.rows.isEmpty() ->
            SiteErrorContent(message = state.error, onRetry = onRetry)

        state.rows.isEmpty() -> SiteEmptyContent(
            hint = stringResource(
                if (onlyNew) R.string.rouvideo_no_follows_filtered else R.string.rouvideo_no_follows
            ),
        )

        else -> {
            val listState = rememberLazyListState()
            val nearEnd by remember(listState) {
                derivedStateOf {
                    val info = listState.layoutInfo
                    val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                    info.totalItemsCount > 0 && last >= info.totalItemsCount - 4
                }
            }
            LaunchedEffect(listState, state.rows.size) {
                snapshotFlow { nearEnd }.distinctUntilChanged().filter { it }.collect { onLoadMore() }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.rows, key = { it.seriesId }) { row ->
                    FollowRowCard(
                        row = row,
                        onOpen = onOpen,
                        onUnfollow = { onUnfollow(row) },
                        onSeen = { onSeen(row) },
                    )
                }
                if (state.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowRowCard(
    row: RouVideoFollowRow,
    onOpen: (String) -> Unit,
    onUnfollow: () -> Unit,
    onSeen: () -> Unit,
) {
    val context = LocalContext.current
    val title = AppTranslation.title(RouVideoSettings.pick(row.title, row.titleZh))
    val target = row.playVideoId
    val playLabel = when (row.playKind) {
        "resume" -> stringResource(R.string.rouvideo_play_resume, row.playNumber)
        "next" -> stringResource(R.string.rouvideo_play_next, row.playNumber)
        "again" -> stringResource(R.string.rouvideo_play_again, row.playNumber)
        "start" -> stringResource(R.string.rouvideo_play_start, row.playNumber)
        else -> stringResource(R.string.rouvideo_watch)
    }
    val status = stringResource(
        if (row.completed) R.string.rouvideo_series_completed else R.string.rouvideo_series_ongoing
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (target != null) Modifier.clickable { onOpen(target) } else Modifier),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = row.coverUrl,
                contentDescription = null,
                imageLoader = RouVideoImageLoader.get(context),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(if (row.isPoster) 84.dp else 128.dp)
                    .aspectRatio(if (row.isPoster) 3f / 4f else 16f / 10f)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (row.unseen > 0) {
                    Text(
                        text = stringResource(
                            R.string.rouvideo_new_episodes, row.unseen, row.episodeCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        text = listOfNotNull(status, row.epLabel).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                row.tags.firstOrNull()?.let {
                    Text(
                        text = RouVideoTagLabels.label(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (target != null) {
                        Button(
                            onClick = { onOpen(target) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        ) { Text(playLabel, style = MaterialTheme.typography.labelMedium) }
                    }
                    if (row.unseen > 0) {
                        OutlinedButton(
                            onClick = onSeen,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text(
                                stringResource(R.string.rouvideo_got_it),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onUnfollow) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.rouvideo_unfollow),
                )
            }
        }
    }
}
