package com.yenaly.han1meviewer.SitesHub.XXXTube.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.SitesHub.XXXTube.data.repository.XXXTubeHistoryRepo
import com.yenaly.han1meviewer.Preferences
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Composable
fun rememberXXXTubeWatched(): Set<String> {
    val context = LocalContext.current
    val flow = remember {
        XXXTubeHistoryRepo.init(context)
        XXXTubeHistoryRepo.loadAll()
            .map { list -> list.map { it.videoCode }.toSet() }
            .catch { emit(emptySet()) }
    }
    val watched by flow.collectAsStateWithLifecycle(initialValue = emptySet())
    return if (Preferences.showPlayedIndicator) watched else emptySet()
}
