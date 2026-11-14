package com.yenaly.han1meviewer.RouVideo.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteHomeTopBar
import com.yenaly.han1meviewer.ui.component.SiteScrollToTopButton
import com.yenaly.han1meviewer.RouVideo.common.RouVideoConstants
import com.yenaly.han1meviewer.RouVideo.common.RouVideoConstants.SeriesFilter
import com.yenaly.han1meviewer.RouVideo.common.RouVideoConstants.SeriesSort
import com.yenaly.han1meviewer.RouVideo.common.RouVideoConstants.Sort
import com.yenaly.han1meviewer.RouVideo.common.RouVideoSettings
import com.yenaly.han1meviewer.ui.component.SiteBarIconButton
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SiteSearchTopBar

private val LIST_SORTS = listOf(Sort.NEWEST, Sort.VIEWS, Sort.LIKES)

private fun defaultSort(tab: RouVideoConstants.HomeTab): Sort = when (val source = tab.source) {
    is RouVideoConstants.TabSource.Library -> source.sort
    is RouVideoConstants.TabSource.Tag -> source.sort
    is RouVideoConstants.TabSource.Series -> Sort.NEWEST
}

@Composable
fun RouVideoHomeScreen(
    onNavigateToVideo: (String) -> Unit,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToCategories: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RouVideoListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val tagsTranslated = com.yenaly.han1meviewer.RouVideo.common.RouVideoTranslation.isActive(
        com.yenaly.han1meviewer.RouVideo.common.RouVideoTranslation.Kind.TAG
    )
    val tabs = remember(tagsTranslated) { RouVideoSettings.visibleTabs() }
    var selectedKey by rememberSaveable { mutableStateOf(tabs.first().key) }
    val tab = tabs.firstOrNull { it.key == selectedKey } ?: tabs.first()

    var sortName by rememberSaveable(tab.key) { mutableStateOf(defaultSort(tab).name) }
    var filterName by rememberSaveable(tab.key) { mutableStateOf(SeriesFilter.ALL.name) }
    var seriesSortName by rememberSaveable(tab.key) { mutableStateOf(SeriesSort.UPDATED.name) }
    val sort = runCatching { Sort.valueOf(sortName) }.getOrDefault(Sort.NEWEST)
    val filter = runCatching { SeriesFilter.valueOf(filterName) }.getOrDefault(SeriesFilter.ALL)
    val seriesSort = runCatching { SeriesSort.valueOf(seriesSortName) }
        .getOrDefault(SeriesSort.UPDATED)

    LaunchedEffect(tab.key, sortName, filterName, seriesSortName) {
        val source = when (val s = tab.source) {
            is RouVideoConstants.TabSource.Library -> RouVideoSource.Library(sort)
            is RouVideoConstants.TabSource.Tag -> RouVideoSource.Tag(s.tag, sort)
            is RouVideoConstants.TabSource.Series -> RouVideoSource.Series(s.tag, filter, seriesSort)
        }
        viewModel.setSource(source)
    }

    val sortLabels = LIST_SORTS.associateWith { stringResource(it.labelRes) }
    val filterLabels = SeriesFilter.entries.associateWith { stringResource(it.labelRes) }
    val seriesSortLabels = SeriesSort.entries.associateWith { stringResource(it.labelRes) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteHomeTopBar(
                site = com.yenaly.han1meviewer.SiteType.ROUVIDEO,
                hint = stringResource(R.string.rouvideo_search_hint),
                onSearchClick = { onNavigateToSearch(null) },
                action = {
                    SiteBarIconButton(
                        imageVector = Icons.Default.Category,
                        contentDescription = stringResource(R.string.rouvideo_categories),
                        onClick = onNavigateToCategories,
                    )
                },
            )
        },
    ) { padding ->
        val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SitePullToRefresh(
                loading = state.isLoading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                RouVideoVideoGrid(
                    // Tab / sort / filter chips scroll together with the videos.
                    header = {
                        Column {
                            SiteChipRow(
                                options = tabs,
                                selected = tab,
                                label = { it.title },
                                onSelect = { selectedKey = it.key },
                                key = { it.key },
                                horizontalPadding = 0.dp,
                            )
                            when (tab.source) {
                                is RouVideoConstants.TabSource.Series -> {
                                    SiteChipRow(
                                        options = SeriesFilter.entries.toList(),
                                        selected = filter,
                                        label = { filterLabels.getValue(it) },
                                        onSelect = { filterName = it.name },
                                        horizontalPadding = 0.dp,
                                    )
                                    SiteChipRow(
                                        options = SeriesSort.entries.toList(),
                                        selected = seriesSort,
                                        label = { seriesSortLabels.getValue(it) },
                                        onSelect = { seriesSortName = it.name },
                                        horizontalPadding = 0.dp,
                                    )
                                }

                                else -> SiteChipRow(
                                    options = LIST_SORTS,
                                    selected = if (sort in LIST_SORTS) sort else Sort.NEWEST,
                                    label = { sortLabels.getValue(it) },
                                    onSelect = { sortName = it.name },
                                    horizontalPadding = 0.dp,
                                )
                            }
                        }
                    },
                    state = state,
                    gridState = gridState,
                    onRetry = { viewModel.refresh() },
                    onLoadMore = { viewModel.loadMore() },
                    onOpenVideo = onNavigateToVideo,
                    skipTag = (tab.source as? RouVideoConstants.TabSource.Tag)?.tag,
                )
            }
            SiteScrollToTopButton(gridState)
        }
    }
}
