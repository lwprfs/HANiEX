package com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.account

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryConstants
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryImageLoader
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGallerySession
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryMe
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryPremium
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote.LeakGalleryAuth
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote.LeakGalleryRepo
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SiteAvatar
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import com.yenaly.han1meviewer.ui.component.content.LoginLockedContent
import kotlinx.coroutines.launch

@Composable
fun LeakGalleryAccountScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onOpenLiked: () -> Unit,
    onOpenFollowing: () -> Unit,
    onOpenFollowedShorts: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageLoader = remember { LeakGalleryImageLoader.get(context) }
    val epoch by LeakGallerySession.epoch.collectAsStateWithLifecycle()
    val loggedIn = remember(epoch) { LeakGalleryAuth.isLoggedIn }

    var me by remember { mutableStateOf<LeakGalleryMe?>(null) }
    var premium by remember { mutableStateOf<LeakGalleryPremium?>(null) }
    var unread by remember { mutableIntStateOf(0) }
    var stats by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }

    LaunchedEffect(loggedIn) {
        me = null
        premium = null
        unread = 0
        stats = emptyList()
        if (!loggedIn) return@LaunchedEffect
        LeakGalleryRepo.me().onSuccess { me = it }
        LeakGalleryRepo.premium().onSuccess { premium = it }
        LeakGalleryRepo.unreadNotifications().onSuccess { unread = it }
        LeakGalleryRepo.statistics().onSuccess { stats = it }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.leakgallery_account)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        if (!loggedIn) {
            LoginLockedContent(onSignIn = onLogin, modifier = Modifier.padding(padding))
            return@Scaffold
        }
        val name = me?.username ?: LeakGalleryAuth.username.orEmpty()
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SiteAvatar(
                    url = me?.avatarUrl?.ifBlank { null } ?: LeakGalleryAuth.avatarUrl,
                    name = name.ifBlank { "?" },
                    size = 64.dp,
                    imageLoader = imageLoader,
                )
                Column {
                    Text(name, style = MaterialTheme.typography.titleLarge)
                    (me?.email ?: LeakGalleryAuth.email)?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    me?.createdAt?.takeIf { it.length >= 10 }?.let {
                        Text(
                            stringResource(R.string.leakgallery_member_since, it.take(10)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    premium?.let { p ->
                        Text(
                            if (p.active) {
                                stringResource(R.string.leakgallery_premium_active, p.until.take(10))
                            } else stringResource(R.string.leakgallery_premium_none),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            if (stats.isNotEmpty()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    stats.forEach { (key, value) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(
                                prettyKey(key),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(value.toString(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            SettingNavigationItem(
                title = stringResource(R.string.leakgallery_notifications_title),
                summary = if (unread > 0) stringResource(R.string.leakgallery_notifications_unread, unread) else null,
                onClick = onOpenNotifications,
            )
            SettingNavigationItem(
                title = stringResource(R.string.leakgallery_liked_title),
                onClick = onOpenLiked,
            )
            SettingNavigationItem(
                title = stringResource(R.string.leakgallery_following_title),
                onClick = onOpenFollowing,
            )
            SettingNavigationItem(
                title = stringResource(R.string.leakgallery_shorts_following),
                onClick = onOpenFollowedShorts,
            )
            SettingNavigationItem(
                title = stringResource(R.string.leakgallery_settings_title),
                summary = stringResource(R.string.leakgallery_settings_summary),
                onClick = onOpenSettings,
            )
            SettingNavigationItem(
                title = stringResource(R.string.leakgallery_manage_on_web),
                summary = stringResource(R.string.leakgallery_manage_on_web_summary),
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, "${LeakGalleryConstants.BASE_URL}/edit-profile".toUri()),
                        )
                    }
                },
            )
            OutlinedButton(
                onClick = { scope.launch { LeakGalleryRepo.logout() } },
                modifier = Modifier.padding(16.dp),
            ) { Text(stringResource(R.string.leakgallery_sign_out)) }
        }
    }
}

private fun prettyKey(key: String): String =
    key.replace(Regex("([a-z])([A-Z])"), "$1 $2").replace('_', ' ')
        .replaceFirstChar { it.uppercase() }
