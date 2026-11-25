package com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.favorites

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteBarIconButton
import com.yenaly.han1meviewer.ui.component.SiteEmptyContent
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WumaobiFavoritesScreen(
    onBack: () -> Unit,
    onOpenFolder: (String) -> Unit,
    viewModel: WumaobiFavoritesViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(state.message) {
        val text = state.message ?: return@LaunchedEffect
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        viewModel.consumeMessage()
    }

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.wumaobi_favorites)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
                actions = {
                    SiteBarIconButton(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.wumaobi_fav_create),
                        onClick = { showCreate = true },
                    )
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            SitePullToRefresh(
                loading = state.isLoading,
                onRefresh = { viewModel.load() },
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.folders.isEmpty() && state.isLoading -> SiteLoadingContent()
                    state.folders.isEmpty() && state.error != null -> SiteErrorContent(
                        message = state.error,
                        onRetry = { viewModel.load() },
                    )

                    state.folders.isEmpty() -> SiteEmptyContent(
                        hint = stringResource(R.string.wumaobi_nothing_here),
                    )

                    else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.folders, key = { it.name }) { folder ->
                            SettingNavigationItem(
                                title = folder.name,
                                valueText = folder.count.ifBlank { null },
                                icon = Icons.Filled.Folder,
                                onClick = { onOpenFolder(folder.name) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(stringResource(R.string.wumaobi_fav_create)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.wumaobi_fav_collection_name)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.create(newName)
                        newName = ""
                        showCreate = false
                    },
                    enabled = newName.isNotBlank(),
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) {
                    Text(stringResource(R.string.cancel), overflow = TextOverflow.Ellipsis)
                }
            },
        )
    }
}
