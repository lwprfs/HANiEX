package com.yenaly.han1meviewer.SitesHub.Coser.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.SitesHub.Coser.common.CoserConstants
import com.yenaly.han1meviewer.SitesHub.Coser.common.CoserImageLoader
import com.yenaly.han1meviewer.SitesHub.Coser.data.model.CoserCard
import com.yenaly.han1meviewer.SitesHub.Coser.data.model.CoserTerm
import com.yenaly.han1meviewer.SitesHub.Coser.data.remote.CoserRepo
import com.yenaly.han1meviewer.SitesHub.Coser.ui.list.CoserCardGrid
import com.yenaly.han1meviewer.SitesHub.Coser.ui.list.CoserFilterHeader
import com.yenaly.han1meviewer.SitesHub.Coser.ui.list.CoserFiltersSaver
import com.yenaly.han1meviewer.SitesHub.Coser.ui.list.CoserListViewModel
import com.yenaly.han1meviewer.SitesHub.Coser.ui.list.CoserSource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteSearchAppBar
import com.yenaly.han1meviewer.ui.component.SiteSearchPrompt
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import com.yenaly.han1meviewer.ui.screen.RetryableImage
import kotlinx.coroutines.delay

@Composable
fun CoserSearchScreen(
    initialQuery: String?,
    onBack: () -> Unit,
    onNavigateToPost: (String) -> Unit,
    onNavigateToTerm: (CoserTerm) -> Unit,
    viewModel: CoserListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf(initialQuery.orEmpty()) }
    var submitted by rememberSaveable { mutableStateOf(initialQuery.orEmpty()) }
    var filters by rememberSaveable(stateSaver = CoserFiltersSaver) { mutableStateOf(CoserConstants.Filters()) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }

    LaunchedEffect(submitted, filters) {
        if (submitted.isBlank()) return@LaunchedEffect
        val slug = CoserConstants.parsePostReference(submitted)
        if (slug != null) {
            onNavigateToPost(slug)
            submitted = ""
        } else {
            viewModel.setSource(CoserSource.Search(submitted, filters))
        }
    }
    LaunchedEffect(Unit) { if (initialQuery.isNullOrBlank()) focus.requestFocus() }

    fun submit() {
        submitted = query.trim()
        keyboard?.hide()
    }

    Scaffold(
        topBar = {
            SiteSearchAppBar(
                query = query,
                onQueryChange = { query = it },
                onSubmit = { submit() },
                onBack = onBack,
                hint = stringResource(R.string.coser_search_hint),
                focusRequester = focus,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when {
                submitted.isBlank() -> SiteSearchPrompt(stringResource(R.string.coser_search_prompt))
                else -> CoserCardGrid(
                    header = { CoserFilterHeader(filters = filters, onChange = { filters = it }) },
                    state = state,
                    onRetry = { viewModel.refresh() },
                    onLoadMore = { viewModel.loadMore() },
                    onOpenPost = onNavigateToPost,
                    onOpenTerm = onNavigateToTerm,
                    emptyHint = stringResource(R.string.coser_no_results),
                )
            }
        }
    }
}
