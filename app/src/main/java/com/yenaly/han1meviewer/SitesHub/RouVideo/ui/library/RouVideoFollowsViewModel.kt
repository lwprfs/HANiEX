package com.yenaly.han1meviewer.SitesHub.RouVideo.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SitesHub.RouVideo.data.model.RouVideoFollowRow
import com.yenaly.han1meviewer.SitesHub.RouVideo.data.remote.RouVideoAccountRepo
import com.yenaly.han1meviewer.SitesHub.RouVideo.data.remote.RouVideoAuth
import com.yenaly.han1meviewer.SitesHub.RouVideo.data.remote.RouVideoNotSignedInException
import com.yenaly.han1meviewer.util.applicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RouVideoFollowsUiState(
    val rows: List<RouVideoFollowRow> = emptyList(),
    val onlyNew: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
    val needsSignIn: Boolean = false,
    val total: Int = 0,
    val newCount: Int = 0,

    val message: String? = null,
)

class RouVideoFollowsViewModel : ViewModel() {

    private val _state = MutableStateFlow(RouVideoFollowsUiState())
    val state: StateFlow<RouVideoFollowsUiState> = _state.asStateFlow()

    private var page = 1
    private var job: Job? = null
    private var loadedOnce = false

    fun load(onlyNew: Boolean, force: Boolean = false) {
        if (!force && loadedOnce && _state.value.onlyNew == onlyNew &&
            (_state.value.rows.isNotEmpty() || _state.value.error == null)
        ) return
        loadedOnce = true
        page = 1
        job?.cancel()
        _state.value = RouVideoFollowsUiState(onlyNew = onlyNew, isLoading = true)
        job = viewModelScope.launch {
            RouVideoAccountRepo.follows(onlyNew, 1)
                .onSuccess { result ->
                    _state.value = RouVideoFollowsUiState(
                        rows = result.rows.distinctBy { it.seriesId },
                        onlyNew = onlyNew,
                        hasNext = result.hasNext && result.rows.isNotEmpty(),
                        total = result.total,
                        newCount = result.newCount,
                    )
                }
                .onFailure { e ->
                    val signedOut = e is RouVideoNotSignedInException
                    if (signedOut) RouVideoAuth.onSessionRejected()
                    _state.value = RouVideoFollowsUiState(
                        onlyNew = onlyNew,
                        error = e.message ?: e.javaClass.simpleName,
                        needsSignIn = signedOut,
                    )
                }
        }
    }

    fun refresh() = load(_state.value.onlyNew, force = true)

    fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || !current.hasNext) return
        _state.update { it.copy(isLoadingMore = true) }
        job = viewModelScope.launch {
            val next = page + 1
            RouVideoAccountRepo.follows(current.onlyNew, next)
                .onSuccess { result ->
                    page = next
                    _state.update { old ->
                        old.copy(
                            rows = (old.rows + result.rows).distinctBy { it.seriesId },
                            isLoadingMore = false,
                            hasNext = result.hasNext && result.rows.isNotEmpty(),
                        )
                    }
                }
                .onFailure { _state.update { old -> old.copy(isLoadingMore = false) } }
        }
    }

    fun unfollow(row: RouVideoFollowRow) {
        val before = _state.value
        _state.update { it.copy(rows = it.rows.filterNot { r -> r.seriesId == row.seriesId }) }
        viewModelScope.launch {
            RouVideoAccountRepo.setFollow(row.seriesId, followed = false)
                .onSuccess {
                    _state.update {
                        it.copy(message = applicationContext.getString(R.string.rouvideo_unfollowed))
                    }
                }
                .onFailure {
                    _state.value = before.copy(
                        message = applicationContext.getString(R.string.rouvideo_action_failed)
                    )
                }
        }
    }

    fun markSeen(row: RouVideoFollowRow) {
        if (row.unseen <= 0) return
        val before = _state.value
        _state.update {
            it.copy(
                rows = it.rows.map { r -> if (r.seriesId == row.seriesId) r.copy(unseen = 0) else r },
                newCount = (it.newCount - row.unseen).coerceAtLeast(0),
            )
        }
        viewModelScope.launch {
            RouVideoAccountRepo.markSeen(row.seriesId).onFailure {
                _state.value = before.copy(
                    message = applicationContext.getString(R.string.rouvideo_action_failed)
                )
            }
        }
    }

    fun markAllSeen() {
        val before = _state.value
        _state.update {
            it.copy(rows = it.rows.map { r -> r.copy(unseen = 0) }, newCount = 0)
        }
        viewModelScope.launch {
            RouVideoAccountRepo.markSeen(null)
                .onSuccess {
                    _state.update {
                        it.copy(message = applicationContext.getString(R.string.rouvideo_all_seen))
                    }
                }
                .onFailure {
                    _state.value = before.copy(
                        message = applicationContext.getString(R.string.rouvideo_action_failed)
                    )
                }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}
