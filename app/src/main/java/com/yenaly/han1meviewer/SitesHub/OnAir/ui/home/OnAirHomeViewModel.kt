package com.yenaly.han1meviewer.SitesHub.OnAir.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.SitesHub.OnAir.data.local.OnAirProfileCache
import com.yenaly.han1meviewer.SitesHub.OnAir.data.model.OnAirPageInfo
import com.yenaly.han1meviewer.SitesHub.OnAir.data.model.OnAirUserProfile
import com.yenaly.han1meviewer.SitesHub.OnAir.data.model.OnAirVideoCard
import com.yenaly.han1meviewer.SitesHub.OnAir.data.remote.OnAirNetworkRepo
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.logic.state.VideoLoadingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OnAirHomeViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "OnAirHomeViewModel"
        private const val CACHE_TTL_MS = 5 * 60 * 1000L
    }

    private val _homeState = MutableStateFlow<WebsiteState<OnAirHomeUiData>>(WebsiteState.Loading)
    val homeState = _homeState.asStateFlow()

    private val _cards = MutableStateFlow<List<OnAirVideoCard>>(emptyList())
    val cards = _cards.asStateFlow()

    private val _pageInfo = MutableStateFlow<OnAirPageInfo?>(null)
    val pageInfo = _pageInfo.asStateFlow()

    private val _loadedPage = MutableStateFlow(1)
    val loadedPage = _loadedPage.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()

    /** Where each loaded site page begins inside [cards]; drives the live page number. */
    private val _pageStarts = MutableStateFlow<List<OnAirPageStart>>(listOf(OnAirPageStart(1, 0)))
    val pageStarts = _pageStarts.asStateFlow()

    /** Session-only page to start from (never persisted, so a restart goes back to page 1). */
    private val _startPage = MutableStateFlow(1)
    val startPage = _startPage.asStateFlow()

    private val _currentUser = MutableStateFlow<OnAirUserProfile?>(null)
    val currentUser = _currentUser.asStateFlow()

    private var lastFetchAt = 0L
    private var currentPage = 1
    private var hasMore = true

    private var currentQuery: String? = null
    private var currentField: String? = null
    private var currentLength: String? = null
    private var currentDate: String? = null
    private var currentSort: String? = null

    private var profileJob: Job? = null
    private var homeJob: Job? = null
    private var moreJob: Job? = null

    fun loadHomePage(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastFetchAt < CACHE_TTL_MS &&
            _homeState.value is WebsiteState.Success
        ) return
        launchLoad(_startPage.value)
    }

    private fun launchLoad(page: Int) {
        homeJob?.cancel()
        moreJob?.cancel()
        _isLoadingMore.value = false
        homeJob = viewModelScope.launch {
            OnAirNetworkRepo.getHomePage(
                page = page,
                query = currentQuery,
                field = currentField,
                length = currentLength,
                date = currentDate,
                sort = currentSort,
            ).collect { state ->
                if (!isActive) return@collect
                when (state) {
                    is WebsiteState.Loading -> {
                        if (_cards.value.isEmpty()) _homeState.value = WebsiteState.Loading
                    }

                    is WebsiteState.Success -> {
                        val info = state.info
                        if (page > 1 && info.videos.isEmpty()) {
                            // Page does not exist on the site: fall back to page 1 for the retry.
                            _startPage.value = 1
                            _homeState.value = WebsiteState.Error(
                                IllegalStateException(
                                    getApplication<Application>()
                                        .getString(R.string.onair_page_empty, page)
                                )
                            )
                            return@collect
                        }
                        lastFetchAt = System.currentTimeMillis()
                        currentPage = page
                        hasMore = true
                        _loadedPage.value = page
                        _pageStarts.value = listOf(OnAirPageStart(page, 0))
                        _cards.value = info.videos
                        _pageInfo.value = info.pageInfo
                        if (info.pageInfo == null) {
                            hasMore = info.videos.isNotEmpty()
                        }

                        info.currentUsername?.let { name ->
                            OnAirProfileCache.setCurrentUsername(name)
                            val cached = OnAirProfileCache.get(name)
                            if (cached != null) {
                                _currentUser.value = cached
                            } else {
                                refreshCurrentUser()
                            }
                        }

                        _homeState.value = WebsiteState.Success(
                            OnAirHomeUiData(
                                videos = info.videos,
                                totalVideos = info.totalVideos,
                                totalPictures = info.totalPictures,
                                totalMembers = info.totalMembers,
                                announcementTitle = info.announcement?.title,
                                announcementUrl = info.announcement?.url,
                            )
                        )
                    }

                    is WebsiteState.Error -> {
                        Log.e(TAG, "Home load failed", state.throwable)
                        _homeState.value = WebsiteState.Error(state.throwable)
                    }
                }
            }
        }
    }

    /**
     * Restart the feed from [page]. Returns false (and does nothing) if the page is out of range.
     * The choice only lives for this session; it is not saved as the home start page.
     */
    fun jumpToPage(page: Int): Boolean {
        val total = _pageInfo.value?.total
        if (page < 1 || (total != null && page > total)) return false
        _startPage.value = page
        lastFetchAt = 0L
        _cards.value = emptyList()
        _pageStarts.value = listOf(OnAirPageStart(page, 0))
        _loadedPage.value = page
        currentPage = page
        hasMore = true
        _homeState.value = WebsiteState.Loading
        launchLoad(page)
        return true
    }

    fun refresh() {
        lastFetchAt = 0L
        OnAirProfileCache.invalidate()
        _currentUser.value = null
        loadHomePage(force = true)
    }

    fun loadNextPage() {
        if (_isLoadingMore.value || !hasMore) return
        if (currentPage >= (_pageInfo.value?.total ?: Int.MAX_VALUE)) {
            hasMore = false
            return
        }
        val nextPage = currentPage + 1

        moreJob = viewModelScope.launch {
            _isLoadingMore.value = true
            OnAirNetworkRepo.getHomePagePaged(
                page = nextPage,
                query = currentQuery,
                field = currentField,
                length = currentLength,
                date = currentDate,
                sort = currentSort,
            ).collect { state ->
                if (!isActive) return@collect
                when (state) {
                    is PageLoadingState.Success -> {
                        val pair = state.info
                        val newCards = pair.first
                        val prevSize = _cards.value.size
                        if (newCards.isNotEmpty()) {
                            _pageStarts.update { it + OnAirPageStart(nextPage, prevSize) }
                        }
                        _cards.update { prev ->
                            (prev + newCards).distinctBy { it.videoId }
                        }
                        pair.second?.let { _pageInfo.value = it }
                        currentPage = nextPage
                        _loadedPage.value = nextPage
                        _isLoadingMore.value = false
                        if (newCards.isEmpty()) hasMore = false
                        _homeState.update { s ->
                            if (s is WebsiteState.Success) {
                                s.copy(info = s.info.copy(videos = _cards.value))
                            } else s
                        }
                    }

                    is PageLoadingState.NoMoreData -> {
                        _isLoadingMore.value = false
                        hasMore = false
                    }

                    is PageLoadingState.Error -> {
                        _isLoadingMore.value = false
                    }

                    is PageLoadingState.Loading -> Unit
                }
            }
        }
    }

    fun applyHomeFilters(
        query: String? = null,
        field: String? = null,
        length: String? = null,
        date: String? = null,
        sort: String? = null,
    ) {
        currentQuery = query
        currentField = field
        currentLength = length
        currentDate = date
        currentSort = sort
        currentPage = 1
        hasMore = true
        lastFetchAt = 0L
        _startPage.value = 1
        _cards.value = emptyList()
        _pageInfo.value = null
        _loadedPage.value = 1
        _pageStarts.value = listOf(OnAirPageStart(1, 0))
        loadHomePage(force = true)
    }

    fun currentUsername(): String? =
        _currentUser.value?.username ?: OnAirProfileCache.getCurrentUsername()

    fun refreshCurrentUser(force: Boolean = false) {
        val name = OnAirProfileCache.getCurrentUsername() ?: return
        if (!force) {
            val cached = OnAirProfileCache.get(name)
            if (cached != null) {
                _currentUser.value = cached
                return
            }
        }
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            OnAirNetworkRepo.getUserProfile(name).collect { state ->
                when (state) {
                    is VideoLoadingState.Success -> {
                        val profile = state.info
                        _currentUser.value = profile
                        OnAirProfileCache.put(name, profile)
                    }

                    is VideoLoadingState.Error -> {
                        Log.w(TAG, "refreshCurrentUser failed", state.throwable)
                    }

                    else -> Unit
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        profileJob?.cancel()
        homeJob?.cancel()
        moreJob?.cancel()
    }
}

data class OnAirHomeUiData(
    val videos: List<OnAirVideoCard>,
    val totalVideos: Int?,
    val totalPictures: Int?,
    val totalMembers: Int?,
    val announcementTitle: String?,
    val announcementUrl: String?,
)
