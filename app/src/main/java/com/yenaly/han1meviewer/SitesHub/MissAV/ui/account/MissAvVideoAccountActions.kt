@file:Suppress("DEPRECATION")

package com.yenaly.han1meviewer.SitesHub.MissAV.ui.account

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.SitesHub.MissAV.data.model.MissAvPlaylistOption
import com.yenaly.han1meviewer.SitesHub.MissAV.data.model.MissAvVideoInfo
import com.yenaly.han1meviewer.SitesHub.MissAV.data.remote.MissAvAccountRepo
import com.yenaly.han1meviewer.SitesHub.MissAV.data.remote.MissAvAuth
import com.yenaly.han1meviewer.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MissAvVideoAccountActions(
    info: MissAvVideoInfo,
    onLogin: () -> Unit,
    onNavigateToActress: (path: String, name: String) -> Unit,
    onNavigateToPlaylists: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user by MissAvAuth.user.collectAsStateWithLifecycle()
    var saved by remember(info.itemKey) { mutableStateOf<Boolean?>(null) }
    var savingBusy by remember { mutableStateOf(false) }
    var showPlaylists by remember { mutableStateOf(false) }

    fun fail(e: Throwable) {
        Toast.makeText(context, context.getString(R.string.missav_acc_action_failed, e.message ?: e.toString()), Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(info.itemKey, user) {
        val key = info.itemKey
        saved = null
        if (user != null && key != null) {
            runCatching { MissAvAccountRepo.itemState(key) }.onSuccess { saved = it.saved }
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val u = user
        if (u == null) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(R.string.missav_acc_login_to_save),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(onClick = onLogin) { Text(stringResource(R.string.missav_acc_login_btn)) }
            }
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val key = info.itemKey
                FilledTonalButton(
                    enabled = key != null && !savingBusy && saved != null,
                    onClick = {
                        val k = key ?: return@FilledTonalButton
                        val want = saved != true
                        scope.launch {
                            savingBusy = true
                            try { saved = MissAvAccountRepo.setItemSaved(k, want) }
                            catch (e: CancellationException) { throw e }
                            catch (e: Exception) { fail(e) }
                            finally { savingBusy = false }
                        }
                    },
                ) {
                    if (savingBusy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Icon(if (saved == true) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, null, Modifier.size(18.dp))
                    Text(
                        stringResource(if (saved == true) R.string.missav_acc_saved_state else R.string.missav_acc_save),
                        Modifier.padding(start = 6.dp),
                    )
                }
                FilledTonalButton(onClick = { showPlaylists = true }) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, Modifier.size(18.dp))
                    Text(stringResource(R.string.missav_acc_add_playlist), Modifier.padding(start = 6.dp))
                }
                info.magnetUrl?.let { magnet ->
                    FilledTonalButton(onClick = { openMagnet(context, magnet) }) {
                        Icon(Icons.Default.Link, null, Modifier.size(18.dp))
                        Text(stringResource(R.string.missav_acc_magnet), Modifier.padding(start = 6.dp))
                    }
                }
            }
        }

        if (info.actressLinks.isNotEmpty()) {
            Text(stringResource(R.string.missav_acc_actresses), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                info.actressLinks.forEach { a ->
                    AssistChip(onClick = { onNavigateToActress(a.path, a.name) }, label = { Text(a.name) })
                }
            }
        }
    }

    if (showPlaylists) PlaylistSheet(
        dvdId = info.videoCode,
        onDismiss = { showPlaylists = false },
        onManage = { showPlaylists = false; onNavigateToPlaylists() },
        onError = ::fail,
    )
}

@Composable
private fun PlaylistSheet(
    dvdId: String,
    onDismiss: () -> Unit,
    onManage: () -> Unit,
    onError: (Throwable) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val options = remember { mutableStateListOf<MissAvPlaylistOption>() }
    var busyKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(dvdId) {
        loading = true; error = null
        try {
            options.clear(); options.addAll(MissAvAccountRepo.playlistOptions(dvdId))
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: e.toString() }
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.missav_acc_add_playlist)) },
        text = {
            when {
                loading -> CircularProgressIndicator(Modifier.size(28.dp))
                error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                options.isEmpty() -> Text(stringResource(R.string.missav_acc_no_playlists))
                else -> LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(options.toList(), key = { it.key }) { opt ->
                        ListItem(
                            headlineContent = { Text(opt.name) },
                            trailingContent = {
                                if (busyKey == opt.key) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                else Checkbox(checked = opt.isAdded, onCheckedChange = null)
                            },
                            modifier = Modifier.clickable(enabled = busyKey == null) {
                                scope.launch {
                                    busyKey = opt.key
                                    try {
                                        val nowAdded = if (opt.isAdded) MissAvAccountRepo.removeFromPlaylist(dvdId, opt.key)
                                        else MissAvAccountRepo.addToPlaylist(dvdId, opt.key)
                                        val i = options.indexOfFirst { it.key == opt.key }
                                        if (i >= 0) options[i] = opt.copy(isAdded = nowAdded)
                                        if (nowAdded == opt.isAdded) throw IllegalStateException("Server did not change the playlist")
                                    } catch (e: CancellationException) { throw e }
                                    catch (e: Exception) { onError(e) }
                                    finally { busyKey = null }
                                }
                            },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.missav_acc_ok)) } },
        dismissButton = { TextButton(onClick = onManage) { Text(stringResource(R.string.missav_acc_playlists)) } },
    )
}

private fun openMagnet(context: Context, magnet: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(magnet)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("magnet", magnet))
        Toast.makeText(context, context.getString(R.string.missav_acc_magnet_copied), Toast.LENGTH_SHORT).show()
    }
}
