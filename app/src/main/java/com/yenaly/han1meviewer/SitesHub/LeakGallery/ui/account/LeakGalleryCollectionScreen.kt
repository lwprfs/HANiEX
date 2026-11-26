package com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.account

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGallerySession
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryTerm
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote.LeakGalleryAuth
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.list.LeakGalleryCardGrid
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.list.LeakGalleryListViewModel
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.list.LeakGallerySource
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import com.yenaly.han1meviewer.ui.component.content.LoginGate

@Composable
fun LeakGalleryCollectionScreen(
    source: LeakGallerySource,
    title: String,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onNavigateToPost: (String) -> Unit,
    onNavigateToTerm: (LeakGalleryTerm) -> Unit,
    viewModel: LeakGalleryListViewModel = viewModel(),
) {
    val epoch by LeakGallerySession.epoch.collectAsStateWithLifecycle()
    val loggedIn = remember(epoch) { LeakGalleryAuth.isLoggedIn }
    LoginGate(loggedIn = loggedIn, title = title, onBack = onBack, onSignIn = onLogin) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        LaunchedEffect(source) { viewModel.setSource(source) }
        val gridState = rememberLazyGridState()
        HanimeScaffold(title = title, onBack = onBack) { padding ->
            Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
                SitePullToRefresh(
                    loading = state.isLoading,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LeakGalleryCardGrid(
                        state = state,
                        gridState = gridState,
                        onRetry = { viewModel.refresh() },
                        onLoadMore = { viewModel.loadMore() },
                        onOpenPost = onNavigateToPost,
                        onOpenTerm = onNavigateToTerm,
                    )
                }
            }
        }
    }
}
