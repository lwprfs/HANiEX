package com.yenaly.han1meviewer.RedGifs.ui.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.RedGifs.common.RedGifsImageLoader
import com.yenaly.han1meviewer.RedGifs.data.model.RedGifsListPage
import com.yenaly.han1meviewer.RedGifs.data.model.RedGifsNiche
import com.yenaly.han1meviewer.RedGifs.data.model.RedGifsUser
import com.yenaly.han1meviewer.RedGifs.data.remote.RedGifsRepo
import com.yenaly.han1meviewer.ui.component.SiteAvatar
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SiteEmptyContent
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import com.yenaly.han1meviewer.ui.component.SiteVerifiedBadge
import kotlinx.coroutines.launch

/** A simple paged vertical list used by niches / creators / following / followers / search results. */
@Composable
internal fun <T : Any> RedGifsPagedList(
    reloadKey: Any?,
    load: suspend (page: Int) -> Result<RedGifsListPage<T>>,
    itemKey: (T) -> Any,
    emptyHint: String,
    modifier: Modifier = Modifier,
    row: @Composable (T) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val currentLoad by rememberUpdatedState(load)
    var items by remember { mutableStateOf<List<T>>(emptyList()) }
    var page by remember { mutableIntStateOf(1) }
    var hasNext by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    LaunchedEffect(reloadKey, reload) {
        loading = true
        error = null
        currentLoad(1)
            .onSuccess {
                items = it.items.distinctBy(itemKey)
                page = 1
                hasNext = it.hasNext && it.items.isNotEmpty()
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
            currentLoad(page + 1)
                .onSuccess { result ->
                    val known = items.map(itemKey).toHashSet()
                    val fresh = result.items.filter { itemKey(it) !in known }
                    items = items + fresh
                    page += 1
                    hasNext = result.hasNext && fresh.isNotEmpty()
                }
                .onFailure { hasNext = false }
            loadingMore = false
        }
    }

    val nearEnd by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 4
        }
    }
    LaunchedEffect(nearEnd, items.size, hasNext) { if (nearEnd && hasNext) loadMore() }

    Box(modifier.fillMaxSize()) {
        when {
            loading && items.isEmpty() -> SiteLoadingContent()
            items.isEmpty() && error != null -> SiteErrorContent(message = error, onRetry = { reload++ })
            items.isEmpty() -> SiteEmptyContent(hint = emptyHint)
            else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                itemsIndexed(items, key = { _, entry -> itemKey(entry) }) { _, item -> row(item) }
                if (loadingMore) item {
                    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
}

private fun compact(n: Long): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format("%.1fK", n / 1_000.0)
    else -> n.toString()
}

@Composable
internal fun RedGifsNicheRow(niche: RedGifsNiche, onClick: () -> Unit) {
    val imageLoader = RedGifsImageLoader.get(LocalContext.current)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SiteAvatar(url = niche.thumbnailUrl, name = niche.name, size = 48.dp, imageLoader = imageLoader)
        Column(Modifier.weight(1f)) {
            Text(niche.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.redgifs_niche_stats, compact(niche.gifs), compact(niche.subscribers)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun RedGifsUserRow(user: RedGifsUser, onClick: () -> Unit) {
    val imageLoader = RedGifsImageLoader.get(LocalContext.current)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SiteAvatar(url = user.avatarUrl, name = user.username, size = 48.dp, imageLoader = imageLoader)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    user.username,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (user.verified) SiteVerifiedBadge()
            }
            Text(
                stringResource(R.string.redgifs_user_stats, compact(user.followers), compact(user.gifs)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** `following == true` lists the niches the user subscribed to; otherwise browses all niches by category. */
@Composable
fun RedGifsNichesScreen(
    following: Boolean,
    onBack: () -> Unit,
    onOpenNiche: (RedGifsNiche) -> Unit,
) {
    var categories by remember { mutableStateOf<List<String>>(emptyList()) }
    var category by rememberSaveable { mutableStateOf("") }
    val allLabel = stringResource(R.string.redgifs_all)

    LaunchedEffect(following) {
        if (!following) RedGifsRepo.nicheCategories().onSuccess { categories = it }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = {
                    Text(stringResource(if (following) R.string.redgifs_followed_niches else R.string.redgifs_niches))
                },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!following && categories.isNotEmpty()) {
                val options = listOf("") + categories
                SiteChipRow(
                    options = options,
                    selected = category,
                    label = { if (it.isEmpty()) allLabel else it },
                    onSelect = { category = it },
                    key = { it },
                )
            }
            RedGifsPagedList(
                reloadKey = category to following,
                load = { page ->
                    if (following) {
                        RedGifsRepo.followedNiches().map { RedGifsListPage(it, 1, 1) }
                    } else {
                        RedGifsRepo.niches("subscribers", page, category.ifEmpty { null })
                    }
                },
                itemKey = { it.id },
                emptyHint = stringResource(R.string.redgifs_empty),
            ) { niche -> RedGifsNicheRow(niche) { onOpenNiche(niche) } }
        }
    }
}

enum class RedGifsUsersMode { CREATORS, FOLLOWING, FOLLOWERS }

private data class CreatorOrder(val key: String, val titleRes: Int)

private val CREATOR_ORDERS = listOf(
    CreatorOrder("trending", R.string.redgifs_order_trending),
    CreatorOrder("top28", R.string.redgifs_order_top),
    CreatorOrder("latest", R.string.redgifs_order_new),
)

@Composable
fun RedGifsUsersScreen(
    mode: RedGifsUsersMode,
    onBack: () -> Unit,
    onOpenUser: (RedGifsUser) -> Unit,
) {
    val context = LocalContext.current
    var orderKey by rememberSaveable { mutableStateOf("trending") }
    val title = when (mode) {
        RedGifsUsersMode.CREATORS -> R.string.redgifs_creators
        RedGifsUsersMode.FOLLOWING -> R.string.redgifs_following
        RedGifsUsersMode.FOLLOWERS -> R.string.redgifs_followers
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (mode == RedGifsUsersMode.CREATORS) {
                SiteChipRow(
                    options = CREATOR_ORDERS,
                    selected = CREATOR_ORDERS.first { it.key == orderKey },
                    label = { context.getString(it.titleRes) },
                    onSelect = { orderKey = it.key },
                    key = { it.key },
                )
            }
            RedGifsPagedList(
                reloadKey = mode to orderKey,
                load = { page ->
                    when (mode) {
                        RedGifsUsersMode.CREATORS -> RedGifsRepo.creators(orderKey, page)
                        RedGifsUsersMode.FOLLOWING -> RedGifsRepo.myFollowing(page)
                        RedGifsUsersMode.FOLLOWERS -> RedGifsRepo.myFollowers(page)
                    }
                },
                itemKey = { it.username },
                emptyHint = stringResource(R.string.redgifs_empty),
            ) { user -> RedGifsUserRow(user) { onOpenUser(user) } }
        }
    }
}
