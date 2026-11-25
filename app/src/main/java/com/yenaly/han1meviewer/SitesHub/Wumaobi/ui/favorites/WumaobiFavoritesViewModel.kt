package com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.SitesHub.Wumaobi.data.model.WumaobiFavFolder
import com.yenaly.han1meviewer.SitesHub.Wumaobi.data.remote.WumaobiAuth
import com.yenaly.han1meviewer.SitesHub.Wumaobi.data.remote.WumaobiNotSignedInException
import com.yenaly.han1meviewer.SitesHub.Wumaobi.data.remote.WumaobiRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WumaobiFavoritesUiState(
    val folders: List<WumaobiFavFolder> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

class WumaobiFavoritesViewModel : ViewModel() {

    private val _state = MutableStateFlow(WumaobiFavoritesUiState())
    val state: StateFlow<WumaobiFavoritesUiState> = _state.asStateFlow()

    fun load() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            WumaobiRepo.favFolders()
                .onSuccess { _state.value = WumaobiFavoritesUiState(folders = it) }
                .onFailure { e ->
                    if (e is WumaobiNotSignedInException) WumaobiAuth.onSessionRejected()
                    _state.value = WumaobiFavoritesUiState(error = e.message ?: e.javaClass.simpleName)
                }
        }
    }

    fun create(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            WumaobiRepo.createFavFolder(clean)
                .onSuccess { load() }
                .onFailure { e ->
                    _state.value = _state.value.copy(message = e.message ?: e.javaClass.simpleName)
                }
        }
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
