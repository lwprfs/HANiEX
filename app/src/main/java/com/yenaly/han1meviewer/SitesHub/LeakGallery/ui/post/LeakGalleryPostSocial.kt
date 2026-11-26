package com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.post

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryConstants
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGallerySession
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryDetail
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryProfileInfo
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote.LeakGalleryAuth
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote.LeakGalleryRepo
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import kotlinx.coroutines.launch

/**
 * Like / Follow / "Watch as shorts" row on the media page. The shorts button opens the same feed
 * the website uses for a creator (`/tiktok/<username>`), starting on this video.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LeakGalleryPostSocialRow(
    info: LeakGalleryDetail,
    onOpenShorts: (mode: String, startId: String) -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val epoch by LeakGallerySession.epoch.collectAsStateWithLifecycle()
    val loggedIn = remember(epoch) { LeakGalleryAuth.isLoggedIn }
    val failed = stringResource(R.string.site_shorts_action_failed)

    var liked by remember(info.slug) { mutableStateOf(info.liked) }
    var likes by remember(info.slug) { mutableStateOf(info.likes) }
    var likeBusy by remember { mutableStateOf(false) }

    val creatorName = info.creator?.name
    var profile by remember(creatorName) {
        mutableStateOf<LeakGalleryProfileInfo?>(creatorName?.let { LeakGalleryRepo.cachedProfile(it) })
    }
    var followBusy by remember { mutableStateOf(false) }
    LaunchedEffect(creatorName, loggedIn) {
        if (creatorName != null) {
            profile = LeakGalleryRepo.profileInfo(creatorName).getOrNull() ?: profile
        }
    }

    FlowRow(
        modifier = modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (info.hasVideo) {
            FilledTonalButton(onClick = {
                onOpenShorts(creatorName ?: LeakGalleryConstants.SHORTS_TRENDING, info.slug)
            }) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(stringResource(R.string.leakgallery_watch_shorts), Modifier.padding(start = 6.dp))
            }
        }

        OutlinedButton(
            enabled = !likeBusy,
            onClick = {
                if (!loggedIn) {
                    onLogin()
                    return@OutlinedButton
                }
                likeBusy = true
                val target = !liked
                scope.launch {
                    LeakGalleryRepo.setLiked(info.slug, target)
                        .onSuccess { count ->
                            liked = target
                            likes = count ?: (likes + if (target) 1 else -1).coerceAtLeast(0)
                            LeakGalleryRepo.updateShort(info.slug) { it.copy(liked = target, likes = likes) }
                        }
                        .onFailure { GlobalToasts.show(failed, level = GlobalToasts.ToastLevel.ERROR) }
                    likeBusy = false
                }
            },
        ) {
            Icon(
                if (liked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                contentDescription = null,
            )
            Text(likes.toString(), Modifier.padding(start = 6.dp))
        }

        val current = profile
        if (current != null && creatorName != null) {
            val following = current.subscribed && loggedIn
            val action: () -> Unit = {
                if (!loggedIn) {
                    onLogin()
                } else if (!followBusy) {
                    followBusy = true
                    val target = !current.subscribed
                    scope.launch {
                        LeakGalleryRepo.setFollowing(current.id, target)
                            .onSuccess {
                                LeakGalleryRepo.markFollowing(creatorName, target)
                                profile = current.copy(subscribed = target)
                            }
                            .onFailure { GlobalToasts.show(failed, level = GlobalToasts.ToastLevel.ERROR) }
                        followBusy = false
                    }
                }
            }
            if (following) {
                OutlinedButton(onClick = action, enabled = !followBusy) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Text(stringResource(R.string.leakgallery_following_state), Modifier.padding(start = 6.dp))
                }
            } else {
                OutlinedButton(onClick = action, enabled = !followBusy) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null)
                    Text(stringResource(R.string.leakgallery_follow), Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}
