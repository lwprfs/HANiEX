package com.yenaly.han1meviewer.Cosplaytele.ui.list

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
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleTerm
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import com.yenaly.han1meviewer.ui.component.SiteScrollToTopButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar

/**
 * One site page by path: `/24-hours/`, `/top-search/`, `/best-cosplayer/`, `/explore-categories/`,
 * `/category/<slug>/` (cosplayer, game, type) and `/tag/<slug>/`.
 */
@Composable
fun CosplayteleListScreen(
    path: String,
    title: String,
    collectTerms: Boolean,
    onBack: () -> Unit,
    onNavigateToPost: (String) -> Unit,
    onNavigateToTerm: (CosplayteleTerm) -> Unit,
    viewModel: CosplayteleListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(path, collectTerms) {
        viewModel.setSource(CosplayteleSource.Page(path, collectTerms))
    }
    val gridState = rememberLazyGridState()
    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            SitePullToRefresh(
                loading = state.isLoading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                CosplayteleCardGrid(
                    state = state,
                    gridState = gridState,
                    onRetry = { viewModel.refresh() },
                    onLoadMore = { viewModel.loadMore() },
                    onOpenPost = onNavigateToPost,
                    onOpenTerm = onNavigateToTerm,
                )
            }
            SiteScrollToTopButton(gridState)
        }
    }
}
