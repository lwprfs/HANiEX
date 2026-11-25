package com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.search

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
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryConstants
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryTerm
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.list.LeakGalleryCardGrid
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.list.LeakGalleryListViewModel
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.list.LeakGallerySource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SiteSearchAppBar
import com.yenaly.han1meviewer.ui.component.SiteSearchPrompt

@Composable
fun LeakGallerySearchScreen(
    initialQuery: String?,
    onBack: () -> Unit,
    onNavigateToPost: (String) -> Unit,
    onNavigateToTerm: (LeakGalleryTerm) -> Unit,
    viewModel: LeakGalleryListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf(initialQuery.orEmpty()) }
    var submitted by rememberSaveable { mutableStateOf(initialQuery.orEmpty()) }
    var searchCreators by rememberSaveable { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }

    LaunchedEffect(submitted, searchCreators) {
        if (submitted.isBlank()) return@LaunchedEffect
        val slug = LeakGalleryConstants.parsePostReference(submitted)
        if (slug != null) {
            onNavigateToPost(slug)
            submitted = ""
        } else {
            viewModel.setSource(LeakGallerySource.Search(submitted, searchCreators))
        }
    }
    LaunchedEffect(Unit) { if (initialQuery.isNullOrBlank()) focus.requestFocus() }

    fun submit() {
        submitted = query.trim()
        keyboard?.hide()
    }

    val creatorsLabel = stringResource(R.string.leakgallery_search_creators)
    val postsLabel = stringResource(R.string.leakgallery_search_posts)

    Scaffold(
        topBar = {
            SiteSearchAppBar(
                query = query,
                onQueryChange = { query = it },
                onSubmit = { submit() },
                onBack = onBack,
                hint = stringResource(R.string.leakgallery_search_hint),
                focusRequester = focus,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SiteChipRow(
                options = listOf(false, true),
                selected = searchCreators,
                label = { if (it) creatorsLabel else postsLabel },
                onSelect = { searchCreators = it },
                key = { if (it) "creators" else "posts" },
                horizontalPadding = 12.dp,
            )
            when {
                submitted.isBlank() -> SiteSearchPrompt(stringResource(R.string.leakgallery_search_prompt))
                else -> LeakGalleryCardGrid(
                    state = state,
                    onRetry = { viewModel.refresh() },
                    onLoadMore = { viewModel.loadMore() },
                    onOpenPost = onNavigateToPost,
                    onOpenTerm = onNavigateToTerm,
                    emptyHint = stringResource(R.string.leakgallery_no_results),
                )
            }
        }
    }
}
