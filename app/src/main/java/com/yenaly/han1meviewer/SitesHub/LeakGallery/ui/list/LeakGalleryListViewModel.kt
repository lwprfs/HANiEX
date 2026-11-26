package com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryCard
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryListPage
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryTerm
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote.LeakGalleryRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LeakGallerySource {

    data class Page(val path: String) : LeakGallerySource

    data class Search(val query: String, val creators: Boolean = false) : LeakGallerySource

    /** Media the signed-in user liked (`likes/me-media-like/<n>`). */
    data object Liked : LeakGallerySource

    /** Creators the signed-in user follows (`profile-subscription/<n>`). */
    data object Following : LeakGallerySource
}

data class LeakGalleryListUiState(
    val cards: List<LeakGalleryCard> = emptyList(),
    val terms: List<LeakGalleryTerm> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
)

class LeakGalleryListViewModel : ViewModel() {

    private val _state = MutableStateFlow(LeakGalleryListUiState())
    val state: StateFlow<LeakGalleryListUiState> = _state.asStateFlow()

    private var source: LeakGallerySource? = null
    private var page = 1
    private var job: Job? = null

    fun setSource(newSource: LeakGallerySource, force: Boolean = false) {
        if (!force && newSource == source &&
            (_state.value.cards.isNotEmpty() || _state.value.terms.isNotEmpty())
        ) return
        source = newSource
        page = 1
        job?.cancel()
        _state.value = LeakGalleryListUiState(isLoading = true)
        job = viewModelScope.launch {
            fetch(newSource, 1)
                .onSuccess { result ->
                    _state.value = LeakGalleryListUiState(
                        cards = result.cards,
                        terms = result.terms,
                        hasNext = result.hasNext,
                    )
                }
                .onFailure { e ->
                    _state.value = LeakGalleryListUiState(error = e.message ?: e.javaClass.simpleName)
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
            fetch(src, page + 1)
                .onSuccess { result ->
                    page++
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

    private suspend fun fetch(src: LeakGallerySource, n: Int): Result<LeakGalleryListPage> = when (src) {
        is LeakGallerySource.Page -> LeakGalleryRepo.page(src.path, n)
        is LeakGallerySource.Search -> LeakGalleryRepo.search(src.query, src.creators, n)
        LeakGallerySource.Liked -> LeakGalleryRepo.likedMedia(n)
        LeakGallerySource.Following -> LeakGalleryRepo.following(n)
    }
}
