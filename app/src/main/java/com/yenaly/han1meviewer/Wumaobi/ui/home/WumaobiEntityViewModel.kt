package com.yenaly.han1meviewer.Wumaobi.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.Wumaobi.common.WumaobiConstants
import com.yenaly.han1meviewer.Wumaobi.data.model.WumaobiEntity
import com.yenaly.han1meviewer.Wumaobi.data.remote.WumaobiRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WumaobiEntityUiState(
    val items: List<WumaobiEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
)

class WumaobiEntityViewModel : ViewModel() {

    private val _state = MutableStateFlow(WumaobiEntityUiState())
    val state: StateFlow<WumaobiEntityUiState> = _state.asStateFlow()

    private var kind: WumaobiConstants.EntityKind? = null
    private var sortPath: String = ""
    private var page = 1
    private var job: Job? = null

    fun setSource(newKind: WumaobiConstants.EntityKind, newSortPath: String, force: Boolean = false) {
        if (!force && newKind == kind && newSortPath == sortPath && _state.value.items.isNotEmpty()) return
        kind = newKind
        sortPath = newSortPath
        page = 1
        job?.cancel()
        _state.value = WumaobiEntityUiState(isLoading = true)
        job = viewModelScope.launch {
            WumaobiRepo.entities(newKind, newSortPath, 1)
                .onSuccess {
                    _state.value = WumaobiEntityUiState(
                        items = it.items,
                        hasNext = it.hasNext && it.items.isNotEmpty(),
                    )
                }
                .onFailure { e ->
                    _state.value = WumaobiEntityUiState(error = e.message ?: e.javaClass.simpleName)
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
            WumaobiRepo.entities(k, sortPath, next)
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
