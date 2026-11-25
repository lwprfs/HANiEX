package com.yenaly.han1meviewer.SitesHub.FullVideos.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.SitesHub.FullVideos.common.FullVideosConstants
import com.yenaly.han1meviewer.SitesHub.FullVideos.data.model.FullVideosEntity
import com.yenaly.han1meviewer.SitesHub.FullVideos.data.remote.FullVideosRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FullVideosEntityUiState(
    val items: List<FullVideosEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
)

class FullVideosEntityViewModel : ViewModel() {

    private val _state = MutableStateFlow(FullVideosEntityUiState())
    val state: StateFlow<FullVideosEntityUiState> = _state.asStateFlow()

    private var kind: FullVideosConstants.EntityKind? = null
    private var sortPath: String = ""
    private var page = 1
    private var job: Job? = null

    fun setSource(newKind: FullVideosConstants.EntityKind, newSortPath: String, force: Boolean = false) {
        if (!force && newKind == kind && newSortPath == sortPath && _state.value.items.isNotEmpty()) return
        kind = newKind
        sortPath = newSortPath
        page = 1
        job?.cancel()
        _state.value = FullVideosEntityUiState(isLoading = true)
        job = viewModelScope.launch {
            FullVideosRepo.entities(newKind, newSortPath, 1)
                .onSuccess {
                    _state.value = FullVideosEntityUiState(
                        items = it.items,
                        hasNext = it.hasNext && it.items.isNotEmpty(),
                    )
                }
                .onFailure { e ->
                    _state.value = FullVideosEntityUiState(error = e.message ?: e.javaClass.simpleName)
                }
        }
    }

    fun refresh() {
        kind?.let { setSource(it, sortPath, force = true) }
    }

    fun loadMore() {
        val current = _state.value
        val k = kind ?: return
        if (current.isLoading || current.isLoadingMore || !current.hasNext) return
        _state.update { it.copy(isLoadingMore = true) }
        job = viewModelScope.launch {
            val next = page + 1
            FullVideosRepo.entities(k, sortPath, next)
                .onSuccess { result ->
                    page = next
                    _state.update { old ->
                        old.copy(
                            items = (old.items + result.items).distinctBy { it.path },
                            isLoadingMore = false,
                            hasNext = result.hasNext && result.items.isNotEmpty(),
                        )
                    }
                }
                .onFailure { _state.update { old -> old.copy(isLoadingMore = false) } }
        }
    }
}
