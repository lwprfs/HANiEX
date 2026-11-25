package com.yenaly.han1meviewer.SitesHub.Coomer.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.SitesHub.Coomer.common.CoomerConstants
import com.yenaly.han1meviewer.SitesHub.Coomer.data.model.CoomerCard
import com.yenaly.han1meviewer.SitesHub.Coomer.data.model.CoomerListPage
import com.yenaly.han1meviewer.SitesHub.Coomer.data.model.CoomerTerm
import com.yenaly.han1meviewer.SitesHub.Coomer.data.remote.CoomerRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CoomerSource {

    data class Page(val path: String) : CoomerSource

    /** [posts] = false searches creators, true searches posts. */
    data class Search(val query: String, val posts: Boolean = false) : CoomerSource
}

data class CoomerListUiState(
    val cards: List<CoomerCard> = emptyList(),
    val terms: List<CoomerTerm> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
)

class CoomerListViewModel : ViewModel() {

    private val _state = MutableStateFlow(CoomerListUiState())
    val state: StateFlow<CoomerListUiState> = _state.asStateFlow()

    private var source: CoomerSource? = null
    private var nextUrl: String? = null
    private var page = 1
    private var job: Job? = null

    fun setSource(newSource: CoomerSource, force: Boolean = false) {
        if (!force && newSource == source &&
            (_state.value.cards.isNotEmpty() || _state.value.terms.isNotEmpty())
        ) return
        source = newSource
        page = 1
        nextUrl = null
        job?.cancel()
        _state.value = CoomerListUiState(isLoading = true)
        job = viewModelScope.launch {
            CoomerRepo.page(firstUrl(newSource))
                .onSuccess { applyFirst(it) }
                .onFailure { e ->
                    _state.value = CoomerListUiState(error = e.message ?: e.javaClass.simpleName)
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
            val url = nextUrl ?: pagedUrl(src, page + 1)
            CoomerRepo.page(url)
                .onSuccess { result ->
                    page++
                    nextUrl = result.nextUrl
                    _state.update { old ->
                        val cards = (old.cards + result.cards).distinctBy { it.slug }
                        val terms = (old.terms + result.terms).distinctBy { it.slug }
                        val grew = cards.size > old.cards.size || terms.size > old.terms.size
                        old.copy(
                            cards = cards,
                            terms = terms,
                            isLoadingMore = false,
                            hasNext = result.hasNext && grew,
                        )
                    }
                }
                .onFailure { _state.update { old -> old.copy(isLoadingMore = false, hasNext = false) } }
        }
    }

    private fun applyFirst(result: CoomerListPage) {
        nextUrl = result.nextUrl
        _state.value = CoomerListUiState(
            cards = result.cards,
            terms = result.terms,
            hasNext = result.hasNext && (result.cards.isNotEmpty() || result.terms.isNotEmpty()),
        )
    }

    private fun firstUrl(src: CoomerSource): String = pagedUrl(src, 1)

    private fun pagedUrl(src: CoomerSource, n: Int): String = when (src) {
        is CoomerSource.Page -> CoomerConstants.pagedUrl(src.path, n)
        is CoomerSource.Search -> CoomerConstants.searchUrl(src.query, src.posts, n)
    }
}
