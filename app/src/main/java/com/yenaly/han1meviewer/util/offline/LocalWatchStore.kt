package com.yenaly.han1meviewer.util.offline

import androidx.core.content.edit
import com.yenaly.han1meviewer.HJson
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.logic.SiteCheckIn
import com.yenaly.han1meviewer.logic.dao.DownloadDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable

/**
 * Latest watch state of a video that was played from the downloads.
 *
 * One entry per video (keyed by the download "dish code", e.g. `onair:123` or a plain Hanime code),
 * so watching the same video again only refreshes this entry - it is never duplicated.
 * Entries stay after the download is deleted, so the history and its time tag survive.
 */
@Serializable
data class LocalWatchEntry(
    val code: String,
    val title: String = "",
    val coverUrl: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val watchedAt: Long = 0L,
    val completed: Boolean = false,
) {
    val site: SiteType get() = SiteCheckIn.siteOfDishCode(code) ?: SiteType.HANIME

    val rawCode: String get() = SiteCheckIn.rawCodeOfDishCode(code)
}

object LocalWatchStore {

    private const val PREF_KEY = "local_watch_store_v1"
    private const val MAX_ENTRIES = 1500

    private val state: MutableStateFlow<Map<String, LocalWatchEntry>> by lazy {
        MutableStateFlow(load())
    }

    val entries: StateFlow<Map<String, LocalWatchEntry>> get() = state.asStateFlow()

    fun get(code: String): LocalWatchEntry? = state.value[code]

    private fun load(): Map<String, LocalWatchEntry> {
        val raw = runCatching { Preferences.preferenceSp.getString(PREF_KEY, null) }.getOrNull()
            ?: return emptyMap()
        return runCatching {
            HJson.decodeFromString<List<LocalWatchEntry>>(raw).associateBy { it.code }
        }.getOrDefault(emptyMap())
    }

    private fun persist(map: Map<String, LocalWatchEntry>) {
        val list = map.values.sortedByDescending { it.watchedAt }.take(MAX_ENTRIES)
        runCatching {
            Preferences.preferenceSp.edit { putString(PREF_KEY, HJson.encodeToString(list)) }
        }
    }

    @Synchronized
    fun record(entry: LocalWatchEntry) {
        if (entry.code.isBlank()) return
        val old = state.value[entry.code]
        val merged = entry.copy(
            title = entry.title.ifBlank { old?.title.orEmpty() },
            coverUrl = entry.coverUrl.ifBlank { old?.coverUrl.orEmpty() },
            durationMs = if (entry.durationMs > 0) entry.durationMs else old?.durationMs ?: 0L,
            watchedAt = if (entry.watchedAt > 0) entry.watchedAt else System.currentTimeMillis(),
        )
        val next = state.value + (merged.code to merged)
        state.value = next
        persist(next)
    }

    @Synchronized
    fun forget(code: String) {
        if (code !in state.value) return
        val next = state.value - code
        state.value = next
        persist(next)
    }

    @Synchronized
    fun clear(site: SiteType?) {
        val next = if (site == null) emptyMap() else state.value.filterValues { it.site != site }
        if (next.size == state.value.size) return
        state.value = next
        persist(next)
    }
}

/** Codes of videos that are currently downloaded (finished). Shared by all cards. */
object LocalDownloadedCodes {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val codes: StateFlow<Set<String>> by lazy {
        val source: Flow<Set<String>> = runCatching {
            DownloadDatabase.instance.hanimeDownloadDao.loadFinishedCodes().map { it.toSet() }
        }.getOrNull()?.catch { emit(emptySet()) } ?: kotlinx.coroutines.flow.flowOf(emptySet())
        source.stateIn(scope, SharingStarted.Eagerly, emptySet())
    }
}
