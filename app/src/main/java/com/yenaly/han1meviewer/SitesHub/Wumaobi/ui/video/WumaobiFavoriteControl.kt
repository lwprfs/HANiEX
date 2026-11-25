package com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.video

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SitesHub.Wumaobi.data.model.WumaobiDetail

@Composable
internal fun WumaobiFavoriteControl(
    info: WumaobiDetail,
    viewModel: WumaobiVideoViewModel,
    onNavigateToAccount: () -> Unit,
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    val message by viewModel.favMessage.collectAsStateWithLifecycle()

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        viewModel.consumeFavMessage()
    }

    val isFavorite = info.favCollections.any { it.checked }
    AssistChip(
        onClick = { if (info.loggedIn) showDialog = true else onNavigateToAccount() },
        label = {
            Text(
                stringResource(
                    if (info.loggedIn) R.string.wumaobi_fav_edit else R.string.wumaobi_fav_sign_in
                )
            )
        },
        leadingIcon = {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
            )
        },
    )

    if (showDialog) {
        val selected = remember(info.favCollections) {
            mutableStateListOf<String>().apply {
                addAll(info.favCollections.filter { it.checked }.map { it.id })
            }
        }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.wumaobi_fav_edit)) },
            text = {
                if (info.favCollections.isEmpty()) {
                    Text(stringResource(R.string.wumaobi_fav_no_collections))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        info.favCollections.forEach { collection ->
                            val checked = collection.id in selected
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (checked) selected.remove(collection.id)
                                        else selected.add(collection.id)
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { on ->
                                        if (on) selected.add(collection.id)
                                        else selected.remove(collection.id)
                                    },
                                )
                                Text(collection.name)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        viewModel.saveFavorites(info.code, selected.toList())
                    },
                    enabled = info.favCollections.isNotEmpty(),
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
