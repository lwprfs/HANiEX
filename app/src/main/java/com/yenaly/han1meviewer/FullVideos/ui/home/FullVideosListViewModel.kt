package com.yenaly.han1meviewer.FullVideos.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.FullVideos.data.model.FullVideosListPage
import com.yenaly.han1meviewer.FullVideos.data.remote.FullVideosRepo
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface FullVideosSource {

    data class Path(val path: String) : FullVideosSource
    data class Search(val query: String) : FullVideosSource
}

data class FullVideosListUiState(
    val items: List<HanimeInfo> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
)

class FullVideosListViewModel : ViewModel() {

    private val _state = MutableStateFlow(FullVideosListUiState())
    val state: StateFlow<FullVideosListUiState> = _state.asStateFlow()

    private var source: FullVideosSource? = null
    private var page = 1
    private var job: Job? = null

    fun setSource(newSource: FullVideosSource, force: Boolean = false) {
        if (!force && newSource == source && _state.value.items.isNotEmpty()) return
        source = newSource
        page = 1
        job?.cancel()
        _state.value = FullVideosListUiState(isLoading = true)
        job = viewModelScope.launch {
            fetch(newSource, 1)
                .onSuccess { applyFirst(it) }
                .onFailure { e ->
                    _state.value = FullVideosListUiState(error = e.message ?: e.javaClass.simpleName)
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

    private fun applyFirst(result: FullVideosListPage) {
        _state.value = FullVideosListUiState(
            items = result.items,
            hasNext = result.hasNext && result.items.isNotEmpty(),
        )
    }

    private suspend fun fetch(src: FullVideosSource, page: Int): Result<FullVideosListPage> = when (src) {
        is FullVideosSource.Path -> FullVideosRepo.listing(src.path, page)
        is FullVideosSource.Search -> FullVideosRepo.search(src.query, page)
    }
}
