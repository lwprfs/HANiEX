package com.yenaly.han1meviewer.SitesHub.OnAir.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

object OnAirDownloadTags {

    enum class Tag { DOWNLOADED, DOWNLOADING }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _tags = MutableStateFlow<Map<String, Tag>>(emptyMap())
    val tags: StateFlow<Map<String, Tag>> get() = _tags

    @Volatile
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            val downloading = runCatching { DatabaseRepo.HanimeDownload.loadAllDownloadingHanime() }
                .getOrNull()?.catch { emit(mutableListOf()) } ?: emptyFlow()
            val downloaded = runCatching {
                DatabaseRepo.HanimeDownload.loadAllDownloadedHanime(HanimeDownloadEntity.SortedBy.ID, true)
            }.getOrNull()?.map { list -> list.map { it.video } }?.catch { emit(emptyList()) }
                ?: emptyFlow()

            combine(downloading, downloaded) { inProgress, finished ->
                val map = HashMap<String, Tag>()
                fun idOf(code: String): String? =
                    if (code.startsWith("onair:", ignoreCase = true)) code.substringAfter(':') else null
                inProgress.mapNotNull { idOf(it.videoCode) }.forEach { map[it] = Tag.DOWNLOADING }
                finished.mapNotNull { idOf(it.videoCode) }.forEach { map[it] = Tag.DOWNLOADED }
                map as Map<String, Tag>
            }.catch { }.collect { _tags.value = it }
        }
    }
}

@Composable
fun rememberOnAirDownloadTags(): State<Map<String, OnAirDownloadTags.Tag>> {
    OnAirDownloadTags.start()
    return OnAirDownloadTags.tags.collectAsState()
}
