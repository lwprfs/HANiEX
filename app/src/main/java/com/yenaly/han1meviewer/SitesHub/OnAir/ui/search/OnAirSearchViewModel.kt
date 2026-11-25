package com.yenaly.han1meviewer.SitesHub.OnAir.ui.search

import android.app.Application
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.screen.SiteSearchSettings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.SitesHub.OnAir.common.OnAirConstants
import com.yenaly.han1meviewer.SitesHub.OnAir.data.local.OnAirSearchHistoryEntry
import com.yenaly.han1meviewer.SitesHub.OnAir.data.local.OnAirSearchHistoryRepo
import com.yenaly.han1meviewer.SitesHub.OnAir.data.model.OnAirPageInfo
import com.yenaly.han1meviewer.SitesHub.OnAir.data.model.OnAirSearchResult
import com.yenaly.han1meviewer.SitesHub.OnAir.data.model.OnAirVideoCard
import com.yenaly.han1meviewer.SitesHub.OnAir.data.remote.OnAirNetworkRepo
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.home.OnAirPageStart
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OnAirSearchViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val _query = MutableStateFlow(savedStateHandle.get<String>("q") ?: "")
    val query = _query.asStateFlow()

    private val _field = MutableStateFlow(savedStateHandle.get<String>("field") ?: OnAirConstants.SEARCH_FIELD_TITLE)
    val field = _field.asStateFlow()

    private val _length = MutableStateFlow(savedStateHandle.get<String>("length") ?: OnAirConstants.LENGTH_ANY)
    val length = _length.asStateFlow()

    private val _date = MutableStateFlow(savedStateHandle.get<String>("date") ?: OnAirConstants.DATE_ANY)
    val date = _date.asStateFlow()

    private val _sort = MutableStateFlow(
        savedStateHandle.get<String>("sort") ?: configuredDefaultSort()
    )
    val sort = _sort.asStateFlow()

    private val _tagFilter = MutableStateFlow(savedStateHandle.get<String?>("tag"))
    val tagFilter = _tagFilter.asStateFlow()

    private val _state = MutableStateFlow<PageLoadingState<OnAirSearchResult>?>(null)
    val state = _state.asStateFlow()

    private val _results = MutableStateFlow<List<OnAirVideoCard>>(emptyList())
    val results = _results.asStateFlow()

    private val _hasSearched = MutableStateFlow(false)
    val hasSearched = _hasSearched.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage = _currentPage.asStateFlow()

    private val _pageInfo = MutableStateFlow<OnAirPageInfo?>(null)
    val pageInfo = _pageInfo.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()

    /** Where each loaded site page begins inside [results]; drives the live page number. */
    private val _pageStarts = MutableStateFlow<List<OnAirPageStart>>(emptyList())
    val pageStarts = _pageStarts.asStateFlow()

    /** Session-only first page of the current result list (never persisted). */
    private val _startPage = MutableStateFlow(1)
    val startPage = _startPage.asStateFlow()

    private var searchJob: Job? = null

    fun setQuery(value: String) {
        _query.value = value
        savedStateHandle["q"] = value
    }

    fun setField(value: String) {
        _field.value = value
        savedStateHandle["field"] = value
    }

    fun setLength(value: String) {
        _length.value = value
        savedStateHandle["length"] = value
    }

    fun setDate(value: String) {
        _date.value = value
        savedStateHandle["date"] = value
    }

    fun setSort(value: String) {
        _sort.value = value
        savedStateHandle["sort"] = value
    }

    fun setTagFilter(value: String?) {
        _tagFilter.value = value
        savedStateHandle["tag"] = value
    }

    fun applyFiltersFromHistory(entry: OnAirSearchHistoryEntry) {
        setQuery(entry.query)
        setField(entry.field)
        setLength(entry.length)
        setDate(entry.date)
        setSort(entry.sort)
        setTagFilter(entry.tag)
    }

    fun reset() {
        _query.value = ""
        _field.value = OnAirConstants.SEARCH_FIELD_TITLE
        _length.value = OnAirConstants.LENGTH_ANY
        _date.value = OnAirConstants.DATE_ANY
        _sort.value = configuredDefaultSort()
        _tagFilter.value = null
        _results.value = emptyList()
        _state.value = null
        _hasSearched.value = false
        _currentPage.value = 1
        _startPage.value = 1
        _pageStarts.value = emptyList()
        _pageInfo.value = null
        _isLoadingMore.value = false
        savedStateHandle["q"] = ""
        savedStateHandle["field"] = OnAirConstants.SEARCH_FIELD_TITLE
        savedStateHandle["length"] = OnAirConstants.LENGTH_ANY
        savedStateHandle["date"] = OnAirConstants.DATE_ANY
        savedStateHandle["sort"] = configuredDefaultSort()
        savedStateHandle["tag"] = null
    }

    fun performSearch() {
        startSearch(page = 1, saveToHistory = true)
    }

    /**
     * Re-run the current search starting at [page]. Returns false (and does nothing) if the page is
     * out of range. The chosen page only lives for this session.
     */
    fun jumpToPage(page: Int): Boolean {
        if (!_hasSearched.value) return false
        val total = _pageInfo.value?.total
        if (page < 1 || (total != null && page > total)) return false
        startSearch(page = page, saveToHistory = false, keepPageInfo = true)
        return true
    }

    private fun startSearch(page: Int, saveToHistory: Boolean, keepPageInfo: Boolean = false) {
        searchJob?.cancel()
        _startPage.value = page
        _currentPage.value = page
        _pageStarts.value = listOf(OnAirPageStart(page, 0))
        _hasSearched.value = true
        _results.value = emptyList()
        _state.value = PageLoadingState.Loading
        _isLoadingMore.value = false
        if (!keepPageInfo) _pageInfo.value = null

        val q = _query.value
        val tag = _tagFilter.value
        if (saveToHistory) persistHistory(q, tag)

        searchJob = viewModelScope.launch {
            if (!tag.isNullOrBlank()) {
                OnAirNetworkRepo.getTagPage(
                    tagName = tag,
                    page = page,
                    query = q.takeIf { it.isNotBlank() },
                    field = _field.value,
                    length = _length.value,
                    date = _date.value,
                    sort = _sort.value,
                ).collect { s -> handleState(s, page) }
            } else {
                OnAirNetworkRepo.searchVideos(
                    page = page,
                    query = q,
                    field = _field.value,
                    length = _length.value,
                    date = _date.value,
                    sort = _sort.value,
                ).collect { s -> handleState(s, page) }
            }
        }
    }

    fun loadNextPage() {
        if (_isLoadingMore.value) return
        if (!_hasSearched.value) return
        val info = _pageInfo.value ?: return
        if (_currentPage.value >= info.total) return
        val next = _currentPage.value + 1
        val q = _query.value
        val tag = _tagFilter.value
        searchJob?.cancel()
        _isLoadingMore.value = true
        searchJob = viewModelScope.launch {
            if (!tag.isNullOrBlank()) {
                OnAirNetworkRepo.getTagPage(
                    tagName = tag,
                    page = next,
                    query = q.takeIf { it.isNotBlank() },
                    field = _field.value,
                    length = _length.value,
                    date = _date.value,
                    sort = _sort.value,
                ).collect { s -> handleState(s, next) }
            } else {
                OnAirNetworkRepo.searchVideos(
                    page = next,
                    query = q,
                    field = _field.value,
                    length = _length.value,
                    date = _date.value,
                    sort = _sort.value,
                ).collect { s -> handleState(s, next) }
            }
        }
    }

    private fun handleState(s: PageLoadingState<OnAirSearchResult>, page: Int) {
        if (!viewModelScope.isActive) return
        _state.value = s
        when (s) {
            is PageLoadingState.Success -> {
                val incoming = s.info.cards
                val isFirst = page == _startPage.value
                val prevSize = _results.value.size
                if (!isFirst && incoming.isNotEmpty()) {
                    _pageStarts.update { it + OnAirPageStart(page, prevSize) }
                }
                _results.update { prev ->
                    if (isFirst) incoming else (prev + incoming).distinctBy { it.videoId }
                }
                _pageInfo.value = s.info.pageInfo
                _currentPage.value = page
                _isLoadingMore.value = false
            }
            is PageLoadingState.NoMoreData -> {
                _isLoadingMore.value = false
            }
            is PageLoadingState.Error -> {
                _isLoadingMore.value = false
            }
            is PageLoadingState.Loading -> Unit
        }
    }

    private fun persistHistory(query: String, tag: String?) {
        val hasAnyFilter = query.isNotBlank() ||
                tag != null ||
                _field.value != OnAirConstants.SEARCH_FIELD_TITLE ||
                _length.value != OnAirConstants.LENGTH_ANY ||
                _date.value != OnAirConstants.DATE_ANY ||
                _sort.value != OnAirConstants.SORT_NEWEST
        if (!hasAnyFilter) return
        OnAirSearchHistoryRepo.push(
            OnAirSearchHistoryEntry(
                query = query.trim(),
                field = _field.value,
                length = _length.value,
                date = _date.value,
                sort = _sort.value,
                tag = tag,
            )
        )
    }

    private fun configuredDefaultSort(): String =
        SiteSearchSettings.defaultSort(SiteType.ONAIR) ?: OnAirConstants.SORT_NEWEST
}
