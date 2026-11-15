package com.yenaly.han1meviewer.MissAV.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.MissAV.data.model.MissAvActress
import com.yenaly.han1meviewer.MissAV.data.model.MissAvActressProfile
import com.yenaly.han1meviewer.MissAV.data.model.MissAvTweet
import com.yenaly.han1meviewer.MissAV.data.model.MissAvListPage
import com.yenaly.han1meviewer.MissAV.data.model.MissAvPlaylist
import com.yenaly.han1meviewer.MissAV.data.model.MissAvUser
import com.yenaly.han1meviewer.MissAV.data.remote.MissAvAccountRepo
import com.yenaly.han1meviewer.MissAV.data.remote.MissAvAuth
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PagedUiState<T>(
    val items: List<T> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val hasNext: Boolean = false,
    val loaded: Boolean = false,
    val page: Int = 0,
)

class PagedLoader<T>(
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val key: (T) -> Any,
    private val fetch: suspend (page: Int) -> MissAvListPage<T>,
) {
    private val _state = MutableStateFlow(PagedUiState<T>())
    val state: StateFlow<PagedUiState<T>> = _state.asStateFlow()
    private var job: Job? = null

    fun loadIfNeeded() {
        val s = _state.value
        if (!s.loaded && !s.loading) refresh()
    }

    fun refresh() {
        job?.cancel()
        job = scope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val r = fetch(1)
                _state.value = PagedUiState(r.items, hasNext = r.hasNext, loaded = true, page = 1)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: e.toString(), loaded = true) }
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.loadingMore || !s.hasNext) return
        job = scope.launch {
            _state.update { it.copy(loadingMore = true) }
            try {
                val r = fetch(s.page + 1)
                _state.update { cur ->
                    val merged = (cur.items + r.items).distinctBy(key)
                    cur.copy(items = merged, loadingMore = false, hasNext = r.hasNext && r.items.isNotEmpty(), page = s.page + 1)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loadingMore = false, error = e.message) }
            }
        }
    }

    fun removeWhere(predicate: (T) -> Boolean) = _state.update { it.copy(items = it.items.filterNot(predicate)) }
    fun clear() = _state.update { it.copy(items = emptyList(), hasNext = false) }
}

data class LoginUiState(val loading: Boolean = false, val error: String? = null)

class MissAvAccountViewModel : ViewModel() {

    val user: StateFlow<MissAvUser?> = MissAvAuth.user

    private val _login = MutableStateFlow(LoginUiState())
    val login: StateFlow<LoginUiState> = _login.asStateFlow()

    fun login(login: String, password: String, remember: Boolean, onSuccess: () -> Unit) {
        if (_login.value.loading) return
        viewModelScope.launch {
            _login.value = LoginUiState(loading = true)
            val r = MissAvAuth.login(login.trim(), password, remember)
            _login.value = LoginUiState(error = r.exceptionOrNull()?.message)
            if (r.isSuccess) onSuccess()
        }
    }

    fun clearLoginError() { _login.value = LoginUiState() }

    fun logout() {
        MissAvAuth.logout()
        history.clear(); saved.clear(); savedActresses.clear()
        playlists.value = PagedUiState()
    }

    val history = PagedLoader<HanimeInfo>(viewModelScope, { it.videoCode }) { MissAvAccountRepo.history(it) }
    val saved = PagedLoader<HanimeInfo>(viewModelScope, { it.videoCode }) { MissAvAccountRepo.saved(it) }
    val savedActresses = PagedLoader<MissAvActress>(viewModelScope, { it.path }) { MissAvAccountRepo.savedActresses(it) }

    val playlists = MutableStateFlow(PagedUiState<MissAvPlaylist>())

    fun loadPlaylists(force: Boolean = false) {
        val s = playlists.value
        if (s.loading || (s.loaded && !force)) return
        viewModelScope.launch {
            playlists.update { it.copy(loading = true, error = null) }
            try {
                val list = MissAvAccountRepo.playlists()
                playlists.value = PagedUiState(list, loaded = true, page = 1)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                playlists.update { it.copy(loading = false, loaded = true, error = e.message ?: e.toString()) }
            }
        }
    }

