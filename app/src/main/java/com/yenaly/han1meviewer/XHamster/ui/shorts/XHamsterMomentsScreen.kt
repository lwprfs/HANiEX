package com.yenaly.han1meviewer.XHamster.ui.shorts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.XHamster.common.XHamsterConstants
import com.yenaly.han1meviewer.XHamster.common.XHamsterImageLoader
import com.yenaly.han1meviewer.XHamster.common.XHamsterSession
import com.yenaly.han1meviewer.XHamster.data.remote.XHamsterMomentsRepo
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SiteEmptyContent
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import com.yenaly.han1meviewer.ui.component.shorts.SiteShort
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortMeta
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortsGrid
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
    val isGifs = kind == XHamsterMomentsRepo.Feed.GIFS

    var rangeKey by rememberSaveable { mutableStateOf("alltime") }
    var items by remember { mutableStateOf<List<SiteShort>>(emptyList()) }
    val metas = remember { mutableMapOf<String, SiteShortMeta>() }
    var page by remember { mutableIntStateOf(1) }
    // One chain id per feed session; every page request must reuse it.
    var chainId by remember { mutableLongStateOf(0L) }
    var hasNext by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(kind, rangeKey, reload, epoch) {
        loading = true
        error = null
        chainId = XHamsterMomentsRepo.newChainId()
        XHamsterMomentsRepo.feed(kind, 1, rangeKey, chainId)
            .onSuccess { result ->
                items = result.items.map { it.first.toSiteShort(isGifs) }.distinctBy { it.id }
                metas.clear()
                result.items.forEach { metas[it.first.id.toString()] = it.second.toSiteMeta() }
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
            var nextPage = page + 1
            var tries = 0
            // A page can come back with only items we already have; keep going a few pages instead
            // of giving up, otherwise scrolling just stops loading.
            while (tries < 3) {
                tries++
                val result = XHamsterMomentsRepo.feed(
                    kind, nextPage, rangeKey, chainId,
                    currentChain = items.mapNotNull { it.id.toLongOrNull() },
                ).getOrNull()
                    // Network error: leave hasNext alone so scrolling again retries this page.
                    ?: break
                val known = items.map { it.id }.toHashSet()
                val fresh = result.items.filter { it.first.id.toString() !in known }
                if (fresh.isNotEmpty()) {
                    fresh.forEach { metas[it.first.id.toString()] = it.second.toSiteMeta() }
                    items = items + fresh.map { it.first.toSiteShort(isGifs) }
                }
                page = nextPage
                hasNext = result.hasNext
                if (fresh.isNotEmpty() || !result.hasNext) break
                nextPage++
            }
            loadingMore = false
        }
    }

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
            if (isGifs) {
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
                items.isEmpty() -> SiteEmptyContent(hint = stringResource(R.string.xhamster_shorts_empty))
                else -> SiteShortsGrid(
                    site = com.yenaly.han1meviewer.SiteType.XHAMSTER,
                    items = items,
                    onOpen = { playing = it },
                    imageLoader = imageLoader,
                    hasMore = hasNext,
                    loadingMore = loadingMore,
                    onLoadMore = { loadMore() },
                )
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
            referer = XHamsterConstants.BASE_URL + if (isGifs) "/gifs" else "/shorts",
            initialMetas = metas.toMap(),
        )
    }
}
