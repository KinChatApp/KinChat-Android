package com.kinchat.app.features.chat.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinchat.app.domain.model.ChatMessage
import com.kinchat.app.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val chatSetupUseCase: ChatSetupUseCase
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _partnerState = MutableStateFlow<PartnerUiState>(PartnerUiState.Loading)
    val partnerState: StateFlow<PartnerUiState> = _partnerState.asStateFlow()

    private val _isPartnerTyping = MutableStateFlow(false)
    val isPartnerTyping = _isPartnerTyping.asStateFlow()

    private val _isPartnerOnline = MutableStateFlow(false)
    val isPartnerOnline = _isPartnerOnline.asStateFlow()

    private val _selectedMessages = MutableStateFlow<Set<String>>(emptySet())
    val selectedMessages = _selectedMessages.asStateFlow()

    private var currentChatId: String? = null
    var currentUserId: String = ""
        private set
        
    private var chatObservingJob: Job? = null

    fun toggleSelection(messageId: String) {
        _selectedMessages.value = _selectedMessages.value.toMutableSet().apply {
            if (contains(messageId)) remove(messageId) else add(messageId)
        }
    }

    fun clearSelection() {
        _selectedMessages.value = emptySet()
    }

    fun initializeChat(passedId: String) {
        chatObservingJob?.cancel()

        chatObservingJob = viewModelScope.launch {
            val quickName = chatRepository.getPartnerName(passedId, "")
            if (!quickName.isNullOrBlank()) {
                _partnerState.value = PartnerUiState.Success(id = passedId, name = quickName)
            } else {
                _partnerState.value = PartnerUiState.Loading
            }
            _messages.value = emptyList()

            // 🚀 চ্যাট সেটআপের জটিল লজিক UseCase এর মাধ্যমে হ্যান্ডেল করা হচ্ছে
            val setupResult = chatSetupUseCase.execute(passedId, quickName)

            if (setupResult != null) {
                currentUserId = setupResult.currentUserId
                currentChatId = setupResult.actualChatId

                if (setupResult.partnerName != null) {
                    _partnerState.value = PartnerUiState.Success(
                        id = setupResult.partnerId.ifEmpty { setupResult.actualChatId },
                        name = setupResult.partnerName
                    )
                } else {
                    _partnerState.value = PartnerUiState.Error
                }

                try {
                    chatRepository.observeMessages(setupResult.actualChatId).collectLatest { msgs ->
                        _messages.value = msgs.distinctBy { it.id }.filter { msg ->
                            msg.deletedForUsers?.contains(currentUserId) != true
                        }
                        if (currentUserId.isNotEmpty()) {
                            chatRepository.updateLastRead(setupResult.actualChatId, currentUserId)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ChatDebug", "Error observing messages: ${e.message}")
                }
            } else if (quickName.isNullOrBlank()) {
                _partnerState.value = PartnerUiState.Error
            }
        }
    }

    suspend fun sendMessage(content: String, replyToId: String? = null): SendMessageResult {
        val chatId = currentChatId
            ?: return SendMessageResult.Failure("চ্যাট এখনো লোড হয়নি, একটু অপেক্ষা করে আবার চেষ্টা করুন")

        if (currentUserId.isEmpty()) {
            return SendMessageResult.Failure("ইউজার লগইন স্ট্যাটাস পাওয়া যায়নি, একটু অপেক্ষা করে আবার চেষ্টা করুন")
        }

        val result = chatRepository.sendMessage(chatId, currentUserId, content, replyToId)

        return if (result.isSuccess) {
            SendMessageResult.Success
        } else {
            val rawMessage = result.exceptionOrNull()?.message ?: "অজানা সমস্যা"
            val cleanMessage = rawMessage.substringBefore("URL:").trim().ifBlank {
                rawMessage.take(300)
            }
            Log.e("ChatDebug", "sendMessage failed: $rawMessage", result.exceptionOrNull())
            SendMessageResult.Failure(cleanMessage)
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch {
            try { chatRepository.editMessage(messageId, newContent) }
            catch (e: Exception) { Log.e("ChatDebug", "Error editing: ${e.message}") }
        }
    }

    fun toggleSaveMessage(messageId: String) {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch { chatRepository.toggleSaveMessage(messageId, currentUserId) }
    }

    fun deleteSelectedMessages(type: String = "for_me") {
        if (currentUserId.isEmpty() || _selectedMessages.value.isEmpty()) return
        viewModelScope.launch {
            _selectedMessages.value.forEach { msgId ->
                chatRepository.deleteMessage(msgId, currentUserId, type)
            }
            clearSelection()
        }
    }

    fun addReaction(messageId: String, reactionType: String) {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch { chatRepository.addReaction(messageId, currentUserId, reactionType) }
    }

    fun updateTypingStatus(isTyping: Boolean) {}
}
