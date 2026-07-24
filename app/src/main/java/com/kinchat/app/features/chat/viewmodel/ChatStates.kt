package com.kinchat.app.features.chat.viewmodel

sealed interface PartnerUiState {
    data object Loading : PartnerUiState
    data class Success(val id: String, val name: String) : PartnerUiState
    data object Error : PartnerUiState
}

sealed class SendMessageResult {
    data object Success : SendMessageResult()
    data class Failure(val reason: String) : SendMessageResult()
}
