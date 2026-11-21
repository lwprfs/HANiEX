package com.yenaly.han1meviewer.RedGifs.ui.creator

import androidx.compose.foundation.layout.Arrangement
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.RedGifs.ui.navigation.LocalRedGifsActions
import androidx.compose.runtime.key
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.RedGifs.common.RedGifsImageLoader
import com.yenaly.han1meviewer.RedGifs.common.RedGifsSession
import com.yenaly.han1meviewer.RedGifs.data.model.RedGifsUser
import com.yenaly.han1meviewer.RedGifs.data.remote.RedGifsAuth
import com.yenaly.han1meviewer.RedGifs.data.remote.RedGifsFeedSource
import com.yenaly.han1meviewer.RedGifs.data.remote.RedGifsRepo
import com.yenaly.han1meviewer.RedGifs.ui.feed.RedGifsFeedContent
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.SiteAvatar
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteSubscribeButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import com.yenaly.han1meviewer.ui.component.SiteVerifiedBadge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RedGifsCreatorScreen(
    name: String,
    onBack: () -> Unit,
    onLogin: () -> Unit,
) {
    val context = LocalContext.current
    val imageLoader = remember { RedGifsImageLoader.get(context) }
    val scope = rememberCoroutineScope()
    val epoch by RedGifsSession.epoch.collectAsStateWithLifecycle()
    val loginRequired = stringResource(R.string.site_shorts_login_required)
    val actionFailed = stringResource(R.string.site_shorts_action_failed)

    var user by remember(name) { mutableStateOf<RedGifsUser?>(RedGifsRepo.cachedUser(name)) }
    var following by remember(name) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var socials by remember(name) { mutableStateOf<List<String>>(emptyList()) }
    var creatorTags by remember(name) { mutableStateOf<List<String>>(emptyList()) }
    var pinned by remember(name) { mutableStateOf(false) }
    val actions = LocalRedGifsActions.current
    val pinsLabel = stringResource(R.string.redgifs_pinned)
    val allLabel = stringResource(R.string.redgifs_tab_new)

    LaunchedEffect(name) {
        RedGifsRepo.user(name).onSuccess { user = it }
        RedGifsRepo.socials(name).onSuccess { socials = it }
        RedGifsRepo.creatorTags(name).onSuccess { creatorTags = it.take(12) }
    }
    LaunchedEffect(name, epoch) {
        following = RedGifsRepo.followingNames().contains(name.lowercase())
    }

    LaunchedEffect(name) {
        while (true) {
            delay(1000)
            if (!busy && RedGifsAuth.isLoggedIn) following = RedGifsRepo.isFollowingCached(name)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val u = user
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SiteAvatar(url = u?.avatarUrl, name = name, size = 56.dp, imageLoader = imageLoader)
                    Column(Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                u?.username ?: name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (u?.verified == true) SiteVerifiedBadge()
                        }
                        if (u != null) {
                            Text(
                                stringResource(R.string.redgifs_user_stats, compactCount(u.followers), compactCount(u.gifs)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    SiteSubscribeButton(
                        subscribed = following,
                        busy = busy,
                        onClick = {
                            if (!RedGifsAuth.isLoggedIn) {
                                GlobalToasts.show(loginRequired, level = GlobalToasts.ToastLevel.ERROR)
                                onLogin()
                                return@SiteSubscribeButton
                            }
                            if (busy) return@SiteSubscribeButton
                            busy = true
                            val want = !following
                            scope.launch {
                                RedGifsRepo.setFollowing(name, want)
                                    .onSuccess { following = want }
                                    .onFailure {
                                        GlobalToasts.show(it.message ?: actionFailed, level = GlobalToasts.ToastLevel.ERROR)
                                    }
                                busy = false
                            }
                        },
                    )
                }
                if (!u?.description.isNullOrBlank()) {
                    Text(
                        u.description.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            if (socials.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    socials.take(4).forEach { url ->
                        Text(
                            url.removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore('/'),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                runCatching {
                                    context.startActivity(
                                        android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)),
                                    )
                                }
                            },
                        )
                    }
                }
            }
            if (creatorTags.isNotEmpty()) {
                SiteChipRow(
                    options = creatorTags,
                    selected = "",
                    label = { "#$it" },
                    onSelect = { actions.openTag(it) },
                    key = { it },
                )
            }
            SiteChipRow(
                options = listOf(false, true),
                selected = pinned,
                label = { if (it) pinsLabel else allLabel },
                onSelect = { pinned = it },
                key = { it },
            )
            key(pinned) {
                RedGifsFeedContent(
                    source = if (pinned) RedGifsFeedSource.Pins(name) else RedGifsFeedSource.Creator(name),
                    onLogin = onLogin,
                )
            }
        }
    }
}

private fun compactCount(n: Long): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format("%.1fK", n / 1_000.0)
    else -> n.toString()
}
