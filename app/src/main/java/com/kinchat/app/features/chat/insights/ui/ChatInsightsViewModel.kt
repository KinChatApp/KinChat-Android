package com.kinchat.app.features.chat.insights.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinchat.app.features.chat.insights.domain.model.ChatInsights
import com.kinchat.app.features.chat.insights.domain.repository.ChatInsightsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ChatInsightsUiState {
    object Loading : ChatInsightsUiState()
    data class Success(val data: ChatInsights) : ChatInsightsUiState()
    data class Error(val message: String) : ChatInsightsUiState()
    object Empty : ChatInsightsUiState()
}

@HiltViewModel
class ChatInsightsViewModel @Inject constructor(
    private val repository: ChatInsightsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatInsightsUiState>(ChatInsightsUiState.Loading)
    val uiState: StateFlow<ChatInsightsUiState> = _uiState.asStateFlow()

    init {
        val userId = savedStateHandle.get<String>("userId")
        if (userId != null) {
            observeLocalInsights(userId)
            refreshInsightsFromServer(userId)
        } else {
            _uiState.value = ChatInsightsUiState.Error("User ID not provided")
        }
    }

    private fun observeLocalInsights(friendId: String) {
        viewModelScope.launch {
            repository.getChatInsightsFlow(friendId).collect { insights ->
                if (insights != null) {
                    if (insights.totalMessages == 0) {
                        _uiState.value = ChatInsightsUiState.Empty
                    } else {
                        _uiState.value = ChatInsightsUiState.Success(insights)
                    }
                }
                // If null, keep the Loading state until refresh finishes
            }
        }
    }

    private fun refreshInsightsFromServer(friendId: String) {
        viewModelScope.launch {
            val result = repository.refreshChatInsights(friendId)
            if (result.isFailure && _uiState.value is ChatInsightsUiState.Loading) {
                // Only show error if we don't even have local data
                _uiState.value = ChatInsightsUiState.Error(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to load insights"
                )
            }
        }
    }
}
