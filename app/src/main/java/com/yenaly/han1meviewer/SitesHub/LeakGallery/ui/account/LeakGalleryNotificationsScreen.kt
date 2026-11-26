package com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGallerySession
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryNotification
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote.LeakGalleryAuth
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote.LeakGalleryRepo
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteEmptyContent
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import com.yenaly.han1meviewer.ui.component.content.LoginLockedContent
import kotlinx.coroutines.launch

@Composable
fun LeakGalleryNotificationsScreen(onBack: () -> Unit, onLogin: () -> Unit) {
    val scope = rememberCoroutineScope()
    val epoch by LeakGallerySession.epoch.collectAsStateWithLifecycle()
    val loggedIn = remember(epoch) { LeakGalleryAuth.isLoggedIn }
    var items by remember { mutableStateOf<List<LeakGalleryNotification>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(loggedIn, reload) {
        if (!loggedIn) return@LaunchedEffect
        items = null
        error = null
        LeakGalleryRepo.notifications(1)
            .onSuccess { items = it }
            .onFailure { error = it.message ?: it.javaClass.simpleName }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.leakgallery_notifications_title)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
                actions = {
                    if (loggedIn) {
                        TextButton(onClick = {
                            scope.launch {
                                LeakGalleryRepo.markAllNotificationsRead()
                                reload++
                            }
                        }) { Text(stringResource(R.string.leakgallery_mark_all_read)) }
                    }
                },
            )
        },
    ) { padding ->
        val list = items
        when {
            !loggedIn -> LoginLockedContent(onSignIn = onLogin, modifier = Modifier.padding(padding))
            list == null && error != null ->
                SiteErrorContent(message = error, onRetry = { reload++ }, modifier = Modifier.padding(padding))
            list == null -> SiteLoadingContent(modifier = Modifier.padding(padding))
            list.isEmpty() ->
                Column(Modifier.padding(padding)) {
                    SiteEmptyContent(hint = stringResource(R.string.leakgallery_notifications_empty))
                }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(list, key = { it.id }) { n ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(
                            n.text,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (n.read) FontWeight.Normal else FontWeight.Bold,
                        )
                        if (n.createdAt.isNotBlank()) {
                            Text(
                                n.createdAt.take(10),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
