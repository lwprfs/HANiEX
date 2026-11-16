package com.yenaly.han1meviewer.Cosplaytele.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.Cosplaytele.common.CosplayteleConstants
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleCard
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleListPage
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleTerm
import com.yenaly.han1meviewer.Cosplaytele.data.remote.CosplayteleRepo
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
    private var thumbJob: Job? = null

    fun setSource(newSource: CosplayteleSource, force: Boolean = false) {
        if (!force && newSource == source && (_state.value.cards.isNotEmpty() || _state.value.terms.isNotEmpty())) return
        source = newSource
        page = 1
        nextUrl = null
        job?.cancel()
        thumbJob?.cancel()
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
        fillMissingThumbs(result.terms)
    }

    /**
     * Some term pages (Top search in particular) are plain text links with no picture. For those
     * tiles, use the cover of the first post on the term's own page, a few at a time, and update
     * the grid as each one arrives.
     */
    private fun fillMissingThumbs(terms: List<CosplayteleTerm>) {
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
                            val cover = CosplayteleRepo.page(
                                CosplayteleConstants.pageUrl(term.path), collectTerms = false,
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

    private companion object {
        val thumbCache = ConcurrentHashMap<String, String>()
    }
}
