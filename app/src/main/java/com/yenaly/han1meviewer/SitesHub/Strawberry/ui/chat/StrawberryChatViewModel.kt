package com.yenaly.han1meviewer.SitesHub.Strawberry.ui.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.model.StrawberryChatMessage
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote.StrawberryChatRepo
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.util.applicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StrawberryChatUiState(
    val messages: List<StrawberryChatMessage> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSending: Boolean = false,
    val sendError: String? = null,
    val loadError: String? = null,
    val hasCsrfToken: Boolean = false,
    val lastUpdatedAt: Long = 0L,
)

class StrawberryChatViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "StrawberryChatViewModel"
    }

    private val _uiState = MutableStateFlow(StrawberryChatUiState())
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var sendJob: Job? = null

    init {
        refresh(initial = true)
    }

    fun refresh(initial: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = initial || it.messages.isEmpty(),
                    isRefreshing = !initial && it.messages.isNotEmpty(),
                    loadError = null,
                )
            }
            val result = StrawberryChatRepo.fetchChatPage(1)
            result.onSuccess { page ->
                _uiState.update {
                    it.copy(
                        messages = page.messages,
                        isLoading = false,
                        isRefreshing = false,
                        hasCsrfToken = page.csrfToken.isNotBlank(),
                        loadError = page.formError,
                        lastUpdatedAt = System.currentTimeMillis(),
                    )
                }
            }.onFailure { e ->
                Log.e(TAG, "refresh failed", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        loadError = e.message ?: applicationContext.getString(R.string.strawberry_failed_to_load_chat),
                    )
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        if (_uiState.value.isSending) return
        sendJob?.cancel()
        sendJob = viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, sendError = null) }
            when (val result = StrawberryChatRepo.postChatMessage(trimmed)) {
                is StrawberryChatRepo.ChatPostResult.Success -> {
                    _uiState.update { it.copy(isSending = false, sendError = null) }
                    refresh(initial = false)
                }
                is StrawberryChatRepo.ChatPostResult.FormError -> {
                    _uiState.update { it.copy(isSending = false, sendError = result.message) }
                }
                is StrawberryChatRepo.ChatPostResult.LoginRequired -> {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            sendError = applicationContext.getString(R.string.strawberry_your_session_expired_sign_in_again),
                        )
                    }
                }
                is StrawberryChatRepo.ChatPostResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            sendError = result.throwable.message ?: applicationContext.getString(R.string.strawberry_network_error),
                        )
                    }
                }
            }
        }
    }

    fun clearSendError() {
        _uiState.update { it.copy(sendError = null) }
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
        sendJob?.cancel()
    }
}
