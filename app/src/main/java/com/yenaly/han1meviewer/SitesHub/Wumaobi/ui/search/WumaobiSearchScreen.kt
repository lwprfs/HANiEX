package com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.search

import com.yenaly.han1meviewer.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.SitesHub.Wumaobi.common.WumaobiConstants
import com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.home.WumaobiListViewModel
import com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.home.WumaobiSource
import com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.home.WumaobiVideoGrid
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteSearchAppBar
import com.yenaly.han1meviewer.ui.component.SiteSearchPrompt
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar

@Composable
fun WumaobiSearchScreen(
    initialQuery: String?,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    viewModel: WumaobiListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf(initialQuery.orEmpty()) }
    var submitted by rememberSaveable { mutableStateOf(initialQuery.orEmpty()) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }

    LaunchedEffect(submitted) {
        if (submitted.isBlank()) return@LaunchedEffect

        val code = WumaobiConstants.parseVideoReference(submitted)
        if (code != null) {
            onNavigateToVideo(code)
            submitted = ""
        } else {
            viewModel.setSource(WumaobiSource.Search(submitted))
        }
    }
    LaunchedEffect(Unit) {
        if (initialQuery.isNullOrBlank()) focus.requestFocus()
    }

    Scaffold(
        topBar = {
            SiteSearchAppBar(
                query = query,
                onQueryChange = { query = it },
                onSubmit = { submitted = query.trim() },
                onBack = onBack,
                hint = stringResource(R.string.wumaobi_search_hint),
                focusRequester = focus,
            )
        },
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            if (submitted.isBlank()) {
                SiteSearchPrompt(stringResource(R.string.wumaobi_search_prompt))
            } else {
                WumaobiVideoGrid(
                    state = state,
                    onRetry = { viewModel.refresh() },
                    onLoadMore = { viewModel.loadMore() },
                    onOpenVideo = onNavigateToVideo,
                    emptyHint = stringResource(R.string.wumaobi_no_results),
                )
            }
        }
    }
}
