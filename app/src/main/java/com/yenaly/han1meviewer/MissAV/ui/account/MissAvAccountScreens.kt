package com.yenaly.han1meviewer.MissAV.ui.account

import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.SiteHubRow
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items as columnItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.MissAV.data.model.MissAvActress
import com.yenaly.han1meviewer.MissAV.data.model.MissAvActressProfile
import com.yenaly.han1meviewer.MissAV.data.model.MissAvTweet
import com.yenaly.han1meviewer.MissAV.viewmodel.MissAvAccountViewModel
import com.yenaly.han1meviewer.MissAV.viewmodel.PagedLoader
import com.yenaly.han1meviewer.MissAV.viewmodel.PagedUiState
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteEmptyContent
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import com.yenaly.han1meviewer.ui.component.VideoCardItem

// ---------------------------------------------------------------------------------------------
// Shared building blocks
// ---------------------------------------------------------------------------------------------

@Composable
private fun LoginRequired(onLogin: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.AccountCircle, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.missav_acc_login_required), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.missav_acc_login_required_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onLogin) { Text(stringResource(R.string.missav_acc_login_btn)) }
        }
    }
}

@Composable
private fun LoadMoreEffect(state: LazyGridState, canLoadMore: Boolean, onLoadMore: () -> Unit) {
    val shouldLoad by remember(state) {
        derivedStateOf {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 4
        }
    }
    LaunchedEffect(shouldLoad, canLoadMore) { if (shouldLoad && canLoadMore) onLoadMore() }
}

