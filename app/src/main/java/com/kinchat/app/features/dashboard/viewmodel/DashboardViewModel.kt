package com.kinchat.app.features.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.core.utils.ContactResolver
import com.kinchat.app.domain.model.Chat
import com.kinchat.app.domain.repository.DashboardRepository
import com.kinchat.app.domain.repository.ChatRepository
import com.kinchat.app.domain.usecase.ContactsUseCases
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val allChats: List<Chat> = emptyList(),
    val chats: List<Chat> = emptyList(),
    val selectedFilter: String = "All",
    val selectedChatForMenu: Chat? = null,
    val showConfirmDeleteDialog: Boolean = false,
    val pendingDeleteChatId: String? = null,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: Lazy<DashboardRepository>,
    private val chatRepository: Lazy<ChatRepository>,
    private val contactsUseCases: Lazy<ContactsUseCases>
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        AppLogger.d("DataFlowLog", "DashboardViewModel: init block started")
        loadChats()
    }

    fun syncContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val syncResult = contactsUseCases.get().syncDeviceContacts()
                if (syncResult.isSuccess) {
                    contactsUseCases.get().loadRemoteContacts()
                }
            } catch (e: Exception) {
                AppLogger.d("DashboardViewModel", "Background sync skipped (likely no permissions yet)")
            }
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { state ->
            state.copy(
                selectedFilter = filter,
                chats = applyFilter(state.allChats, filter)
            )
        }
    }

    private fun applyFilter(chats: List<Chat>, filter: String): List<Chat> {
        return when (filter) {
            "Unread" -> chats.filter { it.unreadCount > 0 }
            "Favourites" -> chats.filter { it.isFavorite }
            "Saved Messages" -> chats.filter { it.isSaved }
            else -> chats
        }
    }

    private fun loadChats() {
        viewModelScope.launch(Dispatchers.Default) {
            AppLogger.d("DataFlowLog", "DashboardViewModel: Launching contactsFlow")
            val contactsFlow = contactsUseCases.get().getContacts()
                .onStart { 
                    AppLogger.d("DataFlowLog", "DashboardViewModel: contactsFlow onStart")
                    emit(emptyList()) 
                }
                .onEach { AppLogger.d("DataFlowLog", "DashboardViewModel: contactsFlow emitted \${it.size} contacts") }
                .catch {
                    AppLogger.e("DashboardViewModel", "Contacts flow error", it)
                    emit(emptyList())
                }

            AppLogger.d("DataFlowLog", "DashboardViewModel: Launching chatsFlow")
            val chatsFlow = dashboardRepository.get().getRecentChats()
                .onEach { AppLogger.d("DataFlowLog", "DashboardViewModel: chatsFlow emitted \${it.size} chats") }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                    AppLogger.e("DashboardViewModel", "Chat flow error", e)
                    emit(emptyList())
                }

            AppLogger.d("DataFlowLog", "DashboardViewModel: Starting combine block")
            combine(contactsFlow, chatsFlow) { contacts, chatList ->
                AppLogger.d("DataFlowLog", "DashboardViewModel: combine processing \${chatList.size} chats with \${contacts.size} contacts")
                chatList.map { chat ->
                    chat.copy(name = ContactResolver.resolveChatName(chat, contacts))
                }.sortedByDescending { it.isPinned }
            }.collect { finalChats ->
                AppLogger.d("DataFlowLog", "DashboardViewModel: collect block triggered. Final UI update.")
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        allChats = finalChats,
                        chats = applyFilter(finalChats, state.selectedFilter)
                    )
                }
            }
        }
    }

    fun openContextMenu(chat: Chat) { _uiState.update { it.copy(selectedChatForMenu = chat) } }
    fun closeContextMenu() { _uiState.update { it.copy(selectedChatForMenu = null) } }
    fun clearTransientUiState() { _uiState.update { it.copy(selectedChatForMenu = null, showConfirmDeleteDialog = false, pendingDeleteChatId = null) } }

    fun pinChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.get().updateChatPinStatus(chatId, !(_uiState.value.allChats.find { it.id == chatId }?.isPinned ?: false))
            closeContextMenu()
        }
    }

    fun favoriteChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.get().updateChatFavoriteStatus(chatId, !(_uiState.value.allChats.find { it.id == chatId }?.isFavorite ?: false))
            closeContextMenu()
        }
    }

    fun archiveChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.get().updateChatArchiveStatus(chatId, !(_uiState.value.allChats.find { it.id == chatId }?.isArchived ?: false))
            closeContextMenu()
        }
    }

    fun muteChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.get().updateChatMuteStatus(chatId, !(_uiState.value.allChats.find { it.id == chatId }?.isMuted ?: false))
            closeContextMenu()
        }
    }

    fun blockChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.get().updateChatBlockStatus(chatId, !(_uiState.value.allChats.find { it.id == chatId }?.isBlocked ?: false))
            closeContextMenu()
        }
    }

    fun requestDeleteChat(chatId: String) {
        _uiState.update { it.copy(showConfirmDeleteDialog = true, selectedChatForMenu = null, pendingDeleteChatId = chatId) }
    }

    fun cancelDeleteChat() {
        _uiState.update { it.copy(showConfirmDeleteDialog = false, pendingDeleteChatId = null) }
    }

    fun confirmDeleteChat() {
        val chatId = _uiState.value.pendingDeleteChatId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(showConfirmDeleteDialog = false, pendingDeleteChatId = null) }
            chatRepository.get().deleteChatParticipant(chatId)
        }
    }
}
