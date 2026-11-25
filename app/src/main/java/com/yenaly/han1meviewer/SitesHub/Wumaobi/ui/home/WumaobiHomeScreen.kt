package com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.SitesHub.Wumaobi.common.WumaobiConstants
import com.yenaly.han1meviewer.SitesHub.Wumaobi.common.WumaobiSettings
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteBarIconButton
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SiteHomeTopBar
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import com.yenaly.han1meviewer.ui.component.SiteScrollToTopButton

@Composable
fun WumaobiHomeScreen(
    onNavigateToVideo: (String) -> Unit,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WumaobiListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sections = remember { WumaobiSettings.visibleSections() }
    var sectionKey by rememberSaveable { mutableStateOf(sections.first().key) }
    val section = sections.firstOrNull { it.key == sectionKey } ?: sections.first()

    LaunchedEffect(section.key) {
        viewModel.setSource(WumaobiSource.Path(section.path, section.sort))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteHomeTopBar(
                site = com.yenaly.han1meviewer.SiteType.WUMAOBI,
                hint = stringResource(R.string.wumaobi_search_hint),
                onSearchClick = { onNavigateToSearch(null) },
                action = {
                    SiteBarIconButton(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(R.string.wumaobi_account_title),
                        onClick = onNavigateToAccount,
                    )
                    SiteBarIconButton(
                        imageVector = Icons.Default.History,
                        contentDescription = stringResource(R.string.watch_history),
                        onClick = onNavigateToHistory,
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
                WumaobiVideoGrid(
                    header = {
                        Column {
                            if (sections.size > 1) {
                                SiteChipRow(
                                    options = sections,
                                    selected = section,
                                    label = { it.title },
                                    onSelect = { sectionKey = it.key },
                                    key = { it.key },
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
                )
            }
            SiteScrollToTopButton(gridState)
        }
    }
}

@Composable
internal fun browseLabel(kind: WumaobiConstants.EntityKind): String = stringResource(
    when (kind) {
        WumaobiConstants.EntityKind.TAGS -> R.string.wumaobi_tags
        WumaobiConstants.EntityKind.ACTRESS -> R.string.wumaobi_models
        WumaobiConstants.EntityKind.SERIES -> R.string.wumaobi_series
    }
)
