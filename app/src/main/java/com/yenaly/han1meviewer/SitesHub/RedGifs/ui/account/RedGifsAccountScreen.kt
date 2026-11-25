package com.yenaly.han1meviewer.SitesHub.RedGifs.ui.account

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SitesHub.RedGifs.common.RedGifsConstants
import com.yenaly.han1meviewer.SitesHub.RedGifs.common.RedGifsImageLoader
import com.yenaly.han1meviewer.SitesHub.RedGifs.common.RedGifsSession
import com.yenaly.han1meviewer.SitesHub.RedGifs.data.model.RedGifsMe
import com.yenaly.han1meviewer.SitesHub.RedGifs.data.remote.RedGifsAuth
import com.yenaly.han1meviewer.SitesHub.RedGifs.data.remote.RedGifsRepo
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SiteAvatar
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.content.LoginLockedContent
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar

@Composable
fun RedGifsAccountScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onOpenFollowing: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowedNiches: () -> Unit,
    onOpenLiked: () -> Unit,
    onOpenMine: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDashboard: () -> Unit = {},
    onOpenEditProfile: () -> Unit = {},
    onOpenCollections: () -> Unit = {},
) {
    val context = LocalContext.current
    val imageLoader = remember { RedGifsImageLoader.get(context) }
    val epoch by RedGifsSession.epoch.collectAsStateWithLifecycle()
    val loggedIn = remember(epoch) { RedGifsAuth.isLoggedIn }
    var me by remember { mutableStateOf<RedGifsMe?>(null) }

    LaunchedEffect(epoch, loggedIn) {
        me = null
        if (loggedIn) RedGifsRepo.me().onSuccess { me = it }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.redgifs_account)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        if (!loggedIn) {
            LoginLockedContent(onSignIn = onLogin, modifier = Modifier.padding(padding))
        } else {
            val user = me?.user
            val name = user?.username ?: RedGifsAuth.username.orEmpty()
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SiteAvatar(
                        url = user?.avatarUrl ?: RedGifsAuth.avatarUrl,
                        name = name.ifBlank { "?" },
                        size = 64.dp,
                        imageLoader = imageLoader,
                    )
                    Column {
                        Text(name, style = MaterialTheme.typography.titleLarge)
                        me?.email?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (user != null) {
                            Text(
                                stringResource(
                                    R.string.redgifs_account_stats,
                                    user.followers, user.following, user.gifs, user.likes,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                SettingNavigationItem(title = stringResource(R.string.redgifs_dashboard), onClick = onOpenDashboard)
                SettingNavigationItem(title = stringResource(R.string.redgifs_edit_profile), onClick = onOpenEditProfile)
                SettingNavigationItem(title = stringResource(R.string.redgifs_collections), onClick = onOpenCollections)
                SettingNavigationItem(title = stringResource(R.string.redgifs_following), onClick = onOpenFollowing)
                SettingNavigationItem(title = stringResource(R.string.redgifs_followers), onClick = onOpenFollowers)
                SettingNavigationItem(title = stringResource(R.string.redgifs_followed_niches), onClick = onOpenFollowedNiches)
                SettingNavigationItem(title = stringResource(R.string.redgifs_liked), onClick = onOpenLiked)
                SettingNavigationItem(title = stringResource(R.string.redgifs_my_uploads), onClick = onOpenMine)
                SettingNavigationItem(title = stringResource(R.string.redgifs_settings_title), onClick = onOpenSettings)
                SettingNavigationItem(
                    title = stringResource(R.string.redgifs_manage_on_web),
                    summary = stringResource(R.string.redgifs_manage_on_web_summary),
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(RedGifsConstants.BASE_URL)),
                            )
                        }
                    },
                )
                OutlinedButton(
                    onClick = { RedGifsAuth.logout() },
                    modifier = Modifier.padding(16.dp),
                ) { Text(stringResource(R.string.redgifs_sign_out)) }
            }
        }
    }
}
