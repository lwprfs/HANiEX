package com.yenaly.han1meviewer.SitesHub.XXXTube.ui.search

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SitesHub.XXXTube.common.XXXTubeConstants
import com.yenaly.han1meviewer.SitesHub.XXXTube.data.model.XXXTubeEntity
import com.yenaly.han1meviewer.SitesHub.XXXTube.ui.entities.XXXTubeEntityGrid
import com.yenaly.han1meviewer.SitesHub.XXXTube.ui.entities.XXXTubeEntitySource
import com.yenaly.han1meviewer.SitesHub.XXXTube.ui.entities.XXXTubeEntityViewModel
import com.yenaly.han1meviewer.SitesHub.XXXTube.ui.home.XXXTubeListSource
import com.yenaly.han1meviewer.SitesHub.XXXTube.ui.home.XXXTubeListViewModel
import com.yenaly.han1meviewer.SitesHub.XXXTube.ui.home.XXXTubeVideoGrid
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SiteSearchAppBar
import com.yenaly.han1meviewer.ui.component.SiteSearchPrompt

@Composable
fun XXXTubeSearchScreen(
    initialQuery: String?,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onOpenEntity: (XXXTubeEntity) -> Unit,
    videoViewModel: XXXTubeListViewModel = viewModel(),
    entityViewModel: XXXTubeEntityViewModel = viewModel(),
) {
    val videoState by videoViewModel.state.collectAsStateWithLifecycle()
    val entityState by entityViewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf(initialQuery.orEmpty()) }
    var submitted by rememberSaveable { mutableStateOf(initialQuery.orEmpty()) }
    var typeName by rememberSaveable { mutableStateOf(XXXTubeConstants.SearchType.VIDEOS.name) }
    val type = XXXTubeConstants.SearchType.valueOf(typeName)
    val focus = remember { FocusRequester() }

    LaunchedEffect(submitted, type) {
        if (submitted.isBlank()) return@LaunchedEffect

        val code = XXXTubeConstants.parseVideoReference(submitted)
        if (code != null) {
            onNavigateToVideo(code)
            submitted = ""
            return@LaunchedEffect
        }
        when (type) {
            XXXTubeConstants.SearchType.VIDEOS ->
                videoViewModel.setSource(XXXTubeListSource.Search(submitted))

            XXXTubeConstants.SearchType.MODELS ->
                entityViewModel.setSource(
                    XXXTubeEntitySource(XXXTubeConstants.EntityKind.MODELS, submitted)
                )

            XXXTubeConstants.SearchType.CATEGORIES ->
                entityViewModel.setSource(
                    XXXTubeEntitySource(XXXTubeConstants.EntityKind.CATEGORIES, submitted)
                )
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
                hint = stringResource(R.string.xxxtube_search_hint),
                focusRequester = focus,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val labels = mapOf(
                XXXTubeConstants.SearchType.VIDEOS to stringResource(R.string.xxxtube_search_videos),
                XXXTubeConstants.SearchType.MODELS to stringResource(R.string.xxxtube_models),
                XXXTubeConstants.SearchType.CATEGORIES to stringResource(R.string.xxxtube_categories),
            )
            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp)) {
                SiteChipRow(
                    options = XXXTubeConstants.SearchType.entries.toList(),
                    selected = type,
                    label = { labels[it].orEmpty() },
                    onSelect = { typeName = it.name },
                    key = { it.name },
                    horizontalPadding = 0.dp,
                )
            }
            when {
                submitted.isBlank() -> SiteSearchPrompt(stringResource(R.string.xxxtube_search_prompt))
                type == XXXTubeConstants.SearchType.VIDEOS -> XXXTubeVideoGrid(
                    state = videoState,
                    onRetry = { videoViewModel.refresh() },
                    onLoadMore = { videoViewModel.loadMore() },
                    onOpenVideo = onNavigateToVideo,
                    emptyHint = stringResource(R.string.xxxtube_no_results),
                )

                else -> XXXTubeEntityGrid(
                    state = entityState,
                    onRetry = { entityViewModel.refresh() },
                    onLoadMore = { entityViewModel.loadMore() },
                    onOpen = onOpenEntity,
                    emptyHint = stringResource(R.string.xxxtube_no_results),
                )
            }
        }
    }
}
