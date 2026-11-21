package com.yenaly.han1meviewer.RedGifs.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.RedGifs.data.model.RedGifsNiche
import com.yenaly.han1meviewer.RedGifs.data.model.RedGifsUser
import com.yenaly.han1meviewer.RedGifs.data.remote.RedGifsFeedSource
import com.yenaly.han1meviewer.RedGifs.data.remote.RedGifsRepo
import com.yenaly.han1meviewer.RedGifs.ui.browse.RedGifsNicheRow
import com.yenaly.han1meviewer.RedGifs.ui.browse.RedGifsPagedList
import com.yenaly.han1meviewer.RedGifs.ui.browse.RedGifsUserRow
import com.yenaly.han1meviewer.RedGifs.ui.feed.RedGifsFeedContent
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar

private enum class SearchTab(val titleRes: Int) {
    GIFS(R.string.redgifs_search_gifs),
    CREATORS(R.string.redgifs_creators),
    NICHES(R.string.redgifs_niches),
}

@Composable
fun RedGifsSearchScreen(
    initialQuery: String?,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onOpenCreator: (RedGifsUser) -> Unit,
    onOpenNiche: (RedGifsNiche) -> Unit,
) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf(initialQuery.orEmpty()) }
    var submitted by rememberSaveable { mutableStateOf(initialQuery.orEmpty().trim()) }
    var tabName by rememberSaveable { mutableStateOf(SearchTab.GIFS.name) }
    val tab = SearchTab.valueOf(tabName)
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        if (submitted.isEmpty()) runCatching { focus.requestFocus() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = {
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.redgifs_search_hint)) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            submitted = input.trim()
                            keyboard?.hide()
                        }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (submitted.isEmpty()) {
                Text(
                    stringResource(R.string.redgifs_search_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                SiteChipRow(
                    options = SearchTab.entries.toList(),
                    selected = tab,
                    label = { context.getString(it.titleRes) },
                    onSelect = { tabName = it.name },
                    key = { it.name },
                )
                when (tab) {
                    SearchTab.GIFS -> key(submitted) {
                        RedGifsFeedContent(source = RedGifsFeedSource.Search(submitted), onLogin = onLogin)
                    }
                    SearchTab.CREATORS -> RedGifsPagedList(
                        reloadKey = submitted,
                        load = { page ->
                            val base = RedGifsRepo.creators("trending", page, submitted)
                            if (page != 1) {
                                base
                            } else {
                                // Exact username lookup (`/v1/users/{name}`) so a known creator is always found.
                                val exact = RedGifsRepo.user(submitted.removePrefix("@")).getOrNull()
                                base.map { result ->
                                    if (exact != null && result.items.none { it.username.equals(exact.username, true) }) {
                                        result.copy(items = listOf(exact) + result.items)
                                    } else {
                                        result
                                    }
                                }
                            }
                        },
                        itemKey = { it.username },
                        emptyHint = stringResource(R.string.redgifs_empty),
                    ) { user -> RedGifsUserRow(user) { onOpenCreator(user) } }
                    SearchTab.NICHES -> RedGifsPagedList(
                        reloadKey = submitted,
                        load = { page -> RedGifsRepo.niches("subscribers", page, null, submitted) },
                        itemKey = { it.id },
                        emptyHint = stringResource(R.string.redgifs_empty),
                    ) { niche -> RedGifsNicheRow(niche) { onOpenNiche(niche) } }
                }
            }
        }
    }
}
