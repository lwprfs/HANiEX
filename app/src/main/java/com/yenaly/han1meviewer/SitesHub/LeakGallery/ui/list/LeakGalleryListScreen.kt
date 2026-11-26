package com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryConstants
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryTerm
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import com.yenaly.han1meviewer.ui.component.SiteScrollToTopButton
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold

@Composable
fun LeakGalleryListScreen(
    path: String,
    title: String,
    onBack: () -> Unit,
    onNavigateToPost: (String) -> Unit,
    onNavigateToTerm: (LeakGalleryTerm) -> Unit,
    viewModel: LeakGalleryListViewModel = viewModel(),
    onNavigateToShort: (mode: String, startId: String?) -> Unit = { _, _ -> },
    onLogin: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val username = remember(path) { LeakGalleryConstants.usernameFromProfilePath(path) }
    var type by rememberSaveable(path) { mutableStateOf("All") }
    val effectivePath = if (username != null) LeakGalleryConstants.profilePath(username, type) else path
    LaunchedEffect(effectivePath) {
        viewModel.setSource(LeakGallerySource.Page(effectivePath))
    }
    val gridState = rememberLazyGridState()
    // Shorts feeds (e.g. /tiktok/trending) open in the shorts player, like the website does.
    val shortsMode = remember(path) { LeakGalleryConstants.shortsModeFromPath(path) }
    HanimeScaffold(title = title, onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            SitePullToRefresh(
                loading = state.isLoading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                LeakGalleryCardGrid(
                    header = if (username != null) {
                        {
                            Column {
                                LeakGalleryProfileHeader(
                                    username = username,
                                    refreshKey = state.cards.size,
                                    onOpenShorts = { onNavigateToShort(username, null) },
                                    onLogin = onLogin,
                                )
                                SiteChipRow(
                                    options = LeakGalleryConstants.PROFILE_TYPES,
                                    selected = type,
                                    label = { it },
                                    onSelect = { type = it },
                                    key = { it },
                                    horizontalPadding = 0.dp,
                                )
                            }
                        }
                    } else null,
                    state = state,
                    gridState = gridState,
                    onRetry = { viewModel.refresh() },
                    onLoadMore = { viewModel.loadMore() },
                    onOpenPost = if (shortsMode != null) {
                        { id -> onNavigateToShort(shortsMode, id) }
                    } else onNavigateToPost,
                    onOpenTerm = onNavigateToTerm,
                )
            }
            SiteScrollToTopButton(gridState)
        }
    }
}
