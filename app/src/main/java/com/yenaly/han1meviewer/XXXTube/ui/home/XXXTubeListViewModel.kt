package com.yenaly.han1meviewer.XXXTube.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.XXXTube.common.XXXTubeConstants
import com.yenaly.han1meviewer.XXXTube.data.model.XXXTubeListPage
import com.yenaly.han1meviewer.XXXTube.data.remote.XXXTubeRepo
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface XXXTubeListSource {

    data class Path(val path: String) : XXXTubeListSource
    data class Search(val query: String) : XXXTubeListSource
}

data class XXXTubeListUiState(
    val items: List<HanimeInfo> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
)

class XXXTubeListViewModel : ViewModel() {

    private val _state = MutableStateFlow(XXXTubeListUiState())
    val state: StateFlow<XXXTubeListUiState> = _state.asStateFlow()

    private var source: XXXTubeListSource? = null
    private var nextUrl: String? = null
    private var job: Job? = null

    fun setSource(newSource: XXXTubeListSource, force: Boolean = false) {
        if (!force && newSource == source && _state.value.items.isNotEmpty()) return
        source = newSource
        nextUrl = null
        job?.cancel()
        _state.value = XXXTubeListUiState(isLoading = true)
        job = viewModelScope.launch {
            XXXTubeRepo.videos(firstUrl(newSource))
                .onSuccess { applyFirst(it) }
                .onFailure { e ->
                    _state.value = XXXTubeListUiState(error = e.message ?: e.javaClass.simpleName)
                }
        }
    }

    fun refresh() {
        source?.let { setSource(it, force = true) }
    }

    fun loadMore() {
        val current = _state.value
        val url = nextUrl ?: return
        if (current.isLoading || current.isLoadingMore || !current.hasNext) return
        _state.update { it.copy(isLoadingMore = true) }
        job = viewModelScope.launch {
            XXXTubeRepo.videos(url)
                .onSuccess { result ->
                    nextUrl = result.nextUrl?.takeIf { it != url }
                    _state.update { old ->
                        val merged = (old.items + result.items).distinctBy { it.videoCode }
                        old.copy(
                            items = merged,
                            isLoadingMore = false,
                            hasNext = nextUrl != null && result.items.isNotEmpty(),
                        )
                    }
                }
                .onFailure { _state.update { old -> old.copy(isLoadingMore = false) } }
        }
    }

    private fun applyFirst(result: XXXTubeListPage) {
        nextUrl = result.nextUrl
        _state.value = XXXTubeListUiState(
            items = result.items,
            hasNext = result.nextUrl != null && result.items.isNotEmpty(),
        )
    }

    private fun firstUrl(src: XXXTubeListSource): String = when (src) {
        is XXXTubeListSource.Path -> XXXTubeConstants.absUrl(src.path)
        is XXXTubeListSource.Search ->
            XXXTubeConstants.searchUrl(src.query, XXXTubeConstants.SearchType.VIDEOS)
    }
}
