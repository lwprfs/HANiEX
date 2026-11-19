package com.yenaly.han1meviewer.FuckingSession.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.FuckingSession.data.repository.FuckingSessionHistoryRepo
import com.yenaly.han1meviewer.Preferences
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Video codes the user has already opened and played. The site itself has no "watched" flag, so
 * this comes from the module's local watch history. Honors the app-wide "played indicator" switch.
 */
@Composable
fun rememberFuckingSessionWatched(): Set<String> {
    val context = LocalContext.current
    val flow = remember {
        FuckingSessionHistoryRepo.init(context)
        FuckingSessionHistoryRepo.loadAll()
            .map { list -> list.mapTo(HashSet()) { it.videoCode } as Set<String> }
            .catch { emit(emptySet()) }
    }
    val watched by flow.collectAsStateWithLifecycle(initialValue = emptySet())
    return if (Preferences.showPlayedIndicator) watched else emptySet()
}
