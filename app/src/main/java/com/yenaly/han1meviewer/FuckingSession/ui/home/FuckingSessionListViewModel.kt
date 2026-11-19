package com.yenaly.han1meviewer.FuckingSession.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.FuckingSession.data.model.FuckingSessionListPage
import com.yenaly.han1meviewer.FuckingSession.data.remote.FuckingSessionRepo
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface FuckingSessionListSource {

    data class Path(val path: String) : FuckingSessionListSource
    data class Search(val query: String) : FuckingSessionListSource
}

data class FuckingSessionListUiState(
    val items: List<HanimeInfo> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
)

class FuckingSessionListViewModel : ViewModel() {

    private val _state = MutableStateFlow(FuckingSessionListUiState())
    val state: StateFlow<FuckingSessionListUiState> = _state.asStateFlow()

    private var source: FuckingSessionListSource? = null
    private var page = 1
    private var job: Job? = null

    fun setSource(newSource: FuckingSessionListSource, force: Boolean = false) {
        if (!force && newSource == source && _state.value.items.isNotEmpty()) return
        source = newSource
        page = 1
        job?.cancel()
        _state.value = FuckingSessionListUiState(isLoading = true)
        job = viewModelScope.launch {
            fetch(newSource, 1)
                .onSuccess { applyFirst(it) }
                .onFailure { e ->
                    _state.value = FuckingSessionListUiState(error = e.message ?: e.javaClass.simpleName)
                }
        }
    }

    fun refresh() {
        source?.let { setSource(it, force = true) }
    }

    fun loadMore() {
        val current = _state.value
        val src = source ?: return
        if (current.isLoading || current.isLoadingMore || !current.hasNext) return
        _state.update { it.copy(isLoadingMore = true) }
        job = viewModelScope.launch {
            val next = page + 1
            fetch(src, next)
                .onSuccess { result ->
                    page = next
                    _state.update { old ->
                        val merged = (old.items + result.items).distinctBy { it.videoCode }
                        old.copy(
                            items = merged,
                            isLoadingMore = false,
                            hasNext = result.hasNext && result.items.isNotEmpty(),
                        )
                    }
                }
                .onFailure { _state.update { old -> old.copy(isLoadingMore = false) } }
        }
    }

    private fun applyFirst(result: FuckingSessionListPage) {
        _state.value = FuckingSessionListUiState(
            items = result.items,
            hasNext = result.hasNext && result.items.isNotEmpty(),
        )
    }

    private suspend fun fetch(src: FuckingSessionListSource, page: Int): Result<FuckingSessionListPage> = when (src) {
        is FuckingSessionListSource.Path -> FuckingSessionRepo.listing(src.path, page)
        is FuckingSessionListSource.Search -> FuckingSessionRepo.search(src.query, page)
    }
}
