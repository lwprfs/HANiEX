package com.yenaly.han1meviewer.Coser.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.Coser.common.CoserConstants
import com.yenaly.han1meviewer.Coser.data.model.CoserCard
import com.yenaly.han1meviewer.Coser.data.model.CoserListPage
import com.yenaly.han1meviewer.Coser.data.model.CoserTerm
import com.yenaly.han1meviewer.Coser.data.remote.CoserRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CoserSource {

    data class Page(
        val path: String,
        val collectTerms: Boolean = false,
        val filters: CoserConstants.Filters = CoserConstants.Filters(),
    ) : CoserSource

    data class Search(
        val query: String,
        val filters: CoserConstants.Filters = CoserConstants.Filters(),
    ) : CoserSource
}

data class CoserListUiState(
    val cards: List<CoserCard> = emptyList(),
    val terms: List<CoserTerm> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
)

class CoserListViewModel : ViewModel() {

    private val _state = MutableStateFlow(CoserListUiState())
    val state: StateFlow<CoserListUiState> = _state.asStateFlow()

    private var source: CoserSource? = null
    private var nextUrl: String? = null
    private var page = 1
    private var job: Job? = null
    private var thumbJob: Job? = null

    fun setSource(newSource: CoserSource, force: Boolean = false) {
        if (!force && newSource == source && (_state.value.cards.isNotEmpty() || _state.value.terms.isNotEmpty())) return
        source = newSource
        page = 1
        nextUrl = null
        job?.cancel()
        thumbJob?.cancel()
        _state.value = CoserListUiState(isLoading = true)
        job = viewModelScope.launch {
            fetch(newSource, firstUrl(newSource))
                .onSuccess { applyFirst(it) }
                .onFailure { e ->
                    _state.value = CoserListUiState(error = e.message ?: e.javaClass.simpleName)
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

    private fun applyFirst(result: CoserListPage) {
        nextUrl = result.nextUrl
        _state.value = CoserListUiState(
            cards = result.cards,
            terms = result.terms,
            hasNext = result.hasNext && result.cards.isNotEmpty(),
        )
        fillMissingThumbs(result.terms)
    }

    private fun fillMissingThumbs(terms: List<CoserTerm>) {
        thumbJob?.cancel()
        val missing = terms.filter { it.thumbUrl.isBlank() }
        if (missing.isEmpty()) return
        thumbJob = viewModelScope.launch {
            val cached = missing.mapNotNull { t -> thumbCache[t.path]?.let { t.path to it } }.toMap()
            if (cached.isNotEmpty()) applyThumbs(cached)
            val todo = missing.filter { it.path !in cached }
            val gate = Semaphore(3)
            coroutineScope {
                for (term in todo) {
                    launch {
                        gate.withPermit {
                            val cover = CoserRepo.page(
                                CoserConstants.pageUrl(term.path), collectTerms = false,
                            ).getOrNull()?.cards?.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl
                            if (!cover.isNullOrBlank()) {
                                thumbCache[term.path] = cover
                                applyThumbs(mapOf(term.path to cover))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun applyThumbs(byPath: Map<String, String>) {
        _state.update { old ->
            old.copy(
                terms = old.terms.map { t ->
                    if (t.thumbUrl.isNotBlank()) t else byPath[t.path]?.let { t.copy(thumbUrl = it) } ?: t
                },
            )
        }
    }

    private fun firstUrl(src: CoserSource): String = when (src) {
        is CoserSource.Page -> CoserConstants.pageUrl(src.path, src.filters)
        is CoserSource.Search -> CoserConstants.searchUrl(src.query, 1, src.filters)
    }

    private fun guessNextUrl(src: CoserSource, n: Int): String = when (src) {
        is CoserSource.Page -> CoserConstants.pagedUrl(src.path, n, src.filters)
        is CoserSource.Search -> CoserConstants.searchUrl(src.query, n, src.filters)
    }

    private suspend fun fetch(src: CoserSource, url: String): Result<CoserListPage> =
        CoserRepo.page(url, collectTerms = (src as? CoserSource.Page)?.collectTerms == true)

    private companion object {
        val thumbCache = ConcurrentHashMap<String, String>()
    }
}
