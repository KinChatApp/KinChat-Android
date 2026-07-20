package com.tuktak.app.features.chat.info.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuktak.app.features.chat.info.domain.model.UserProfile
import com.tuktak.app.features.chat.info.domain.repository.ChatInfoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatInfoUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val chatId: String? = null,
    val isMuted: Boolean = false,
    val isBlocked: Boolean = false,
    val mediaCount: Int = 0,
    val actionLoading: Boolean = false,
    val error: String? = null,
    val messageCleared: Boolean = false
)

@HiltViewModel
class ChatInfoViewModel @Inject constructor(
    private val repository: ChatInfoRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val userId: String = checkNotNull(savedStateHandle["userId"])

    private val _uiState = MutableStateFlow(ChatInfoUiState())
    val uiState: StateFlow<ChatInfoUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
        loadSettings()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            repository.getUserProfile(userId)
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { profileData ->
                    _uiState.update { it.copy(profile = profileData, isLoading = false) }
                }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            repository.getChatSettings(userId)
                .onSuccess { settings ->
                    _uiState.update {
                        it.copy(
                            chatId = settings.chatId,
                            isMuted = settings.isMuted,
                            isBlocked = settings.isBlocked,
                            mediaCount = settings.mediaCount
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun toggleMute() {
        val chatId = _uiState.value.chatId ?: return
        val newStatus = !_uiState.value.isMuted
        
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true) }
            repository.toggleMute(chatId, newStatus)
                .onSuccess {
                    _uiState.update { it.copy(isMuted = newStatus, actionLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(actionLoading = false, error = e.message) }
                }
        }
    }

    fun toggleBlock() {
        val newStatus = !_uiState.value.isBlocked
        
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true) }
            repository.toggleBlock(userId, newStatus)
                .onSuccess {
                    _uiState.update { it.copy(isBlocked = newStatus, actionLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(actionLoading = false, error = e.message) }
                }
        }
    }

    fun clearChat() {
        val chatId = _uiState.value.chatId ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true) }
            repository.clearChat(chatId)
                .onSuccess {
                    _uiState.update { it.copy(actionLoading = false, messageCleared = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(actionLoading = false, error = e.message) }
                }
        }
    }

    fun reportUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true) }
            repository.reportUser(userId)
                .onSuccess {
                    _uiState.update { it.copy(actionLoading = false, error = "Report sent successfully") }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(actionLoading = false, error = e.message) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
