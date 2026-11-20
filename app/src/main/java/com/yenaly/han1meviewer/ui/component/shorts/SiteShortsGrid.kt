package com.yenaly.han1meviewer.ui.component.shorts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.screen.RetryableImage
import com.yenaly.han1meviewer.util.compactCount

@Composable
fun SiteShortsGrid(
    items: List<SiteShort>,
    onOpen: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader? = null,
    hasMore: Boolean = false,
    loadingMore: Boolean = false,
    onLoadMore: () -> Unit = {},
    columns: Int = 3,
    state: LazyGridState = rememberLazyGridState(),
) {
    val nearEnd by remember(state) {
        derivedStateOf {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 6
        }
    }
    LaunchedEffect(nearEnd, items.size, hasMore) { if (nearEnd && hasMore) onLoadMore() }

    LazyVerticalGrid(
        state = state,
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(items, key = { _, s -> s.id }) { index, short ->
            SiteShortThumb(short, imageLoader, onClick = { onOpen(index) })
        }
        if (loadingMore) item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun SiteShortThumb(short: SiteShort, imageLoader: ImageLoader?, onClick: () -> Unit) {
    Box(
        Modifier
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        RetryableImage(
            model = short.coverUrl,
            contentDescription = short.title,
            modifier = Modifier.fillMaxSize(),
            placeholder = painterResource(R.drawable.h_chan_loading),
            error = painterResource(R.drawable.h_chan_load_failed),
            contentScale = ContentScale.Crop,
            imageLoader = imageLoader,
        )
        if (short.isGif) {
            ShortsGifBadge(Modifier.align(Alignment.TopStart).padding(6.dp))
        }
        short.views.compactCount()?.let { views ->
            Row(
                Modifier.align(Alignment.BottomStart).padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(R.drawable.ic_baseline_play_arrow_24), null,
                    tint = Color.White, modifier = Modifier.size(12.dp),
                )
                Text(
                    views, color = Color.White, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 2.dp),
                )
            }
        }
    }
}

@Composable
internal fun ShortsGifBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            stringResource(R.string.site_shorts_gif_badge),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}
