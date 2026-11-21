package com.yenaly.han1meviewer.Wumaobi.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.Wumaobi.data.model.WumaobiListPage
import com.yenaly.han1meviewer.Wumaobi.data.remote.WumaobiRepo
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface WumaobiSource {

    data class Path(val path: String, val sort: String? = null) : WumaobiSource
    data class Search(val query: String) : WumaobiSource
    data class Favorites(val name: String) : WumaobiSource
}

data class WumaobiListUiState(
    val items: List<HanimeInfo> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
)

class WumaobiListViewModel : ViewModel() {

    private val _state = MutableStateFlow(WumaobiListUiState())
    val state: StateFlow<WumaobiListUiState> = _state.asStateFlow()

    private var source: WumaobiSource? = null
    private var page = 1
    private var job: Job? = null

    fun setSource(newSource: WumaobiSource, force: Boolean = false) {
        if (!force && newSource == source && _state.value.items.isNotEmpty()) return
        source = newSource
        page = 1
        job?.cancel()
        _state.value = WumaobiListUiState(isLoading = true)
        job = viewModelScope.launch {
            fetch(newSource, 1)
                .onSuccess { applyFirst(it) }
                .onFailure { e ->
                    _state.value = WumaobiListUiState(error = e.message ?: e.javaClass.simpleName)
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

    private fun applyFirst(result: WumaobiListPage) {
        _state.value = WumaobiListUiState(
            items = result.items,
            hasNext = result.hasNext && result.items.isNotEmpty(),
        )
    }

    private suspend fun fetch(src: WumaobiSource, page: Int): Result<WumaobiListPage> = when (src) {
        is WumaobiSource.Path -> WumaobiRepo.listing(src.path, page, src.sort)
        is WumaobiSource.Search -> WumaobiRepo.search(src.query, page)
        is WumaobiSource.Favorites -> WumaobiRepo.favVideos(src.name, page)
    }
}
