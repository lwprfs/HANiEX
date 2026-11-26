package com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryConstants
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGallerySettings
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryTerm
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.list.LeakGalleryCardGrid
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.list.LeakGalleryListViewModel
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.list.LeakGallerySource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.component.SiteBarIconButton
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SiteHomeTopBar
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import com.yenaly.han1meviewer.ui.component.SiteScrollToTopButton

@Composable
fun LeakGalleryHomeScreen(
    onNavigateToPost: (String) -> Unit,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToTerm: (LeakGalleryTerm) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LeakGalleryListViewModel = viewModel(),
    onNavigateToShort: (mode: String, startId: String?) -> Unit = { _, _ -> },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedKey by rememberSaveable { mutableStateOf(LeakGallerySettings.defaultSection) }
    val selected = LeakGalleryConstants.HOME_SECTIONS.firstOrNull { it.key == selectedKey }
        ?: LeakGalleryConstants.HOME_SECTIONS.first()

    LaunchedEffect(selectedKey) {
        viewModel.setSource(LeakGallerySource.Page(selected.path))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteHomeTopBar(
                site = SiteType.LEAKGALLERY,
                hint = stringResource(R.string.leakgallery_search_hint),
                onSearchClick = { onNavigateToSearch(null) },
                action = {
                    SiteBarIconButton(
                        imageVector = Icons.Default.History,
                        contentDescription = stringResource(R.string.watch_history),
                        onClick = onNavigateToHistory,
                    )
                },
            )
        },
    ) { padding ->
        val gridState = rememberLazyGridState()
        Box(modifier = modifier.fillMaxSize().padding(padding)) {
            SitePullToRefresh(
                loading = state.isLoading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                LeakGalleryCardGrid(
                    header = {
                        SiteChipRow(
                            options = LeakGalleryConstants.HOME_SECTIONS,
                            selected = selected,
                            label = { it.title },
                            onSelect = { selectedKey = it.key },
                            key = { it.key },
                            horizontalPadding = 0.dp,
                        )
                    },
                    state = state,
                    gridState = gridState,
                    onRetry = { viewModel.refresh() },
                    onLoadMore = { viewModel.loadMore() },
                    onOpenPost = LeakGalleryConstants.shortsModeFromPath(selected.path)?.let { mode ->
                        { id: String -> onNavigateToShort(mode, id) }
                    } ?: onNavigateToPost,
                    onOpenTerm = onNavigateToTerm,
                )
            }
            SiteScrollToTopButton(gridState)
        }
    }
}
