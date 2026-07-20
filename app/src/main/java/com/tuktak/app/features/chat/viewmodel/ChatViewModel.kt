package com.tuktak.app.features.chat.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuktak.app.domain.model.ChatMessage
import com.tuktak.app.domain.repository.ChatRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

// 🚀 FIX: Partner ID ডাটাবেজ থেকে রিসিভ করার জন্য একটি ছোট DTO ক্লাস
@Serializable
private data class ParticipantDto(val user_id: String)

sealed interface PartnerUiState {
    data object Loading : PartnerUiState
    // 🚀 FIX: এখানে `id` প্রপার্টি যুক্ত করা হয়েছে
    data class Success(val id: String, val name: String) : PartnerUiState
    data object Error : PartnerUiState
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _partnerState = MutableStateFlow<PartnerUiState>(PartnerUiState.Loading)
    val partnerState: StateFlow<PartnerUiState> = _partnerState.asStateFlow()

    private val _isPartnerTyping = MutableStateFlow(false)
    val isPartnerTyping = _isPartnerTyping.asStateFlow()

    private val _isPartnerOnline = MutableStateFlow(false)
    val isPartnerOnline = _isPartnerOnline.asStateFlow()

    private var currentChatId: String? = null
    private var chatObservingJob: Job? = null
    var currentUserId: String = ""
        private set

    fun initializeChat(chatId: String) {
        currentChatId = chatId
        chatObservingJob?.cancel()

        chatObservingJob = viewModelScope.launch {
            _partnerState.value = PartnerUiState.Loading
            _messages.value = emptyList()

            var user = supabaseClient.auth.currentUserOrNull()
            if (user == null) {
                delay(500)
                user = supabaseClient.auth.currentUserOrNull()
            }

            currentUserId = user?.id ?: ""

            if (currentUserId.isNotEmpty() && chatId.isNotEmpty()) {
                val name = chatRepository.getPartnerName(chatId, currentUserId)
                
                // 🚀 FIX: ডাটাবেজ থেকে Partner-এর আসল User ID (partnerId) বের করা হচ্ছে
                var partnerId = ""
                try {
                    val participant = supabaseClient.postgrest["chat_participants"]
                        .select {
                            filter {
                                eq("chat_id", chatId)
                                neq("user_id", currentUserId)
                            }
                        }.decodeList<ParticipantDto>().firstOrNull() // firstOrNull() ব্যবহার করা হলো যাতে Group Chat হলেও ক্র্যাশ না করে
                    
                    partnerId = participant?.user_id ?: ""
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Error fetching partner ID", e)
                }

                if (name != null) {
                    // 🚀 FIX: Partner ID এবং Name একসাথে স্টেটে সেভ করা হচ্ছে
                    _partnerState.value = PartnerUiState.Success(
                        id = partnerId.ifEmpty { chatId }, // Fallback হিসেবে chatId
                        name = name
                    )
                } else {
                    _partnerState.value = PartnerUiState.Error
                }
            } else {
                _partnerState.value = PartnerUiState.Error
            }

            try {
                chatRepository.observeMessages(chatId).collectLatest { msgs ->
                    _messages.value = msgs.distinctBy { it.id }.filter { msg ->
                        msg.deletedForUsers?.contains(currentUserId) != true
                    }
                    if (currentUserId.isNotEmpty()) {
                        chatRepository.updateLastRead(chatId, currentUserId)
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error fetching messages", e)
            }
        }
    }

    fun sendMessage(content: String, replyToId: String? = null) {
        Log.d("ChatDebug", "Send button clicked! Message: $content")

        val chatId = currentChatId
        if (chatId == null) {
            Log.e("ChatDebug", "Error: currentChatId is null! (Chat not initialized properly)")
            return
        }

        if (currentUserId.isEmpty()) {
            Log.e("ChatDebug", "Error: currentUserId is empty! (User not authenticated or loaded)")
            return
        }

        viewModelScope.launch {
            Log.d("ChatDebug", "Inserting message to database...")
            val result = chatRepository.sendMessage(chatId, currentUserId, content, replyToId)

            if (result.isSuccess) {
                Log.d("ChatDebug", "Message successfully inserted to database!")
            } else if (result.isFailure) {
                val error = result.exceptionOrNull()
                Log.e("ChatDebug", "Message send failed: ${error?.message}", error)
            }
        }
    }

    fun toggleSaveMessage(messageId: String) {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch { chatRepository.toggleSaveMessage(messageId, currentUserId) }
    }

    fun deleteMessage(messageId: String, type: String = "for_me") {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch { chatRepository.deleteMessage(messageId, currentUserId, type) }
    }

    fun addReaction(messageId: String, reactionType: String) {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch { chatRepository.addReaction(messageId, currentUserId, reactionType) }
    }

    fun updateTypingStatus(isTyping: Boolean) { /* Presence logic */ }
}
