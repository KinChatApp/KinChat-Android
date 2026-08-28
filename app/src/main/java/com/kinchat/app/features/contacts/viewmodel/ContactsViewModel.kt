package com.kinchat.app.features.contacts.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinchat.app.domain.model.UserContact
import com.kinchat.app.domain.repository.ChatRepository
import com.kinchat.app.domain.usecase.ContactsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactsUiState(
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val registeredContacts: List<UserContact> = emptyList(),
    val unregisteredContacts: List<UserContact> = emptyList(),
    val errorMsg: String? = null
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactsUseCases: ContactsUseCases,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    private val _resolvedChatId = MutableStateFlow<String?>(null)
    val resolvedChatId: StateFlow<String?> = _resolvedChatId.asStateFlow()

    init {
        loadContacts()
        observeContacts()
    }

    private fun observeContacts() {
        contactsUseCases.getContacts()
            .map { contacts ->
                val uniqueRegistered = contacts
                    .filter { it.registeredUserId != null }
                    .associateBy { it.registeredUserId }
                    .values.toList()

                val registeredPhones = uniqueRegistered.map { it.contactPhoneNormalized }.toSet()

                val unregistered = contacts
                    .filter { it.registeredUserId == null && it.contactPhoneNormalized !in registeredPhones }
                    .associateBy { it.contactPhoneNormalized }
                    .values.toList()

                // 🚀 FIX: অ্যালফাবেটিক্যালি সর্ট করা হচ্ছে (A-Z)
                val sortedRegistered = uniqueRegistered.sortedBy { it.contactName.trim().lowercase() }
                val sortedUnregistered = unregistered.sortedBy { it.contactName.trim().lowercase() }

                Pair(sortedRegistered, sortedUnregistered)
            }
            .flowOn(Dispatchers.Default)
            .onEach { (registered, unregistered) ->
                _uiState.update {
                    it.copy(registeredContacts = registered, unregisteredContacts = unregistered, isLoading = false)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            contactsUseCases.loadRemoteContacts()
        }
    }

    fun syncContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, errorMsg = null) }
            val result = contactsUseCases.syncDeviceContacts()

            if (result.isSuccess) {
                contactsUseCases.loadRemoteContacts()
            }

            _uiState.update { it.copy(isSyncing = false, errorMsg = result.errorMessage) }
        }
    }

    fun openChatWithUser(partnerUserId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMsg = null) }

            val result = chatRepository.createChatIfNotExists(partnerUserId)

            if (result.isSuccess) {
                _resolvedChatId.value = result.getOrNull()
            } else {
                _uiState.update { it.copy(errorMsg = result.exceptionOrNull()?.message ?: "Failed to open chat.") }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onChatNavigated() {
        _resolvedChatId.value = null
    }

    companion object {
        private const val TAG = "ContactsViewModel"
        private val NON_DIGIT_REGEX = Regex("\\D")
    }
}
