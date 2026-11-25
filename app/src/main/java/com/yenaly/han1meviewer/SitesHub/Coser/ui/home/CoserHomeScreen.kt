package com.yenaly.han1meviewer.SitesHub.Coser.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
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
import com.yenaly.han1meviewer.SitesHub.Coser.common.CoserConstants
import com.yenaly.han1meviewer.SitesHub.Coser.data.model.CoserTerm
import com.yenaly.han1meviewer.SitesHub.Coser.ui.list.CoserCardGrid
import com.yenaly.han1meviewer.SitesHub.Coser.ui.list.CoserFilterBar
import com.yenaly.han1meviewer.SitesHub.Coser.ui.list.CoserListViewModel
import com.yenaly.han1meviewer.SitesHub.Coser.ui.list.CoserSource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.component.SiteBarIconButton
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SiteHomeTopBar
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import com.yenaly.han1meviewer.ui.component.SiteScrollToTopButton

@Composable
fun CoserHomeScreen(
    onNavigateToPost: (String) -> Unit,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToTerm: (CoserTerm) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CoserListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedKey by rememberSaveable { mutableStateOf(CoserConstants.HOME_SECTIONS.first().key) }
    val selected = CoserConstants.HOME_SECTIONS.firstOrNull { it.key == selectedKey }
        ?: CoserConstants.HOME_SECTIONS.first()

    var mins by rememberSaveable { mutableStateOf(0) }
    var quality by rememberSaveable { mutableStateOf(0) }
    var sort by rememberSaveable { mutableStateOf("") }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val filters = CoserConstants.Filters(mins, quality, sort)
    val filterable = !selected.collectTerms

    LaunchedEffect(selectedKey, mins, quality, sort) {
        viewModel.setSource(
            CoserSource.Page(
                selected.path,
                selected.collectTerms,
                if (filterable) filters else CoserConstants.Filters(),
            )
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteHomeTopBar(
                site = SiteType.COSER,
                hint = stringResource(R.string.coser_search_hint),
                onSearchClick = { onNavigateToSearch(null) },
                action = {
                    if (filterable) {
                        SiteBarIconButton(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.coser_filters),
                            onClick = { showFilters = !showFilters },
                        )
                    }
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
                CoserCardGrid(
                    header = {
                        Column {
                        SiteChipRow(
                            options = CoserConstants.HOME_SECTIONS,
                            selected = selected,
                            label = { it.title },
                            onSelect = { selectedKey = it.key },
                            key = { it.key },
                            horizontalPadding = 0.dp,
                        )
                        if (filterable && showFilters) {
                            CoserFilterBar(
                                filters = filters,
                                onChange = { mins = it.mins; quality = it.quality; sort = it.sort },
                            )
                        }
                        }
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
