package com.yenaly.han1meviewer.Cosplaytele.ui.list

import androidx.compose.foundation.clickable
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
import com.yenaly.han1meviewer.Cosplaytele.common.CosplayteleImageLoader
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleCard
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleTerm
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteEmptyContent
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SiteVideoCard
import com.yenaly.han1meviewer.ui.screen.RetryableImage
import com.yenaly.han1meviewer.ui.screen.rememberVideoGridColumns
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

const val COSPLAYTELE_CARD_RATIO = 3f / 4f

@Composable
fun cosplayteleBadges(card: CosplayteleCard): Pair<String?, String?> {
    val photos = if (card.photoCount > 0) stringResource(R.string.cosplaytele_badge_photos, card.photoCount) else null
    val video = if (card.videoCount > 0) stringResource(R.string.cosplaytele_badge_video) else null
    return photos to video
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CosplayteleCardGrid(
    state: CosplayteleListUiState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenPost: (String) -> Unit,
    onOpenTerm: (CosplayteleTerm) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    emptyHint: String = stringResource(R.string.cosplaytele_nothing_here),
    gridState: LazyGridState = rememberLazyGridState(),
) {
    val imageLoader = CosplayteleImageLoader.get(LocalContext.current)
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
        if (state.terms.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CosplayteleTermBlock(state.terms, onOpenTerm)
            }
        }
        items(state.cards, key = { it.slug }) { card ->
            val (photos, video) = cosplayteleBadges(card)
            SiteVideoCard(
                title = card.title,
                coverUrl = card.coverUrl,
                onClick = { onOpenPost(card.slug) },
                aspectRatio = COSPLAYTELE_CARD_RATIO,
                topEndBadge = photos,
                bottomStartBadge = video,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CosplayteleTermBlock(terms: List<CosplayteleTerm>, onOpenTerm: (CosplayteleTerm) -> Unit) {
    val withThumbs = terms.filter { it.thumbUrl.isNotBlank() }
    val plain = terms.filter { it.thumbUrl.isBlank() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        if (withThumbs.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                withThumbs.forEach { term -> CosplayteleTermAvatar(term, onOpenTerm) }
            }
        }
        if (plain.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                plain.forEach { term ->
                    AssistChip(
                        onClick = { onOpenTerm(term) },
                        label = {
                            Text(if (term.count > 0) "${term.name} (${term.count})" else term.name)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CosplayteleTermAvatar(term: CosplayteleTerm, onOpenTerm: (CosplayteleTerm) -> Unit) {
    val imageLoader = CosplayteleImageLoader.get(LocalContext.current)
    Surface(
        shape = RoundedCornerShape(40.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.clip(RoundedCornerShape(40.dp)).clickable { onOpenTerm(term) },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 14.dp),
        ) {
            RetryableImage(
                model = term.thumbUrl,
                contentDescription = term.name,
                modifier = Modifier.size(40.dp).clip(CircleShape),
                placeholder = painterResource(R.drawable.h_chan_loading),
                error = painterResource(R.drawable.h_chan_load_failed),
                contentScale = ContentScale.Crop,
                imageLoader = imageLoader,
            )
            Text(
                text = term.name,
                modifier = Modifier.padding(start = 8.dp).widthIn(max = 150.dp),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
