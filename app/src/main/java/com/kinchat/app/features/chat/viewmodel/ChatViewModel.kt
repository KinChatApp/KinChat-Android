package com.kinchat.app.features.chat.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinchat.app.domain.model.ChatMessage
import com.kinchat.app.domain.repository.ChatRepository
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
import java.util.UUID
import javax.inject.Inject

@Serializable
private data class ParticipantDto(val chat_id: String? = null, val user_id: String)

@Serializable
private data class ChatDto(val id: String)

sealed interface PartnerUiState {
    data object Loading : PartnerUiState
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

    private val AI_BOT_ID = "de438bb4-d954-4c31-9ad1-9dd34b85d981"

    fun initializeChat(passedId: String) {
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

            if (currentUserId.isNotEmpty() && passedId.isNotEmpty()) {
                var actualChatId = passedId
                var partnerName = chatRepository.getPartnerName(actualChatId, currentUserId)
                var partnerId = ""

                if (passedId == AI_BOT_ID || partnerName == null) {
                    try {
                        Log.d("ChatDebug", "Initializing Chat Room setup...")

                        if (passedId == AI_BOT_ID) {
                            partnerId = AI_BOT_ID
                            partnerName = "TukTak AI"
                        }

                        val myChats = supabaseClient.postgrest["chat_participants"]
                            .select { filter { eq("user_id", currentUserId) } }
                            .decodeList<ParticipantDto>().mapNotNull { it.chat_id }

                        val partnerChats = supabaseClient.postgrest["chat_participants"]
                            .select { filter { eq("user_id", partnerId) } }
                            .decodeList<ParticipantDto>().mapNotNull { it.chat_id }

                        val sharedChatId = myChats.intersect(partnerChats.toSet()).firstOrNull()

                        if (sharedChatId != null) {
                            actualChatId = sharedChatId
                            Log.d("ChatDebug", "Found existing chat room: $actualChatId")
                        } else {
                            Log.d("ChatDebug", "No chat room found. Creating a new one...")
                            val newChatId = UUID.randomUUID().toString()

                            supabaseClient.postgrest["chats"].insert(ChatDto(id = newChatId))
                            supabaseClient.postgrest["chat_participants"].insert(listOf(
                                ParticipantDto(chat_id = newChatId, user_id = currentUserId),
                                ParticipantDto(chat_id = newChatId, user_id = partnerId)
                            ))

                            actualChatId = newChatId
                            Log.d("ChatDebug", "Successfully created new chat room: $actualChatId")
                        }
                    } catch (e: Exception) {
                        Log.e("ChatDebug", "Error in Chat Setup: ${e.message}", e)
                        if (passedId == AI_BOT_ID) {
                            partnerName = "TukTak AI"
                        }
                    }
                } else {
                    try {
                        val participant = supabaseClient.postgrest["chat_participants"]
                            .select {
                                filter {
                                    eq("chat_id", actualChatId)
                                    neq("user_id", currentUserId)
                                }
                            }.decodeList<ParticipantDto>().firstOrNull()

                        partnerId = participant?.user_id ?: ""
                    } catch (e: Exception) {
                        Log.e("ChatDebug", "Error fetching partner ID", e)
                    }
                }

                currentChatId = actualChatId

                if (partnerName != null) {
                    _partnerState.value = PartnerUiState.Success(
                        id = partnerId.ifEmpty { actualChatId },
                        name = partnerName ?: "TukTak AI"
                    )
                } else {
                    _partnerState.value = PartnerUiState.Error
                }

                try {
                    // 🚀 এই অংশটি এখন সরাসরি Room Database (Local Cache) থেকে ফ্লো রিড করবে
                    chatRepository.observeMessages(actualChatId).collectLatest { msgs ->
                        _messages.value = msgs.distinctBy { it.id }.filter { msg ->
                            msg.deletedForUsers?.contains(currentUserId) != true
                        }
                        if (currentUserId.isNotEmpty()) {
                            chatRepository.updateLastRead(actualChatId, currentUserId)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ChatDebug", "Error fetching messages", e)
                }

            } else {
                _partnerState.value = PartnerUiState.Error
            }
        }
    }

    fun sendMessage(content: String, replyToId: String? = null): Boolean {
        Log.d("ChatDebug", "Send button clicked! Message: $content")

        val chatId = currentChatId
        if (chatId == null) {
            Log.e("ChatDebug", "Error: currentChatId is null! Chat setup is incomplete.")
            return false 
        }

        if (currentUserId.isEmpty()) {
            Log.e("ChatDebug", "Error: currentUserId is empty!")
            return false
        }

        viewModelScope.launch {
            val result = chatRepository.sendMessage(chatId, currentUserId, content, replyToId)
            if (result.isFailure) {
                val error = result.exceptionOrNull()
                Log.e("ChatDebug", "Message send failed: ${error?.message}", error)
            }
        }
        return true 
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

    fun updateTypingStatus(isTyping: Boolean) {}
}
