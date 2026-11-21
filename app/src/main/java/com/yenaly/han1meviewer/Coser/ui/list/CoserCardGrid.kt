package com.yenaly.han1meviewer.Coser.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.Coser.common.CoserImageLoader
import com.yenaly.han1meviewer.Coser.data.model.CoserCard
import com.yenaly.han1meviewer.Coser.data.model.CoserTerm
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteEmptyContent
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SiteVideoCard
import com.yenaly.han1meviewer.ui.screen.RetryableImage
import com.yenaly.han1meviewer.ui.screen.rememberVideoGridColumns
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

const val COSER_CARD_RATIO = 16f / 9f

/** Returns (top-end badge, bottom-start badge) for a card: quality / "+N videos" and duration. */
@Composable
fun coserBadges(card: CoserCard): Pair<String?, String?> {
    val top = buildList {
        if (card.quality.isNotBlank()) add(card.quality)
        if (card.extraItems > 0) add("+${card.extraItems}")
    }.joinToString(" ").ifBlank { null }
    return top to card.duration.ifBlank { null }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoserCardGrid(
    state: CoserListUiState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenPost: (String) -> Unit,
    onOpenTerm: (CoserTerm) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    emptyHint: String = stringResource(R.string.coser_nothing_here),
    gridState: LazyGridState = rememberLazyGridState(),
) {
    val imageLoader = CoserImageLoader.get(LocalContext.current)
    if (state.cards.isEmpty() && state.terms.isEmpty()) {
        Column(modifier = modifier.fillMaxSize()) {
            if (header != null) Box(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp)) { header() }
            Box(Modifier.weight(1f).fillMaxWidth()) {
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
    LaunchedEffect(gridState, state.cards.size) {
        snapshotFlow { nearEnd }.distinctUntilChanged().filter { it }.collect { onLoadMore() }
    }
    val columns = rememberVideoGridColumns().coerceAtLeast(2)
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (header != null) item(span = { GridItemSpan(maxLineSpan) }) { header() }
        items(state.terms, key = { "term_${it.kind.name}_${it.slug}" }) { term ->
            CoserTermCard(
                term = term,
                onClick = { onOpenTerm(term) },
                imageLoader = imageLoader,
            )
        }
        items(state.cards, key = { it.slug }) { card ->
            val (top, bottom) = coserBadges(card)
            SiteVideoCard(
                title = card.title,
                coverUrl = card.coverUrl,
                onClick = { onOpenPost(card.slug) },
                aspectRatio = COSER_CARD_RATIO,
                views = card.views.ifBlank { null },
                uploadTime = card.date.ifBlank { null },
                subtitle = card.seriesName.ifBlank { null },
                topEndBadge = top,
                bottomStartBadge = bottom,
                imageLoader = imageLoader,
                contentScale = ContentScale.Crop,
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

@Composable
fun CoserTermCard(
    term: CoserTerm,
    onClick: () -> Unit,
    imageLoader: coil3.ImageLoader,
    modifier: Modifier = Modifier,
) {
    val badge = if (term.count > 0) stringResource(R.string.coser_badge_posts, term.count) else null
    if (term.thumbUrl.isNotBlank()) {
        SiteVideoCard(
            title = term.name,
            coverUrl = term.thumbUrl,
            onClick = onClick,
            modifier = modifier,
            aspectRatio = COSER_CARD_RATIO,
            topEndBadge = badge,
            imageLoader = imageLoader,
            contentScale = ContentScale.Crop,
        )
        return
    }
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(COSER_CARD_RATIO)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = term.name.trim().take(1).uppercase(),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                if (badge != null) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                    ) {
                        Text(
                            text = badge,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = androidx.compose.ui.graphics.Color.White,
                        )
                    }
                }
            }
            Text(
                text = term.name,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
