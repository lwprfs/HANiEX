package com.yenaly.han1meviewer.RedGifs.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.RedGifs.data.model.RedGifsCollection
import com.yenaly.han1meviewer.RedGifs.data.model.RedGifsListPage
import com.yenaly.han1meviewer.RedGifs.data.model.RedGifsMe
import com.yenaly.han1meviewer.RedGifs.data.remote.RedGifsAuth
import com.yenaly.han1meviewer.RedGifs.data.remote.RedGifsRepo
import com.yenaly.han1meviewer.RedGifs.ui.browse.RedGifsPagedList
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import kotlinx.coroutines.launch

/** `PATCH /v1/me` for display name, bio and website. */
@Composable
fun RedGifsEditProfileScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val saved = stringResource(R.string.redgifs_prefs_saved)
    val failed = stringResource(R.string.site_shorts_action_failed)
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        RedGifsRepo.me().onSuccess {
            name = it.user.displayName
            description = it.user.description
            loaded = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.redgifs_edit_profile)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text(stringResource(R.string.redgifs_profile_name)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text(stringResource(R.string.redgifs_profile_bio)) },
                minLines = 3, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = website, onValueChange = { website = it },
                label = { Text(stringResource(R.string.redgifs_profile_website)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Button(
                enabled = loaded && !saving,
                onClick = {
                    saving = true
                    scope.launch {
                        RedGifsRepo.updateProfile(name.trim(), description.trim(), website.trim())
                            .onSuccess { GlobalToasts.show(saved, level = GlobalToasts.ToastLevel.SUCCESS) }
                            .onFailure { GlobalToasts.show(it.message ?: failed, level = GlobalToasts.ToastLevel.ERROR) }
                        saving = false
                    }
                },
            ) { Text(stringResource(R.string.redgifs_save)) }
        }
    }
}

/** Lists your collections and creates new ones (the HAR has no endpoint for opening one). */
@Composable
fun RedGifsCollectionsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val failed = stringResource(R.string.site_shorts_action_failed)
    var reload by remember { mutableIntStateOf(0) }
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }
    var published by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.redgifs_collections)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
                actions = {
                    TextButton(onClick = { showCreate = true }) { Text(stringResource(R.string.redgifs_new_collection)) }
                },
            )
        },
    ) { padding ->
        RedGifsPagedList(
            reloadKey = reload,
            load = { page ->
                val user = RedGifsAuth.username ?: RedGifsRepo.me().getOrNull()?.user?.username
                if (user == null) Result.failure<RedGifsListPage<RedGifsCollection>>(IllegalStateException("not signed in"))
                else RedGifsRepo.collections(user, page)
            },
            itemKey = { it.folderId },
            emptyHint = stringResource(R.string.redgifs_collections_empty),
            modifier = Modifier.padding(padding),
        ) { c ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(c.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.redgifs_collection_count, c.count) +
                            if (c.published) " · " + stringResource(R.string.redgifs_collection_public) else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (c.description.isNotBlank()) Text(c.description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(stringResource(R.string.redgifs_new_collection)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName, onValueChange = { newName = it },
                        label = { Text(stringResource(R.string.redgifs_profile_name)) }, singleLine = true,
                    )
                    OutlinedTextField(
                        value = newDescription, onValueChange = { newDescription = it },
                        label = { Text(stringResource(R.string.redgifs_profile_bio)) },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Switch(checked = published, onCheckedChange = { published = it })
                        Text(stringResource(R.string.redgifs_collection_public))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        showCreate = false
                        scope.launch {
                            RedGifsRepo.createCollection(newName.trim(), newDescription.trim(), published)
                                .onSuccess {
                                    newName = ""
                                    newDescription = ""
                                    reload++
                                }
                                .onFailure { GlobalToasts.show(it.message ?: failed, level = GlobalToasts.ToastLevel.ERROR) }
                        }
                    },
                ) { Text(stringResource(R.string.redgifs_save)) }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** `GET /v1/tags`; tapping a tag opens a tag search. */
@Composable
fun RedGifsTagsScreen(onBack: () -> Unit, onOpenTag: (String) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.redgifs_tags)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        RedGifsPagedList(
            reloadKey = Unit,
            load = { RedGifsRepo.tags().map { RedGifsListPage(it, 1, 1) } },
            itemKey = { it.first },
            emptyHint = stringResource(R.string.redgifs_empty),
            modifier = Modifier.padding(padding),
        ) { (tag, count) ->
            Row(
                Modifier.fillMaxWidth().clickable { onOpenTag(tag) }.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("#$tag", style = MaterialTheme.typography.titleMedium)
                Text(count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Your numbers plus shortcuts to everything you manage. */
@Composable
fun RedGifsDashboardScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onCollections: () -> Unit,
    onMine: () -> Unit,
    onLiked: () -> Unit,
    onFollowers: () -> Unit,
    onFollowing: () -> Unit,
) {
    var me by remember { mutableStateOf<RedGifsMe?>(null) }
    LaunchedEffect(Unit) { RedGifsRepo.me().onSuccess { me = it } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.redgifs_dashboard)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            me?.user?.let { u ->
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(u.username, style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.redgifs_dash_followers, u.followers))
                    Text(stringResource(R.string.redgifs_dash_following, u.following))
                    Text(stringResource(R.string.redgifs_dash_gifs, u.gifs))
                    Text(stringResource(R.string.redgifs_dash_views, u.views))
                    Text(stringResource(R.string.redgifs_dash_likes, u.likes))
                }
            }
            SettingNavigationItem(title = stringResource(R.string.redgifs_edit_profile), onClick = onEditProfile)
            SettingNavigationItem(title = stringResource(R.string.redgifs_my_uploads), onClick = onMine)
            SettingNavigationItem(title = stringResource(R.string.redgifs_collections), onClick = onCollections)
            SettingNavigationItem(title = stringResource(R.string.redgifs_liked), onClick = onLiked)
            SettingNavigationItem(title = stringResource(R.string.redgifs_followers), onClick = onFollowers)
            SettingNavigationItem(title = stringResource(R.string.redgifs_following), onClick = onFollowing)
        }
    }
}
