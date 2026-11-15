package com.yenaly.han1meviewer.Buondua.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.Buondua.common.BuonduaImageLoader
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteEmptyContent
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SiteVideoCard
import com.yenaly.han1meviewer.ui.screen.rememberVideoGridColumns
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** Cover thumbnails on the site are 1024x1535, i.e. portrait 2:3. */
private const val COVER_ASPECT = 2f / 3f

@Composable
fun BuonduaAlbumGrid(
    state: BuonduaListUiState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    header: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    emptyHint: String = stringResource(R.string.buondua_nothing_here),
    gridState: LazyGridState = rememberLazyGridState(),
) {
    val imageLoader = BuonduaImageLoader.get(LocalContext.current)
    if (state.items.isEmpty()) {
        Column(modifier = modifier.fillMaxSize()) {
            if (header != null) {
                Box(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp)) { header() }
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoading -> SiteLoadingContent()
                    state.error != null -> SiteErrorContent(message = state.error, onRetry = onRetry)
                    else -> SiteEmptyContent(hint = emptyHint)
                }
            }
        }
        return
    }

    val nearEnd by remember(gridState) {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 6
        }
    }
    LaunchedEffect(gridState, state.items.size) {
        snapshotFlow { nearEnd }.distinctUntilChanged().filter { it }.collect { onLoadMore() }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(rememberVideoGridColumns()),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (header != null) item(span = { GridItemSpan(maxLineSpan) }) { header() }
        items(state.items, key = { it.videoCode }) { album ->
            SiteVideoCard(
                title = album.title,
                coverUrl = album.coverUrl,
                onClick = { onOpenAlbum(album.videoCode) },
                aspectRatio = COVER_ASPECT,
                bottomStartBadge = album.duration,
                subtitle = album.genre,
                imageLoader = imageLoader,
            )
        }
        if (state.isLoadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
