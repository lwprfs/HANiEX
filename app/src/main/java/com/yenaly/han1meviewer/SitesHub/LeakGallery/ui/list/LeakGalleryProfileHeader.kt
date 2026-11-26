package com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryImageLoader
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGallerySession
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryProfileInfo
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote.LeakGalleryAuth
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote.LeakGalleryRepo
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.SiteAvatar
import kotlinx.coroutines.launch

/**
 * Creator profile header: avatar, name, Follow button and "Watch in shorts" (the website's
 * "Watch in TikTok version" link on a profile page, which opens `/tiktok/<username>`).
 * [refreshKey] should change when the profile page finished loading so the cached profile
 * block (id, follow state) is picked up.
 */
@Composable
fun LeakGalleryProfileHeader(
    username: String,
    refreshKey: Int,
    onOpenShorts: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageLoader = remember { LeakGalleryImageLoader.get(context) }
    val epoch by LeakGallerySession.epoch.collectAsStateWithLifecycle()
    val loggedIn = remember(epoch) { LeakGalleryAuth.isLoggedIn }

    var info by remember(username) { mutableStateOf(LeakGalleryRepo.cachedProfile(username)) }
    var busy by remember { mutableStateOf(false) }
    val failed = stringResource(R.string.site_shorts_action_failed)

    LaunchedEffect(username, refreshKey, loggedIn) {
        info = LeakGalleryRepo.cachedProfile(username)
            ?: LeakGalleryRepo.profileInfo(username).getOrNull()
    }

    Column(modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SiteAvatar(
                url = info?.avatarUrl?.ifBlank { null },
                name = username,
                size = 56.dp,
                imageLoader = imageLoader,
            )
            Column(Modifier.weight(1f)) {
                Text(username, style = MaterialTheme.typography.titleMedium)
                info?.subscribers?.let {
                    Text(
                        stringResource(R.string.leakgallery_followers_count, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val current: LeakGalleryProfileInfo? = info
            if (current != null) {
                val following = current.subscribed && loggedIn
                val onClick: () -> Unit = {
                    if (!loggedIn) {
                        onLogin()
                    } else if (!busy) {
                        busy = true
                        scope.launch {
                            val target = !current.subscribed
                            LeakGalleryRepo.setFollowing(current.id, target)
                                .onSuccess {
                                    LeakGalleryRepo.markFollowing(username, target)
                                    info = current.copy(subscribed = target)
                                }
                                .onFailure {
                                    GlobalToasts.show(failed, level = GlobalToasts.ToastLevel.ERROR)
                                }
                            busy = false
                        }
                    }
                }
                if (following) {
                    OutlinedButton(onClick = onClick, enabled = !busy) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Text(
                            stringResource(R.string.leakgallery_following_state),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                } else {
                    Button(onClick = onClick, enabled = !busy) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null)
                        Text(
                            stringResource(R.string.leakgallery_follow),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
            Button(
                onClick = onOpenShorts,
                colors = ButtonDefaults.filledTonalButtonColors(),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(
                    stringResource(R.string.leakgallery_watch_shorts),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}
