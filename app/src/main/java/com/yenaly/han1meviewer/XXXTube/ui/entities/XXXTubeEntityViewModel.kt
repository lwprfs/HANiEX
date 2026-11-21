package com.yenaly.han1meviewer.XXXTube.ui.entities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.XXXTube.common.XXXTubeConstants
import com.yenaly.han1meviewer.XXXTube.data.model.XXXTubeEntity
import com.yenaly.han1meviewer.XXXTube.data.model.XXXTubeEntityPage
import com.yenaly.han1meviewer.XXXTube.data.remote.XXXTubeRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class XXXTubeEntitySource(
    val kind: XXXTubeConstants.EntityKind,
    val query: String? = null,
)

data class XXXTubeEntityUiState(
    val items: List<XXXTubeEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
)

class XXXTubeEntityViewModel : ViewModel() {

    private val _state = MutableStateFlow(XXXTubeEntityUiState())
    val state: StateFlow<XXXTubeEntityUiState> = _state.asStateFlow()

    private var source: XXXTubeEntitySource? = null
    private var nextUrl: String? = null
    private var job: Job? = null

    fun setSource(newSource: XXXTubeEntitySource, force: Boolean = false) {
        if (!force && newSource == source && _state.value.items.isNotEmpty()) return
        source = newSource
        nextUrl = null
        job?.cancel()
        _state.value = XXXTubeEntityUiState(isLoading = true)
        job = viewModelScope.launch {
            XXXTubeRepo.entities(firstUrl(newSource), newSource.kind)
                .onSuccess { applyFirst(it) }
                .onFailure { e ->
                    _state.value = XXXTubeEntityUiState(error = e.message ?: e.javaClass.simpleName)
                }
        }
    }

    fun refresh() {
        source?.let { setSource(it, force = true) }
    }

    fun loadMore() {
        val current = _state.value
        val url = nextUrl ?: return
        val kind = source?.kind ?: return
        if (current.isLoading || current.isLoadingMore || !current.hasNext) return
        _state.update { it.copy(isLoadingMore = true) }
        job = viewModelScope.launch {
            XXXTubeRepo.entities(url, kind)
                .onSuccess { result ->
                    nextUrl = result.nextUrl?.takeIf { it != url }
                    _state.update { old ->
                        old.copy(
                            items = (old.items + result.items).distinctBy { it.path },
                            isLoadingMore = false,
                            hasNext = nextUrl != null && result.items.isNotEmpty(),
                        )
                    }
                }
                .onFailure { _state.update { old -> old.copy(isLoadingMore = false) } }
        }
    }

    private fun applyFirst(result: XXXTubeEntityPage) {
        nextUrl = result.nextUrl
        _state.value = XXXTubeEntityUiState(
            items = result.items,
            hasNext = result.nextUrl != null && result.items.isNotEmpty(),
        )
    }

    private fun firstUrl(src: XXXTubeEntitySource): String =
        if (src.query.isNullOrBlank()) {
            XXXTubeConstants.absUrl(src.kind.listPath)
        } else {
            val type = when (src.kind) {
                XXXTubeConstants.EntityKind.MODELS -> XXXTubeConstants.SearchType.MODELS
                XXXTubeConstants.EntityKind.CATEGORIES -> XXXTubeConstants.SearchType.CATEGORIES
            }
            XXXTubeConstants.searchUrl(src.query, type)
        }
}
