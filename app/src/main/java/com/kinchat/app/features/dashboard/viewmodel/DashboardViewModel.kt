package com.kinchat.app.features.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinchat.app.domain.model.Chat
import com.kinchat.app.domain.repository.DashboardRepository
import com.kinchat.app.domain.repository.ChatRepository
import com.kinchat.app.domain.usecase.ContactsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val chats: List<Chat> = emptyList(),
    val selectedChatForMenu: Chat? = null,
    val showConfirmDeleteDialog: Boolean = false,
    val pendingDeleteChatId: String? = null,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val chatRepository: ChatRepository,
    private val contactsUseCases: ContactsUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadChats()
    }

    private fun loadChats() {
        viewModelScope.launch {
            // 🚀 Non-blocking Combine: কন্টাক্ট লোড হতে দেরি হলেও যেন চ্যাট লিস্ট আটকে না থাকে
            combine(
                dashboardRepository.getRecentChats(),
                contactsUseCases.getContacts().onStart { emit(emptyList()) }
            ) { chatList, contacts ->
                chatList.map { chat ->
                    val contactName = contacts.find { it.registeredUserId == chat.partnerId }?.contactName
                    if (contactName != null) {
                        chat.copy(name = contactName)
                    } else {
                        chat
                    }
                }.sortedByDescending { it.isPinned }
            }
            .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            .collect { finalChats ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        chats = finalChats
                    )
                }
            }
        }
    }

    fun openContextMenu(chat: Chat) {
        _uiState.update { it.copy(selectedChatForMenu = chat) }
    }

    fun closeContextMenu() {
        _uiState.update { it.copy(selectedChatForMenu = null) }
    }

    fun clearTransientUiState() {
        _uiState.update {
            it.copy(
                selectedChatForMenu = null,
                showConfirmDeleteDialog = false,
                pendingDeleteChatId = null
            )
        }
    }

    fun pinChat(chatId: String) {
        viewModelScope.launch {
            val chat = _uiState.value.chats.find { it.id == chatId } ?: return@launch
            val newStatus = !chat.isPinned

            _uiState.update { state ->
                val updatedChats = state.chats.map {
                    if (it.id == chatId) it.copy(isPinned = newStatus) else it
                }
                state.copy(
                    chats = updatedChats.sortedByDescending { c -> c.isPinned },
                    selectedChatForMenu = null
                )
            }
            chatRepository.updateChatPinStatus(chatId, newStatus)
        }
    }

    fun favoriteChat(chatId: String) {
        viewModelScope.launch {
            val chat = _uiState.value.chats.find { it.id == chatId } ?: return@launch
            val newStatus = !chat.isFavorite

            _uiState.update { state ->
                state.copy(
                    chats = state.chats.map { if (it.id == chatId) it.copy(isFavorite = newStatus) else it },
                    selectedChatForMenu = null
                )
            }
            chatRepository.updateChatFavoriteStatus(chatId, newStatus)
        }
    }

    fun archiveChat(chatId: String) {
        viewModelScope.launch {
            val chat = _uiState.value.chats.find { it.id == chatId } ?: return@launch
            val newStatus = !chat.isArchived

            _uiState.update { state ->
                state.copy(
                    chats = state.chats.map { if (it.id == chatId) it.copy(isArchived = newStatus) else it },
                    selectedChatForMenu = null
                )
            }
            chatRepository.updateChatArchiveStatus(chatId, newStatus)
        }
    }

    fun muteChat(chatId: String) {
        viewModelScope.launch {
            val chat = _uiState.value.chats.find { it.id == chatId } ?: return@launch
            val newStatus = !chat.isMuted

            _uiState.update { state ->
                state.copy(
                    chats = state.chats.map { if (it.id == chatId) it.copy(isMuted = newStatus) else it },
                    selectedChatForMenu = null
                )
            }
            chatRepository.updateChatMuteStatus(chatId, newStatus)
        }
    }

    fun blockChat(chatId: String) {
        viewModelScope.launch {
            val chat = _uiState.value.chats.find { it.id == chatId } ?: return@launch
            val newStatus = !chat.isBlocked

            _uiState.update { state ->
                state.copy(
                    chats = state.chats.map { if (it.id == chatId) it.copy(isBlocked = newStatus) else it },
                    selectedChatForMenu = null
                )
            }
            chatRepository.updateChatBlockStatus(chatId, newStatus)
        }
    }

    fun requestDeleteChat(chatId: String) {
        _uiState.update {
            it.copy(
                showConfirmDeleteDialog = true,
                selectedChatForMenu = null,
                pendingDeleteChatId = chatId
            )
        }
    }

    fun cancelDeleteChat() {
        _uiState.update { it.copy(showConfirmDeleteDialog = false, pendingDeleteChatId = null) }
    }

    fun confirmDeleteChat() {
        val chatId = _uiState.value.pendingDeleteChatId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(showConfirmDeleteDialog = false, pendingDeleteChatId = null) }
            val result = chatRepository.deleteChatParticipant(chatId)
            if (result.isSuccess) {
                _uiState.update { currentState ->
                    currentState.copy(chats = currentState.chats.filter { it.id != chatId })
                }
            } else {
                _uiState.update { it.copy(error = "Failed to delete chat") }
            }
        }
    }
}
