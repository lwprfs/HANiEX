package com.yenaly.han1meviewer.SitesHub.RedGifs.ui.feed

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yenaly.han1meviewer.SitesHub.RedGifs.data.model.RedGifsGif
import com.yenaly.han1meviewer.SitesHub.RedGifs.data.remote.RedGifsFeedSource
import com.yenaly.han1meviewer.SitesHub.RedGifs.data.remote.RedGifsRepo
import com.yenaly.han1meviewer.SitesHub.RedGifs.ui.shorts.RedGifsShortsPlayer
import com.yenaly.han1meviewer.SitesHub.RedGifs.ui.shorts.toSiteShort
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar

@Composable
fun RedGifsFeedScreen(
    source: RedGifsFeedSource,
    title: String,
    onBack: () -> Unit,
    onLogin: () -> Unit,
) {
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
            RedGifsFeedContent(source = source, onLogin = onLogin)
        }
    }
}

@Composable
fun RedGifsGifScreen(id: String, onBack: () -> Unit) {
    var gif by remember(id) { mutableStateOf<RedGifsGif?>(null) }
    var error by remember(id) { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(id, reload) {
        error = null
        RedGifsRepo.gif(id)
            .onSuccess { gif = it }
            .onFailure { error = it.message ?: it.javaClass.simpleName }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = { Text(id) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when {
                gif == null && error != null -> SiteErrorContent(message = error, onRetry = { reload++ })
                gif == null -> SiteLoadingContent()
            }
        }
    }

    gif?.let { g ->
        RedGifsShortsPlayer(
            items = listOf(g.toSiteShort()),
            initialIndex = 0,
            onDismiss = onBack,
        )
    }
}
