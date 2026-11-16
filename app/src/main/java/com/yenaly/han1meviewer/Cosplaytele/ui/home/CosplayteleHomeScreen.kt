package com.yenaly.han1meviewer.Cosplaytele.ui.home

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
import com.yenaly.han1meviewer.Cosplaytele.common.CosplayteleConstants
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleTerm
import com.yenaly.han1meviewer.Cosplaytele.ui.list.CosplayteleCardGrid
import com.yenaly.han1meviewer.Cosplaytele.ui.list.CosplayteleListViewModel
import com.yenaly.han1meviewer.Cosplaytele.ui.list.CosplayteleSource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.component.SiteBarIconButton
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SiteHomeTopBar
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import com.yenaly.han1meviewer.ui.component.SiteScrollToTopButton

@Composable
fun CosplayteleHomeScreen(
    onNavigateToPost: (String) -> Unit,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToTerm: (CosplayteleTerm) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CosplayteleListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedKey by rememberSaveable { mutableStateOf(CosplayteleConstants.HOME_SECTIONS.first().key) }
    val selected = CosplayteleConstants.HOME_SECTIONS.firstOrNull { it.key == selectedKey }
        ?: CosplayteleConstants.HOME_SECTIONS.first()

    LaunchedEffect(selectedKey) {
        viewModel.setSource(CosplayteleSource.Page(selected.path, selected.collectTerms))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteHomeTopBar(
                site = SiteType.COSPLAYTELE,
                hint = stringResource(R.string.cosplaytele_search_hint),
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
                CosplayteleCardGrid(
                    header = {
                        SiteChipRow(
                            options = CosplayteleConstants.HOME_SECTIONS,
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