/** Paginated grid with pull-to-refresh, loading, error and empty states. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> PagedGrid(
    loader: PagedLoader<T>,
    key: (T) -> Any,
    columns: Int,
    emptyHint: String,
    header: (@Composable () -> Unit)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    val state by loader.state.collectAsStateWithLifecycle()
    LaunchedEffect(loader) { loader.loadIfNeeded() }
    val gridState = rememberLazyGridState()
    LoadMoreEffect(gridState, state.hasNext && !state.loadingMore && !state.loading) { loader.loadMore() }

    Column(Modifier.fillMaxSize()) {
        header?.invoke()
        when {
            state.loading && state.items.isEmpty() -> SiteLoadingContent()
            state.error != null && state.items.isEmpty() ->
                SiteErrorContent(message = state.error, onRetry = { loader.refresh() })
            state.items.isEmpty() -> SiteEmptyContent(hint = emptyHint)
            else -> PullToRefreshBox(isRefreshing = state.loading, onRefresh = { loader.refresh() }, modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.items, key = key) { itemContent(it) }
                    if (state.loadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoGridItem(video: HanimeInfo, onVideo: (String, String) -> Unit) {
    VideoCardItem(
        videoItem = video,
        isHorizontalCard = true,
        onClickVideosItem = { code -> onVideo(code, "/en/$code") },
        onLongClickVideosItem = { _, _ -> },
    )
}

@Composable
private fun ActressItem(actress: MissAvActress, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AsyncImage(
            model = actress.avatarUrl,
            contentDescription = actress.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(88.dp).clip(CircleShape),
        )
        Text(actress.name, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

// ---------------------------------------------------------------------------------------------
// Login
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissAvLoginScreen(
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
    viewModel: MissAvAccountViewModel = viewModel(),
) {
    val ui by viewModel.login.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var remember by rememberSaveable { mutableStateOf(true) }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val canSubmit = login.isNotBlank() && password.isNotEmpty() && !ui.loading
    val submit = { if (canSubmit) viewModel.login(login, password, remember, onLoggedIn) }

    Scaffold(topBar = {
        SiteTopAppBar(title = { Text(stringResource(R.string.missav_acc_login_title)) }, navigationIcon = { SiteBackButton(onClick = onBack) })
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(Icons.Default.AccountCircle, null, Modifier.size(72.dp).align(Alignment.CenterHorizontally), tint = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = login, onValueChange = { login = it; viewModel.clearLoginError() },
                label = { Text(stringResource(R.string.missav_acc_login_field)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it; viewModel.clearLoginError() },
                label = { Text(stringResource(R.string.missav_acc_password)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
            )
            Row(Modifier.fillMaxWidth().clickable { remember = !remember }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = remember, onCheckedChange = { remember = it })
                Text(stringResource(R.string.missav_acc_remember))
            }
            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            Button(onClick = { submit() }, enabled = canSubmit, modifier = Modifier.fillMaxWidth()) {
                if (ui.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.missav_acc_login_btn))
            }
            TextButton(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Preferences.missAvBaseUrl.trimEnd('/') + "/en")))
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text(stringResource(R.string.missav_acc_register_web)) }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Account hub
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissAvAccountScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onSaved: () -> Unit,
    onSavedActresses: () -> Unit,
    onPlaylists: () -> Unit,
    onSiteHistory: () -> Unit,
    onLocalHistory: () -> Unit,
    onActresses: () -> Unit,
    viewModel: MissAvAccountViewModel = viewModel(),
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        SiteTopAppBar(title = { Text(stringResource(R.string.missav_acc_account_title)) }, navigationIcon = { SiteBackButton(onClick = onBack) })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Card(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(Icons.Default.AccountCircle, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        val u = user
                        if (u != null) {
                            Text(u.displayName.ifBlank { stringResource(R.string.missav_acc_account_title) }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.missav_acc_logged_in), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(stringResource(R.string.missav_acc_not_logged_in), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.missav_acc_login_to_save), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (user == null) FilledTonalButton(onClick = onLogin) { Text(stringResource(R.string.missav_acc_login_btn)) }
                }
            }
            AccountRow(Icons.Default.Bookmark, R.string.missav_acc_saved, onSaved)
            AccountRow(Icons.Default.Favorite, R.string.missav_acc_saved_actresses, onSavedActresses)
            AccountRow(Icons.AutoMirrored.Filled.PlaylistPlay, R.string.missav_acc_playlists, onPlaylists)
            AccountRow(Icons.Default.History, R.string.missav_acc_site_history, onSiteHistory)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            AccountRow(Icons.Default.Face, R.string.missav_acc_actresses, onActresses)
            AccountRow(Icons.Default.History, R.string.missav_acc_local_history, onLocalHistory)
            if (user != null) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) { Text(stringResource(R.string.missav_acc_logout)) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AccountRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: Int, onClick: () -> Unit) {
    // Same row style as the Library / Profile hub pages.
    val label = stringResource(title)
    SiteHubRow(SiteHubEntry(key = label, title = label, icon = icon, onClick = onClick))
}

// ---------------------------------------------------------------------------------------------
// Saved (videos / actresses)
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissAvSavedScreen(
    initialTab: Int,
    onBack: () -> Unit,
    onNavigateToVideo: (String, String) -> Unit,
    onNavigateToActress: (path: String, name: String) -> Unit,
    onLogin: () -> Unit,
    viewModel: MissAvAccountViewModel = viewModel(),
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    Scaffold(topBar = {
        SiteTopAppBar(title = { Text(stringResource(R.string.missav_acc_saved)) }, navigationIcon = { SiteBackButton(onClick = onBack) })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (user == null) { LoginRequired(onLogin); return@Column }
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.missav_acc_tab_videos)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.missav_acc_tab_actresses)) })
            }
            if (tab == 0) {
                PagedGrid(viewModel.saved, { it.videoCode }, 2, stringResource(R.string.missav_acc_empty)) { VideoGridItem(it, onNavigateToVideo) }
            } else {
                PagedGrid(viewModel.savedActresses, { it.path }, 3, stringResource(R.string.missav_acc_empty)) { a ->
                    ActressItem(a) { onNavigateToActress(a.path, a.name) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Account watch history
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissAvSiteHistoryScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String, String) -> Unit,
    onLogin: () -> Unit,
    viewModel: MissAvAccountViewModel = viewModel(),
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val state by viewModel.history.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirm by remember { mutableStateOf(false) }
    Scaffold(topBar = {
        SiteTopAppBar(
            title = { Text(stringResource(R.string.missav_acc_site_history)) },
            navigationIcon = { SiteBackButton(onClick = onBack) },
            actions = {
                if (user != null && state.items.isNotEmpty()) IconButton(onClick = { confirm = true }) {
                    Icon(Icons.Default.DeleteSweep, stringResource(R.string.missav_acc_clear_history))
                }
            },
        )
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (user == null) LoginRequired(onLogin)
            else PagedGrid(viewModel.history, { it.videoCode }, 2, stringResource(R.string.missav_acc_empty)) { VideoGridItem(it, onNavigateToVideo) }
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text(stringResource(R.string.missav_acc_clear_history)) },
        text = { Text(stringResource(R.string.missav_acc_clear_history_confirm)) },
        confirmButton = {
            TextButton(onClick = {
                confirm = false
                viewModel.clearHistory { msg -> Toast.makeText(context, context.getString(R.string.missav_acc_action_failed, msg), Toast.LENGTH_SHORT).show() }
            }) { Text(stringResource(R.string.missav_acc_ok)) }
        },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(android.R.string.cancel)) } },
    )
}

// ---------------------------------------------------------------------------------------------
// Playlists
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissAvPlaylistsScreen(
    onBack: () -> Unit,
    onOpenPlaylist: (key: String, name: String) -> Unit,
    onCreate: () -> Unit,
    onLogin: () -> Unit,
    viewModel: MissAvAccountViewModel = viewModel(),
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val state by viewModel.playlists.collectAsStateWithLifecycle()
    LaunchedEffect(user) { if (user != null) viewModel.loadPlaylists(force = true) }
    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.missav_acc_playlists)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
                actions = { if (user != null) IconButton(onClick = onCreate) { Icon(Icons.Default.Add, stringResource(R.string.missav_acc_create_playlist)) } },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                user == null -> LoginRequired(onLogin)
                state.loading && state.items.isEmpty() -> SiteLoadingContent()
                state.error != null && state.items.isEmpty() -> SiteErrorContent(message = state.error, onRetry = { viewModel.loadPlaylists(force = true) })
                state.items.isEmpty() -> SiteEmptyContent(hint = stringResource(R.string.missav_acc_no_playlists))
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                    columnItems(state.items, key = { it.key }) { p ->
                        ListItem(
                            headlineContent = { Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = p.info?.let { info -> @Composable { Text(info, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
                            leadingContent = {
                                if (p.coverUrl != null) AsyncImage(p.coverUrl, null, Modifier.size(width = 96.dp, height = 54.dp).clip(MaterialTheme.shapes.small), contentScale = ContentScale.Crop)
                                else Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, Modifier.size(40.dp))
                            },
                            modifier = Modifier.clickable { onOpenPlaylist(p.key, p.name) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissAvPlaylistScreen(
    playlistKey: String,
    playlistName: String,
    onBack: () -> Unit,
    onNavigateToVideo: (String, String) -> Unit,
    onLogin: () -> Unit,
    viewModel: MissAvAccountViewModel = viewModel(),
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        SiteTopAppBar(title = { Text(playlistName.ifBlank { stringResource(R.string.missav_acc_playlists) }, maxLines = 1, overflow = TextOverflow.Ellipsis) }, navigationIcon = { SiteBackButton(onClick = onBack) })
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (user == null) LoginRequired(onLogin)
            else PagedGrid(viewModel.playlistLoader(playlistKey), { it.videoCode }, 2, stringResource(R.string.missav_acc_empty)) { VideoGridItem(it, onNavigateToVideo) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissAvCreatePlaylistScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    viewModel: MissAvAccountViewModel = viewModel(),
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val creating by viewModel.creating.collectAsStateWithLifecycle()
    val error by viewModel.createError.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    Scaffold(topBar = {
        SiteTopAppBar(title = { Text(stringResource(R.string.missav_acc_create_playlist)) }, navigationIcon = { SiteBackButton(onClick = onBack) })
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (user == null) LoginRequired(onLogin) else Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.missav_acc_playlist_name)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = { viewModel.createPlaylist(name, onBack) },
                    enabled = name.isNotBlank() && !creating,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (creating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(stringResource(R.string.missav_acc_create))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Actresses
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissAvActressesScreen(
    onBack: () -> Unit,
    onNavigateToActress: (path: String, name: String) -> Unit,
    viewModel: MissAvAccountViewModel = viewModel(),
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val path = if (tab == 0) "/en/actresses" else "/en/actresses/ranking"
    Scaffold(topBar = {
        SiteTopAppBar(title = { Text(stringResource(R.string.missav_acc_actresses)) }, navigationIcon = { SiteBackButton(onClick = onBack) })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.missav_acc_popular)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.missav_acc_ranking)) })
            }
            PagedGrid(viewModel.actressListLoader(path), { it.path }, 3, stringResource(R.string.missav_acc_empty)) { a ->
                ActressItem(a) { onNavigateToActress(a.path, a.name) }
            }
        }
    }
}

@Composable
private fun ActressHero(profile: MissAvActressProfile?, fallbackName: String, id: String?) {
    val avatar = profile?.avatarUrl ?: id?.let { "https://fourhoi.com/actress/$it-t.jpg" }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (avatar != null) {
            AsyncImage(avatar, profile?.name ?: fallbackName, Modifier.size(72.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Default.AccountCircle, null, Modifier.size(72.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(profile?.name ?: fallbackName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            profile?.details?.forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TweetCard(tweet: MissAvTweet) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tweet.text.isNotBlank()) androidx.compose.foundation.text.selection.SelectionContainer {
                Text(tweet.text, style = MaterialTheme.typography.bodyMedium)
            }
            tweet.media.forEach { url ->
                AsyncImage(
                    model = url, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).clip(MaterialTheme.shapes.medium),
                )
            }
            val time = tweet.createdAtHuman.ifBlank { tweet.createdAt }
            if (time.isNotBlank()) Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Paginated vertical list with the same loading / error / empty handling as [PagedGrid]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> PagedList(
    loader: PagedLoader<T>,
    key: (T) -> Any,
    emptyHint: String,
    itemContent: @Composable (T) -> Unit,
) {
    val state by loader.state.collectAsStateWithLifecycle()
    LaunchedEffect(loader) { loader.loadIfNeeded() }
    val listState = rememberLazyListState()
    val shouldLoad by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(shouldLoad, state.hasNext, state.loadingMore, state.loading) {
        if (shouldLoad && state.hasNext && !state.loadingMore && !state.loading) loader.loadMore()
    }
    when {
        state.loading && state.items.isEmpty() -> SiteLoadingContent()
        state.error != null && state.items.isEmpty() -> SiteErrorContent(message = state.error, onRetry = { loader.refresh() })
        state.items.isEmpty() -> SiteEmptyContent(hint = emptyHint)
        else -> PullToRefreshBox(isRefreshing = state.loading, onRefresh = { loader.refresh() }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                columnItems(state.items, key = key) { itemContent(it) }
                if (state.loadingMore) item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissAvActressScreen(
    path: String,
    name: String,
    onBack: () -> Unit,
    onNavigateToVideo: (String, String) -> Unit,
    onLogin: () -> Unit,
    viewModel: MissAvAccountViewModel = viewModel(),
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val detail = viewModel.actressDetail(path)
    val id by detail.id.collectAsStateWithLifecycle()
    val profile by detail.profile.collectAsStateWithLifecycle()
    val saved by detail.saved.collectAsStateWithLifecycle()
    val busy by detail.busy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(id, user) { if (id != null) viewModel.refreshActressSaved(path) }
    Scaffold(topBar = {
        SiteTopAppBar(
            title = { Text(profile?.name ?: name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { SiteBackButton(onClick = onBack) },
            actions = {
                if (id != null) {
                    if (busy) CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.dp)
                    else IconButton(onClick = {
                        if (user == null) onLogin()
                        else viewModel.toggleActressSaved(path) { msg ->
                            Toast.makeText(context, context.getString(R.string.missav_acc_action_failed, msg), Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            if (saved == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            stringResource(if (saved == true) R.string.missav_acc_saved_state else R.string.missav_acc_save),
                            tint = if (saved == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ActressHero(profile, name, id)
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.missav_acc_tab_videos)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.missav_acc_tab_news)) })
            }
            if (tab == 0) {
                PagedGrid(detail.loader, { it.videoCode }, 2, stringResource(R.string.missav_acc_empty)) { VideoGridItem(it, onNavigateToVideo) }
            } else {
                PagedList(detail.news, { it.id }, stringResource(R.string.missav_acc_news_empty)) { TweetCard(it) }
            }
        }
    }
}
