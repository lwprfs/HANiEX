package com.yenaly.han1meviewer.SitesHub.Coomer.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.SitesHub.Coomer.common.CoomerConstants
import com.yenaly.han1meviewer.SitesHub.Coomer.data.model.CoomerTerm
import com.yenaly.han1meviewer.SitesHub.Coomer.ui.list.CoomerCardGrid
import com.yenaly.han1meviewer.SitesHub.Coomer.ui.list.CoomerListViewModel
import com.yenaly.han1meviewer.SitesHub.Coomer.ui.list.CoomerSource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SiteSearchAppBar
import com.yenaly.han1meviewer.ui.component.SiteSearchPrompt

@Composable
fun CoomerSearchScreen(
    initialQuery: String?,
    onBack: () -> Unit,
    onNavigateToPost: (String) -> Unit,
    onNavigateToTerm: (CoomerTerm) -> Unit,
    viewModel: CoomerListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf(initialQuery.orEmpty()) }
    var submitted by rememberSaveable { mutableStateOf(initialQuery.orEmpty()) }
    var searchPosts by rememberSaveable { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }

    LaunchedEffect(submitted, searchPosts) {
        if (submitted.isBlank()) return@LaunchedEffect
        val slug = CoomerConstants.parsePostReference(submitted)
        if (slug != null) {
            onNavigateToPost(slug)
            submitted = ""
        } else {
            viewModel.setSource(CoomerSource.Search(submitted, searchPosts))
        }
    }
    LaunchedEffect(Unit) { if (initialQuery.isNullOrBlank()) focus.requestFocus() }

    fun submit() {
        submitted = query.trim()
        keyboard?.hide()
    }

    val creatorsLabel = stringResource(R.string.coomer_search_creators)
    val postsLabel = stringResource(R.string.coomer_search_posts)

    Scaffold(
        topBar = {
            SiteSearchAppBar(
                query = query,
                onQueryChange = { query = it },
                onSubmit = { submit() },
                onBack = onBack,
                hint = stringResource(R.string.coomer_search_hint),
                focusRequester = focus,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SiteChipRow(
                options = listOf(false, true),
                selected = searchPosts,
                label = { if (it) postsLabel else creatorsLabel },
                onSelect = { searchPosts = it },
                key = { if (it) "posts" else "creators" },
                horizontalPadding = 12.dp,
            )
            when {
                submitted.isBlank() -> SiteSearchPrompt(stringResource(R.string.coomer_search_prompt))
                else -> CoomerCardGrid(
                    state = state,
                    onRetry = { viewModel.refresh() },
                    onLoadMore = { viewModel.loadMore() },
                    onOpenPost = onNavigateToPost,
                    onOpenTerm = onNavigateToTerm,
                    emptyHint = stringResource(R.string.coomer_no_results),
                )
            }
        }
    }
}
