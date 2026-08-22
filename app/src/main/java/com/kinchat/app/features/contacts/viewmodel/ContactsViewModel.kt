package com.kinchat.app.features.contacts.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinchat.app.domain.model.UserContact
import com.kinchat.app.domain.usecase.ContactsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
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
    private val supabaseClient: SupabaseClient
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
                    .associateBy { contact ->
                        val phoneDigits = contact.contactPhoneNormalized.replace(NON_DIGIT_REGEX, "")
                        if (phoneDigits.length >= 10) phoneDigits.takeLast(10) else contact.registeredUserId ?: contact.id
                    }.values.toList()

                val unregistered = contacts.filter { it.registeredUserId == null }
                Pair(uniqueRegistered, unregistered)
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
            
            // লোকাল সিঙ্ক সফল হলে সার্ভার থেকেও রেজিস্টার্ড ইউজার চেক করে আপডেট করুন
            if (result.isSuccess) {
                contactsUseCases.loadRemoteContacts()
            }
            
            _uiState.update { it.copy(isSyncing = false, errorMsg = result.errorMessage) }
        }
    }

    fun openChatWithUser(partnerUserId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMsg = null) }
            try {
                val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                if (currentUserId != null) {
                    val response = supabaseClient.postgrest.rpc(
                        "create_chat_if_not_exists",
                        mapOf("user1_id" to currentUserId, "user2_id" to partnerUserId)
                    ).decodeAsOrNull<String>()

                    val chatId = response?.replace("\"", "")
                    if (!chatId.isNullOrBlank()) {
                        _resolvedChatId.value = chatId
                    } else {
                        _uiState.update { it.copy(errorMsg = "Failed to create chat room.") }
                    }
                } else {
                    _uiState.update { it.copy(errorMsg = "User not authenticated.") }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resolving chat ID", e)
                _uiState.update { it.copy(errorMsg = "Network error: ${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
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
