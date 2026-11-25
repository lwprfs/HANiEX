package com.yenaly.han1meviewer.SitesHub.Buondua.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.SitesHub.Buondua.common.BuonduaConstants
import com.yenaly.han1meviewer.SitesHub.Buondua.ui.home.BuonduaAlbumGrid
import com.yenaly.han1meviewer.SitesHub.Buondua.ui.home.BuonduaListViewModel
import com.yenaly.han1meviewer.SitesHub.Buondua.ui.home.BuonduaSource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteSearchAppBar
import com.yenaly.han1meviewer.ui.component.SiteSearchPrompt
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar

@Composable
fun BuonduaSearchScreen(
    initialQuery: String?,
    onBack: () -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    viewModel: BuonduaListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf(initialQuery.orEmpty()) }
    var submitted by rememberSaveable { mutableStateOf(initialQuery.orEmpty()) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }

    LaunchedEffect(submitted) {
        if (submitted.isBlank()) return@LaunchedEffect
        val code = BuonduaConstants.parseAlbumReference(submitted)
        if (code != null) {
            onNavigateToAlbum(code)
            submitted = ""
        } else {
            viewModel.setSource(BuonduaSource.Search(submitted))
        }
    }
    LaunchedEffect(Unit) { if (initialQuery.isNullOrBlank()) focus.requestFocus() }

    Scaffold(
        topBar = {
            SiteSearchAppBar(
                query = query,
                onQueryChange = { query = it },
                onSubmit = { submitted = query.trim() },
                onBack = onBack,
                hint = stringResource(R.string.buondua_search_hint),
                focusRequester = focus,
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (submitted.isBlank()) {
                SiteSearchPrompt(stringResource(R.string.buondua_search_prompt))
            } else {
                state.heading?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                BuonduaAlbumGrid(
                    state = state,
                    onRetry = { viewModel.refresh() },
                    onLoadMore = { viewModel.loadMore() },
                    onOpenAlbum = onNavigateToAlbum,
                    emptyHint = stringResource(R.string.buondua_no_results),
                )
            }
        }
    }
}
