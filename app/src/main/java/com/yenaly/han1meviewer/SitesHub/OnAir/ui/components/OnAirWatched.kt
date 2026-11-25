package com.yenaly.han1meviewer.SitesHub.OnAir.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.SitesHub.OnAir.data.local.OnAirHistoryRepo
import com.yenaly.han1meviewer.Preferences
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** IDs of OnAir videos present in watch history (empty when the played indicator is disabled). */
@Composable
fun rememberOnAirWatched(): Set<String> {
    val context = LocalContext.current
    val flow = remember {
        OnAirHistoryRepo.init(context)
        OnAirHistoryRepo.loadAll()
            .map { list -> list.map { it.videoId }.toSet() }
            .catch { emit(emptySet()) }
    }
    val watched by flow.collectAsStateWithLifecycle(initialValue = emptySet())
    return if (Preferences.showPlayedIndicator) watched else emptySet()
}
