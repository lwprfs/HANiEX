package com.yenaly.han1meviewer.XHamster.ui.shorts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.XHamster.common.XHamsterFormat
import com.yenaly.han1meviewer.XHamster.common.XHamsterImageLoader
import com.yenaly.han1meviewer.XHamster.common.XHamsterSession
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShort
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShortMeta
import com.yenaly.han1meviewer.XHamster.data.remote.XHamsterMomentsRepo
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SiteEmptyContent
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import com.yenaly.han1meviewer.ui.screen.RetryableImage
import kotlinx.coroutines.launch

private data class TimeRange(val key: String, @androidx.annotation.StringRes val title: Int)

private val GIF_RANGES = listOf(
    TimeRange("daily", R.string.xh_gif_daily),
    TimeRange("weekly", R.string.xh_gif_weekly),
    TimeRange("monthly", R.string.xh_gif_monthly),
    TimeRange("alltime", R.string.xh_gif_alltime),
    TimeRange("newest", R.string.xh_gif_newest),
)

@Composable
fun XHamsterMomentsScreen(
    kind: XHamsterMomentsRepo.Feed,
    title: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val imageLoader = XHamsterImageLoader.get(context)
    val scope = rememberCoroutineScope()
    val epoch by XHamsterSession.epoch.collectAsStateWithLifecycle()

    var rangeKey by rememberSaveable { mutableStateOf("alltime") }
    var items by remember { mutableStateOf<List<XHamsterShort>>(emptyList()) }
    val metas = remember { mutableMapOf<Long, XHamsterShortMeta>() }
    var page by remember { mutableIntStateOf(1) }
    var hasNext by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(kind, rangeKey, reload, epoch) {
        loading = true
        error = null
        XHamsterMomentsRepo.feed(kind, 1, rangeKey)
            .onSuccess { result ->
                items = result.items.map { it.first }.distinctBy { it.id }
                metas.clear()
                result.items.forEach { metas[it.first.id] = it.second }
                page = 1
                hasNext = result.hasNext
            }
            .onFailure {
                items = emptyList()
                hasNext = false
                error = it.message ?: it.javaClass.simpleName
            }
        loading = false
    }

    fun loadMore() {
        if (!hasNext || loadingMore || loading) return
        loadingMore = true
        scope.launch {
            XHamsterMomentsRepo.feed(kind, page + 1, rangeKey)
                .onSuccess { result ->
                    val known = items.map { it.id }.toHashSet()
                    val fresh = result.items.filter { it.first.id !in known }
                    fresh.forEach { metas[it.first.id] = it.second }
                    items = items + fresh.map { it.first }
                    page += 1
                    hasNext = result.hasNext && fresh.isNotEmpty()
                }
                .onFailure { hasNext = false }
            loadingMore = false
        }
    }

    val gridState = rememberLazyGridState()
    val nearEnd by remember(gridState) {
        derivedStateOf {
            val li = gridState.layoutInfo
            val last = li.visibleItemsInfo.lastOrNull()?.index ?: 0
            li.totalItemsCount > 0 && last >= li.totalItemsCount - 6
        }
    }
    LaunchedEffect(nearEnd, items.size, hasNext) { if (nearEnd && hasNext) loadMore() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = { Text(title) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (kind == XHamsterMomentsRepo.Feed.GIFS) {
                Box(Modifier.padding(horizontal = 12.dp)) {
                    SiteChipRow(
                        options = GIF_RANGES,
                        selected = GIF_RANGES.first { it.key == rangeKey },
                        label = { context.getString(it.title) },
                        onSelect = { rangeKey = it.key },
                        key = { it.key },
                        horizontalPadding = 0.dp,
                    )
                }
            }
            when {
                loading && items.isEmpty() -> SiteLoadingContent()
                items.isEmpty() && error != null -> SiteErrorContent(message = error, onRetry = { reload++ })
                items.isEmpty() -> SiteEmptyContent(hint = stringResourceEmpty())
                else -> LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(items, key = { _, s -> s.id }) { index, short ->
                        Box(
                            Modifier.aspectRatio(9f / 16f).clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { playing = index },
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
                            XHamsterFormat.count(short.views)?.let {
                                Text(
                                    "▶ $it", color = Color.White, style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                    if (loadingMore) item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }
    }

    playing?.let { start ->
        XHamsterShortsPlayer(
            items = items,
            initialIndex = start,
            hasMore = hasNext,
            onLoadMore = { loadMore() },
            onDismiss = { playing = null },
            referer = if (kind == XHamsterMomentsRepo.Feed.GIFS) "${com.yenaly.han1meviewer.XHamster.common.XHamsterConstants.BASE_URL}/gifs" else "${com.yenaly.han1meviewer.XHamster.common.XHamsterConstants.BASE_URL}/shorts",
            ownerName = null,
            ownerAvatar = null,
            initialMetas = metas.toMap(),
        )
    }
}

@Composable
private fun stringResourceEmpty(): String = androidx.compose.ui.res.stringResource(R.string.xhamster_shorts_empty)
