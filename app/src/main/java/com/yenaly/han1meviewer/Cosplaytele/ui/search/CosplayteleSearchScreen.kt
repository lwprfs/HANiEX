package com.yenaly.han1meviewer.Cosplaytele.ui.search

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
import com.yenaly.han1meviewer.Cosplaytele.common.CosplayteleConstants
import com.yenaly.han1meviewer.Cosplaytele.common.CosplayteleImageLoader
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleCard
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleTerm
import com.yenaly.han1meviewer.Cosplaytele.data.remote.CosplayteleRepo
import com.yenaly.han1meviewer.Cosplaytele.ui.list.CosplayteleCardGrid
import com.yenaly.han1meviewer.Cosplaytele.ui.list.CosplayteleListViewModel
import com.yenaly.han1meviewer.Cosplaytele.ui.list.CosplayteleSource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import com.yenaly.han1meviewer.ui.screen.RetryableImage
import kotlinx.coroutines.delay

/**
 * Search: while typing, the site's live search (Ajax Search Lite) provides suggestions; submitting
 * runs the full `/?s=` search with paging. Pasting a post link opens the post directly.
 */
@Composable
fun CosplayteleSearchScreen(
    initialQuery: String?,
    onBack: () -> Unit,
    onNavigateToPost: (String) -> Unit,
    onNavigateToTerm: (CosplayteleTerm) -> Unit,
    viewModel: CosplayteleListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf(initialQuery.orEmpty()) }
    var submitted by rememberSaveable { mutableStateOf(initialQuery.orEmpty()) }
    var suggestions by remember { mutableStateOf<List<CosplayteleCard>>(emptyList()) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2 || q == submitted) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(300)
        suggestions = CosplayteleRepo.liveSearch(q).getOrDefault(emptyList())
    }

    LaunchedEffect(submitted) {
        if (submitted.isBlank()) return@LaunchedEffect
        val slug = CosplayteleConstants.parsePostReference(submitted)
        if (slug != null) {
            onNavigateToPost(slug)
            submitted = ""
        } else {
            suggestions = emptyList()
            viewModel.setSource(CosplayteleSource.Search(submitted))
        }
    }
    LaunchedEffect(Unit) { if (initialQuery.isNullOrBlank()) focus.requestFocus() }

    fun submit() {
        submitted = query.trim()
        keyboard?.hide()
    }

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.cosplaytele_search_hint)) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { submit() }),
                    )
                },
                navigationIcon = { SiteBackButton(onClick = onBack) },
                actions = {
                    IconButton(onClick = { submit() }) {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when {
                suggestions.isNotEmpty() -> SuggestionList(suggestions, onNavigateToPost)
                submitted.isBlank() -> Text(
                    text = stringResource(R.string.cosplaytele_search_prompt),
                    modifier = Modifier.padding(16.dp),
                )
                else -> CosplayteleCardGrid(
                    state = state,
                    onRetry = { viewModel.refresh() },
                    onLoadMore = { viewModel.loadMore() },
                    onOpenPost = onNavigateToPost,
                    onOpenTerm = onNavigateToTerm,
                    emptyHint = stringResource(R.string.cosplaytele_no_results),
                )
            }
        }
    }
}

@Composable
private fun SuggestionList(items: List<CosplayteleCard>, onOpen: (String) -> Unit) {
    val imageLoader = CosplayteleImageLoader.get(LocalContext.current)
    LazyColumn(Modifier.fillMaxSize()) {
        items(items, key = { it.slug }) { card ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpen(card.slug) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RetryableImage(
                    model = card.coverUrl,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                    placeholder = painterResource(R.drawable.h_chan_loading),
                    error = painterResource(R.drawable.h_chan_load_failed),
                    contentScale = ContentScale.Crop,
                    imageLoader = imageLoader,
                )
                Text(
                    text = card.title,
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
