package com.kinchat.app.features.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinchat.app.domain.model.Chat
import com.kinchat.app.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val chats: List<Chat> = emptyList(),
    val selectedChatForMenu: Chat? = null,
    val showConfirmDeleteDialog: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadChats()
    }

    private fun loadChats() {
        viewModelScope.launch {
            repository.getRecentChats()
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { chatList ->
                    _uiState.update { it.copy(isLoading = false, chats = chatList) }
                }
        }
    }

    fun openContextMenu(chat: Chat) {
        _uiState.update { it.copy(selectedChatForMenu = chat) }
    }

    fun closeContextMenu() {
        _uiState.update { it.copy(selectedChatForMenu = null) }
    }

    fun requestDeleteChat() {
        _uiState.update { it.copy(showConfirmDeleteDialog = true, selectedChatForMenu = null) }
    }

    fun cancelDeleteChat() {
        _uiState.update { it.copy(showConfirmDeleteDialog = false) }
    }

    fun confirmDeleteChat(chatId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(showConfirmDeleteDialog = false) }
            val result = repository.deleteChat(chatId)
            if (result.isSuccess) {
                // Remove from local state optimistically
                _uiState.update { currentState ->
                    currentState.copy(chats = currentState.chats.filter { it.id != chatId })
                }
            } else {
                _uiState.update { it.copy(error = "Failed to delete chat") }
            }
        }
    }
}