    private val _creating = MutableStateFlow(false)
    val creating: StateFlow<Boolean> = _creating.asStateFlow()
    private val _createError = MutableStateFlow<String?>(null)
    val createError: StateFlow<String?> = _createError.asStateFlow()

    fun createPlaylist(name: String, onDone: () -> Unit) {
        if (_creating.value) return
        viewModelScope.launch {
            _creating.value = true; _createError.value = null
            try {
                val list = MissAvAccountRepo.createPlaylist(name.trim())
                playlists.value = PagedUiState(list, loaded = true, page = 1)
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _createError.value = e.message ?: e.toString()
            } finally {
                _creating.value = false
            }
        }
    }

    fun clearHistory(onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                MissAvAccountRepo.clearHistory()
                history.clear()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(e.message ?: e.toString())
            }
        }
    }

    private val playlistLoaders = mutableMapOf<String, PagedLoader<HanimeInfo>>()
    fun playlistLoader(key: String) = playlistLoaders.getOrPut(key) {
        PagedLoader<HanimeInfo>(viewModelScope, { it.videoCode }) { p -> MissAvAccountRepo.playlistVideos(key, p) }
    }

    private val actressListLoaders = mutableMapOf<String, PagedLoader<MissAvActress>>()
    fun actressListLoader(path: String) = actressListLoaders.getOrPut(path) {
        PagedLoader<MissAvActress>(viewModelScope, { it.path }) { p -> MissAvAccountRepo.actresses(path, p) }
    }

    class ActressDetail(val loader: PagedLoader<HanimeInfo>) {
        val id = MutableStateFlow<String?>(null)
        val profile = MutableStateFlow<MissAvActressProfile?>(null)
        val saved = MutableStateFlow<Boolean?>(null)
        val busy = MutableStateFlow(false)
        lateinit var news: PagedLoader<MissAvTweet>
    }

    private val actressDetails = mutableMapOf<String, ActressDetail>()
    fun actressDetail(path: String): ActressDetail = actressDetails.getOrPut(path) {
        lateinit var detail: ActressDetail
        val loader = PagedLoader<HanimeInfo>(viewModelScope, { it.videoCode }) { p ->
            val r = MissAvAccountRepo.actressPage(path, p)
            if (detail.id.value == null && r.id != null) detail.id.value = r.id
            if (r.profile != null) detail.profile.value = r.profile
            r.videos
        }
        detail = ActressDetail(loader)
        var cursor: String? = null
        detail.news = PagedLoader<MissAvTweet>(viewModelScope, { it.id }) { p ->
            if (p == 1) cursor = null

            var id = detail.id.value
            if (id == null) {
                id = MissAvAccountRepo.actressPage(path, 1).also { r ->
                    r.id?.let { detail.id.value = it }
                    r.profile?.let { detail.profile.value = it }
                }.id
            }
            if (id == null) throw IllegalStateException("Actress id not found")
            val page = MissAvAccountRepo.actressTweets(id, cursor)
            cursor = page.nextCursor
            MissAvListPage(page.items, hasNext = page.nextCursor != null)
        }
        detail
    }

    fun refreshActressSaved(path: String) {
        val d = actressDetail(path)
        val id = d.id.value ?: return
        if (MissAvAuth.currentUser() == null) { d.saved.value = null; return }
        viewModelScope.launch {
            runCatching { MissAvAccountRepo.actressState(id) }
                .onSuccess { d.saved.value = it.saved }
        }
    }

    fun toggleActressSaved(path: String, onError: (String) -> Unit) {
        val d = actressDetail(path)
        val id = d.id.value ?: return
        val want = d.saved.value != true
        if (d.busy.value) return
        viewModelScope.launch {
            d.busy.value = true
            try {
                d.saved.value = MissAvAccountRepo.setActressSaved(id, want)
                savedActresses.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(e.message ?: e.toString())
            } finally {
                d.busy.value = false
            }
        }
    }
}
