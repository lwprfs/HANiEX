package com.yenaly.han1meviewer.ui.component.shorts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import com.yenaly.han1meviewer.ui.screen.SiteShortsCardSettings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
    site: com.yenaly.han1meviewer.SiteType? = null,
) {
    val custom = site?.takeIf { it in SiteShortsCardSettings.sites }
    val gridColumns = custom?.let { SiteShortsCardSettings.columns(it) } ?: columns
    val options = ShortCardOptions(
        ratio = custom?.let { SiteShortsCardSettings.ratio(it) } ?: (9f / 16f),
        showViews = custom?.let { SiteShortsCardSettings.isShown(it, SiteShortsCardSettings.Detail.Views) } ?: true,
        showDuration = custom?.let { SiteShortsCardSettings.isShown(it, SiteShortsCardSettings.Detail.Duration) } ?: true,
        showGif = custom?.let { SiteShortsCardSettings.isShown(it, SiteShortsCardSettings.Detail.GifBadge) } ?: true,
        showTitle = custom?.let { SiteShortsCardSettings.isShown(it, SiteShortsCardSettings.Detail.Title) } ?: false,
    )
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
        columns = GridCells.Fixed(gridColumns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        itemsIndexed(items, key = { _, s -> s.id }) { index, short ->
            SiteShortThumb(short, imageLoader, options, onClick = { onOpen(index) })
        }
        if (loadingMore) item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
    }
}

private data class ShortCardOptions(
    val ratio: Float,
    val showViews: Boolean,
    val showDuration: Boolean,
    val showGif: Boolean,
    val showTitle: Boolean,
)

@Composable
private fun SiteShortThumb(
    short: SiteShort,
    imageLoader: ImageLoader?,
    options: ShortCardOptions,
    onClick: () -> Unit,
) {
    val views = if (options.showViews) short.views.compactCount() else null
    val duration = if (options.showDuration) short.durationSeconds else null
    val title = short.title.takeIf { options.showTitle && it.isNotBlank() }
    Box(
        Modifier
            .aspectRatio(options.ratio)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
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

        if (views != null || duration != null || title != null) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(if (title != null) 96.dp else 64.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                        )
                    ),
            )
        }

        if (short.isGif && options.showGif) {
            ShortsGifBadge(Modifier.align(Alignment.TopStart).padding(8.dp))
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = if (views != null || duration != null) 6.dp else 0.dp),
                )
            }
            if (views != null || duration != null) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ShortsViewsBadge(views)
                    ShortsDurationBadge(duration)
                }
            }
        }
    }
}
