package com.kinchat.app.features.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinchat.app.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HeaderUiState(
    val avatarUrl: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class HeaderViewModel @Inject constructor(
    private val repository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HeaderUiState())
    val uiState: StateFlow<HeaderUiState> = _uiState.asStateFlow()

    init {
        fetchUserProfile()
    }

    private fun fetchUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = repository.getCurrentUserId()
            
            if (userId != null) {
                // 🚀 FIX V5: Flow observe করা হচ্ছে
                repository.observeUserProfile(userId).collect { profile ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            avatarUrl = profile?.avatarUrl
                        ) 
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
