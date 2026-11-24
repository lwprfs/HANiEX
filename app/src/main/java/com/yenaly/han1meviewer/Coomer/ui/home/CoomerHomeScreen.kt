package com.yenaly.han1meviewer.Coomer.ui.home

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
import com.yenaly.han1meviewer.Coomer.common.CoomerConstants
import com.yenaly.han1meviewer.Coomer.data.model.CoomerTerm
import com.yenaly.han1meviewer.Coomer.ui.list.CoomerCardGrid
import com.yenaly.han1meviewer.Coomer.ui.list.CoomerListViewModel
import com.yenaly.han1meviewer.Coomer.ui.list.CoomerSource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.component.SiteBarIconButton
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SiteHomeTopBar
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import com.yenaly.han1meviewer.ui.component.SiteScrollToTopButton

@Composable
fun CoomerHomeScreen(
    onNavigateToPost: (String) -> Unit,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToTerm: (CoomerTerm) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CoomerListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedKey by rememberSaveable { mutableStateOf(CoomerConstants.HOME_SECTIONS.first().key) }
    val selected = CoomerConstants.HOME_SECTIONS.firstOrNull { it.key == selectedKey }
        ?: CoomerConstants.HOME_SECTIONS.first()

    LaunchedEffect(selectedKey) {
        viewModel.setSource(CoomerSource.Page(selected.path))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteHomeTopBar(
                site = SiteType.COOMER,
                hint = stringResource(R.string.coomer_search_hint),
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
                CoomerCardGrid(
                    header = {
                        SiteChipRow(
                            options = CoomerConstants.HOME_SECTIONS,
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
                    onOpenPost = onNavigateToPost,
                    onOpenTerm = onNavigateToTerm,
                )
            }
            SiteScrollToTopButton(gridState)
        }
    }
}
