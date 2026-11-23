package com.yenaly.han1meviewer.RedGifs.ui.creator

import com.yenaly.han1meviewer.ui.component.social.SocialActionButton
import com.yenaly.han1meviewer.ui.component.social.SocialButtonStyle
import com.yenaly.han1meviewer.ui.component.social.SocialLinkFlow
import com.yenaly.han1meviewer.ui.component.social.SocialProfileHeader
import com.yenaly.han1meviewer.ui.component.social.SocialStat
import com.yenaly.han1meviewer.ui.component.social.SocialTabRow
import com.yenaly.han1meviewer.ui.component.social.SocialTagFlow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
            val toggleFollow: () -> Unit = {
                if (!RedGifsAuth.isLoggedIn) {
                    GlobalToasts.show(loginRequired, level = GlobalToasts.ToastLevel.ERROR)
                    onLogin()
                } else if (!busy) {
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
                }
            }
            val viewsLikes = listOfNotNull(
                u?.views?.takeIf { it > 0 }?.let { stringResource(R.string.redgifs_dash_views, it) },
                u?.likes?.takeIf { it > 0 }?.let { stringResource(R.string.redgifs_dash_likes, it) },
            ).joinToString(" · ")
            val shownName = u?.displayName?.takeIf { it.isNotBlank() && !it.equals(name, true) }

            Box(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                SocialProfileHeader(
                    name = shownName ?: u?.username ?: name,
                    username = "@${u?.username ?: name}".takeIf { shownName != null },
                    avatarUrl = u?.avatarUrl,
                    verified = u?.verified == true,
                    bio = u?.description?.takeIf { it.isNotBlank() },
                    metaLines = listOf(viewsLikes),
                    imageLoader = imageLoader,
                    actions = {
                        SocialActionButton(
                            label = stringResource(
                                if (following) R.string.site_subscribed else R.string.site_subscribe
                            ),
                            onClick = toggleFollow,
                            style = if (following) SocialButtonStyle.Tonal else SocialButtonStyle.Primary,
                            loading = busy,
                            modifier = Modifier.weight(1f),
                        )
                    },
                    stats = if (u == null) emptyList() else listOf(
                        SocialStat(stringResource(R.string.redgifs_followers), u.followers),
                        SocialStat(stringResource(R.string.site_following), u.following),
                        SocialStat(stringResource(R.string.redgifs_search_gifs), u.gifs),
                    ),
                    extra = if (socials.isEmpty() && creatorTags.isEmpty()) null else {
                        {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SocialLinkFlow(
                                    socials.take(4).map { url ->
                                        url.removePrefix("https://").removePrefix("http://")
                                            .removePrefix("www.").substringBefore('/') to url
                                    },
                                )
                                SocialTagFlow(creatorTags, prefix = "#", onClick = { actions.openTag(it) })
                            }
                        }
                    },
                )
            }
            SocialTabRow(
                labels = listOf(allLabel, pinsLabel),
                selectedIndex = if (pinned) 1 else 0,
                onSelect = { pinned = it == 1 },
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
