package com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.home.WumaobiListViewModel
import com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.home.WumaobiSource
import com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.home.WumaobiVideoGrid
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import com.yenaly.han1meviewer.ui.component.SiteScrollToTopButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar

@Composable
fun WumaobiFavFolderScreen(
    name: String,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    viewModel: WumaobiListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(name) { viewModel.setSource(WumaobiSource.Favorites(name)) }

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        val gridState = rememberLazyGridState()
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            SitePullToRefresh(
                loading = state.isLoading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                WumaobiVideoGrid(
                    state = state,
                    gridState = gridState,
                    onRetry = { viewModel.refresh() },
                    onLoadMore = { viewModel.loadMore() },
                    onOpenVideo = onNavigateToVideo,
                )
            }
            SiteScrollToTopButton(gridState)
        }
    }
}
