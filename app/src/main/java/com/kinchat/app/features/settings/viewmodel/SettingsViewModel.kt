package com.kinchat.app.features.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinchat.app.domain.model.UserSettings
import com.kinchat.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = true,
    val settings: UserSettings = UserSettings(),
    val errorMsg: String? = null,
    val isLoggedOut: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMsg = null) }
            val result = repository.getUserSettings()
            result.onSuccess { settings ->
                _uiState.update { it.copy(isLoading = false, settings = settings) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMsg = "Failed to load settings: ${e.message}") }
            }
        }
    }

    fun updateTheme(newTheme: String) {
        val previousSettings = _uiState.value.settings
        // Optimistic update
        _uiState.update { it.copy(settings = it.settings.copy(theme = newTheme), errorMsg = null) }
        
        viewModelScope.launch {
            val result = repository.updateSetting("theme", newTheme)
            if (result.isFailure) {
                // Revert on failure
                _uiState.update { 
                    it.copy(
                        settings = previousSettings, 
                        errorMsg = "Failed to update theme."
                    ) 
                }
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        val previousSettings = _uiState.value.settings
        _uiState.update { it.copy(settings = it.settings.copy(notificationsEnabled = enabled), errorMsg = null) }
        
        viewModelScope.launch {
            val result = repository.updateSetting("notifications_enabled", enabled)
            if (result.isFailure) {
                _uiState.update { 
                    it.copy(
                        settings = previousSettings, 
                        errorMsg = "Failed to update notifications."
                    ) 
                }
            }
        }
    }

    fun toggleReadReceipts(enabled: Boolean) {
        val previousSettings = _uiState.value.settings
        _uiState.update { it.copy(settings = it.settings.copy(readReceiptsEnabled = enabled), errorMsg = null) }
        
        viewModelScope.launch {
            val result = repository.updateSetting("read_receipts_enabled", enabled)
            if (result.isFailure) {
                _uiState.update { 
                    it.copy(
                        settings = previousSettings, 
                        errorMsg = "Failed to update read receipts."
                    ) 
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMsg = null) }
            val result = repository.logout()
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isLoggedOut = true) }
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMsg = "Logout failed. Please try again."
                    ) 
                }
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMsg = null) }
            val result = repository.deleteAccount()
            if (result.isFailure) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMsg = result.exceptionOrNull()?.message ?: "Failed to delete account."
                    ) 
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMsg = null) }
    }
}
