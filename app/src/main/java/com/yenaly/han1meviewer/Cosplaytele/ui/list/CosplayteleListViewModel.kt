package com.yenaly.han1meviewer.Cosplaytele.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.Cosplaytele.common.CosplayteleConstants
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleCard
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleListPage
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleTerm
import com.yenaly.han1meviewer.Cosplaytele.data.remote.CosplayteleRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CosplayteleSource {

    data class Page(val path: String, val collectTerms: Boolean = false) : CosplayteleSource
    data class Search(val query: String) : CosplayteleSource
}

data class CosplayteleListUiState(
    val cards: List<CosplayteleCard> = emptyList(),
    val terms: List<CosplayteleTerm> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
)

class CosplayteleListViewModel : ViewModel() {

    private val _state = MutableStateFlow(CosplayteleListUiState())
    val state: StateFlow<CosplayteleListUiState> = _state.asStateFlow()

    private var source: CosplayteleSource? = null
    private var nextUrl: String? = null
    private var page = 1
    private var job: Job? = null

    fun setSource(newSource: CosplayteleSource, force: Boolean = false) {
        if (!force && newSource == source && (_state.value.cards.isNotEmpty() || _state.value.terms.isNotEmpty())) return
        source = newSource
        page = 1
        nextUrl = null
        job?.cancel()
        _state.value = CosplayteleListUiState(isLoading = true)
        job = viewModelScope.launch {
            fetch(newSource, firstUrl(newSource))
                .onSuccess { applyFirst(it) }
                .onFailure { e ->
                    _state.value = CosplayteleListUiState(error = e.message ?: e.javaClass.simpleName)
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
            val url = nextUrl ?: guessNextUrl(src, page + 1)
            fetch(src, url)
                .onSuccess { result ->
                    page++
                    nextUrl = result.nextUrl
                    _state.update { old ->
                        val merged = (old.cards + result.cards).distinctBy { it.slug }
                        old.copy(
                            cards = merged,
                            isLoadingMore = false,
                            hasNext = result.hasNext && merged.size > old.cards.size,
                        )
                    }
                }
                .onFailure { _state.update { old -> old.copy(isLoadingMore = false, hasNext = false) } }
        }
    }

    private fun applyFirst(result: CosplayteleListPage) {
        nextUrl = result.nextUrl
        _state.value = CosplayteleListUiState(
            cards = result.cards,
            terms = result.terms,
            hasNext = result.hasNext && result.cards.isNotEmpty(),
        )
    }

    private fun firstUrl(src: CosplayteleSource): String = when (src) {
        is CosplayteleSource.Page -> CosplayteleConstants.pageUrl(src.path)
        is CosplayteleSource.Search -> CosplayteleConstants.searchUrl(src.query)
    }

    private fun guessNextUrl(src: CosplayteleSource, n: Int): String = when (src) {
        is CosplayteleSource.Page -> CosplayteleConstants.pagedUrl(src.path, n)
        is CosplayteleSource.Search -> CosplayteleConstants.searchUrl(src.query, n)
    }

    private suspend fun fetch(src: CosplayteleSource, url: String): Result<CosplayteleListPage> =
        CosplayteleRepo.page(url, collectTerms = (src as? CosplayteleSource.Page)?.collectTerms == true)
}
