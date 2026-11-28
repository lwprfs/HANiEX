package com.yenaly.han1meviewer.SitesHub.Strawberry.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.local.StrawberryHistoryRepo
import com.yenaly.han1meviewer.Preferences
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Composable
fun rememberStrawberryWatched(): Set<String> {
    val context = LocalContext.current
    val flow = remember {
        StrawberryHistoryRepo.init(context)
        StrawberryHistoryRepo.loadAll()
            .map { list -> list.map { it.videoId }.toSet() }
            .catch { emit(emptySet()) }
    }
    val watched by flow.collectAsStateWithLifecycle(initialValue = emptySet())
    return if (Preferences.showPlayedIndicator) watched else emptySet()
}
