package com.yenaly.han1meviewer.SitesHub.Buondua.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.SitesHub.Buondua.common.BuonduaConstants
import com.yenaly.han1meviewer.SitesHub.Buondua.data.model.BuonduaListPage
import com.yenaly.han1meviewer.SitesHub.Buondua.data.remote.BuonduaRepo
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface BuonduaSource {
    data class Category(val path: String) : BuonduaSource
    data class Search(val query: String) : BuonduaSource

    data class Url(val url: String) : BuonduaSource

    fun firstUrl(): String = when (this) {
        is Category -> BuonduaConstants.categoryUrl(path)
        is Search -> BuonduaConstants.searchUrl(query)
        is Url -> url
    }
}

data class BuonduaListUiState(
    val items: List<HanimeInfo> = emptyList(),
    val heading: String? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val nextUrl: String? = null,
    val error: String? = null,
) {
    val hasNext: Boolean get() = nextUrl != null
}

class BuonduaListViewModel : ViewModel() {

    private val _state = MutableStateFlow(BuonduaListUiState())
    val state: StateFlow<BuonduaListUiState> = _state.asStateFlow()

    private var source: BuonduaSource? = null
    private var job: Job? = null

    fun setSource(newSource: BuonduaSource, force: Boolean = false) {
        if (!force && newSource == source && _state.value.items.isNotEmpty()) return
        source = newSource
        job?.cancel()
        _state.value = BuonduaListUiState(isLoading = true)
        job = viewModelScope.launch {
            BuonduaRepo.list(newSource.firstUrl())
                .onSuccess { applyFirst(it) }
                .onFailure { e ->
                    _state.value = BuonduaListUiState(error = e.message ?: e.javaClass.simpleName)
                }
        }
    }

    fun refresh() {
        source?.let { setSource(it, force = true) }
    }

    fun loadMore() {
        val current = _state.value
        val next = current.nextUrl ?: return
        if (current.isLoading || current.isLoadingMore) return
        _state.update { it.copy(isLoadingMore = true) }
        job = viewModelScope.launch {
            BuonduaRepo.list(next)
                .onSuccess { result ->
                    _state.update { old ->
                        val merged = (old.items + result.items).distinctBy { it.videoCode }

                        val progressed = merged.size > old.items.size
                        old.copy(
                            items = merged,
                            isLoadingMore = false,
                            nextUrl = result.nextUrl.takeIf { progressed && it != next },
                        )
                    }
                }
                .onFailure { _state.update { old -> old.copy(isLoadingMore = false) } }
        }
    }

    private fun applyFirst(result: BuonduaListPage) {
        _state.value = BuonduaListUiState(
            items = result.items,
            heading = result.heading,
            nextUrl = result.nextUrl.takeIf { result.items.isNotEmpty() },
        )
    }
}
