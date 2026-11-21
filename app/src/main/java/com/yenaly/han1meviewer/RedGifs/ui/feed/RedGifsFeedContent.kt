package com.yenaly.han1meviewer.RedGifs.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.RedGifs.common.RedGifsImageLoader
import com.yenaly.han1meviewer.RedGifs.common.RedGifsSession
import com.yenaly.han1meviewer.RedGifs.data.model.RedGifsGif
import com.yenaly.han1meviewer.RedGifs.data.remote.RedGifsAuth
import com.yenaly.han1meviewer.RedGifs.data.remote.RedGifsFeedSource
import com.yenaly.han1meviewer.RedGifs.data.remote.RedGifsRepo
import com.yenaly.han1meviewer.RedGifs.ui.shorts.RedGifsShortsPlayer
import com.yenaly.han1meviewer.RedGifs.ui.shorts.toSiteShort
import com.yenaly.han1meviewer.ui.component.SiteEmptyContent
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortsGrid
import kotlinx.coroutines.launch

/**
 * One paged grid of RedGifs clips that opens the shared vertical shorts player.
 * Used by the home tabs, niche/creator pages, search and the library lists.
 * Pass `key(source)` at the call site when the source changes.
 */
@Composable
fun RedGifsFeedContent(
    source: RedGifsFeedSource,
    modifier: Modifier = Modifier,
    onLogin: () -> Unit = {},
) {
    val context = LocalContext.current
    val imageLoader = remember { RedGifsImageLoader.get(context) }
    val scope = rememberCoroutineScope()
    val epoch by RedGifsSession.epoch.collectAsStateWithLifecycle()
    val loggedIn = remember(epoch) { RedGifsAuth.isLoggedIn }
    val blocked = source.requiresLogin && !loggedIn

    var gifs by remember { mutableStateOf<List<RedGifsGif>>(emptyList()) }
    var page by remember { mutableIntStateOf(1) }
    var hasNext by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(source, reload, epoch) {
        if (blocked) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        error = null
        RedGifsRepo.feed(source, 1)
            .onSuccess { result ->
                gifs = result.gifs.distinctBy { it.id }
                page = 1
                hasNext = result.hasNext && result.gifs.isNotEmpty()
            }
            .onFailure {
                gifs = emptyList()
                hasNext = false
                error = it.message ?: it.javaClass.simpleName
            }
        loading = false
    }

    fun loadMore() {
        if (!hasNext || loadingMore || loading) return
        loadingMore = true
        scope.launch {
            RedGifsRepo.feed(source, page + 1)
                .onSuccess { result ->
                    val known = gifs.map { it.id }.toHashSet()
                    val fresh = result.gifs.filter { it.id !in known }.distinctBy { it.id }
                    gifs = gifs + fresh
                    page += 1
                    hasNext = result.hasNext && fresh.isNotEmpty()
                }
                .onFailure { hasNext = false }
            loadingMore = false
        }
    }

    val shorts = remember(gifs) { gifs.map { it.toSiteShort() } }

    Box(modifier.fillMaxSize()) {
        when {
            blocked -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.redgifs_login_required),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onLogin, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.redgifs_sign_in))
                }
            }
            loading && gifs.isEmpty() -> SiteLoadingContent()
            gifs.isEmpty() && error != null -> SiteErrorContent(message = error, onRetry = { reload++ })
            gifs.isEmpty() -> SiteEmptyContent(hint = stringResource(R.string.redgifs_empty))
            else -> SiteShortsGrid(
                site = com.yenaly.han1meviewer.SiteType.REDGIFS,
                items = shorts,
                onOpen = { playing = it },
                imageLoader = imageLoader,
                hasMore = hasNext,
                loadingMore = loadingMore,
                onLoadMore = { loadMore() },
            )
        }
    }

    playing?.let { start ->
        RedGifsShortsPlayer(
            items = shorts,
            initialIndex = start,
            hasMore = hasNext,
            onLoadMore = { loadMore() },
            onDismiss = { playing = null },
        )
    }
}
