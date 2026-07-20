package com.tuktak.app.features.contacts.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuktak.app.domain.model.UserContact
import com.tuktak.app.domain.usecase.ContactsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    private val supabaseClient: SupabaseClient // 🚀 Supabase অ্যাড করা হলো
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    // 🚀 চ্যাট আইডি রিসলভ করার স্টেট
    private val _resolvedChatId = MutableStateFlow<String?>(null)
    val resolvedChatId: StateFlow<String?> = _resolvedChatId.asStateFlow()

    init {
        loadContacts()
        observeContacts()
    }

    private fun observeContacts() {
        viewModelScope.launch {
            contactsUseCases.getContacts().collect { contacts ->
                val uniqueRegistered = contacts
                    .filter { it.registeredUserId != null }
                    .associateBy { contact ->
                        val phoneDigits = contact.contactPhoneNormalized.replace(Regex("\\D"), "")
                        if (phoneDigits.length >= 10) phoneDigits.takeLast(10) else contact.registeredUserId ?: contact.id
                    }
                    .values
                    .toList()

                val unregistered = contacts.filter { it.registeredUserId == null }

                _uiState.update {
                    it.copy(
                        registeredContacts = uniqueRegistered,
                        unregisteredContacts = unregistered,
                        isLoading = false
                    )
                }
            }
        }
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
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    errorMsg = result.errorMessage
                )
            }
        }
    }

    // 🚀 ইউজারের নামের ওপর ক্লিক করলে চ্যাট আইডি তৈরি বা ফেচ করা হবে
    fun openChatWithUser(partnerUserId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) } // লোডিং দেখাবো
            try {
                val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                if (currentUserId != null) {
                    val response = supabaseClient.postgrest.rpc(
                        "create_chat_if_not_exists",
                        mapOf("user1_id" to currentUserId, "user2_id" to partnerUserId)
                    ).decodeAsOrNull<String>()

                    val chatId = response?.replace("\"", "") // কোটেশন রিমুভ
                    if (chatId != null) {
                        _resolvedChatId.value = chatId // সাকসেস হলে নেভিগেট হবে
                    } else {
                        _uiState.update { it.copy(errorMsg = "Failed to create chat room.") }
                    }
                }
            } catch (e: Exception) {
                Log.e("ContactsVM", "Error resolving chat ID", e)
                _uiState.update { it.copy(errorMsg = "Network error: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onChatNavigated() {
        _resolvedChatId.value = null
    }
}
