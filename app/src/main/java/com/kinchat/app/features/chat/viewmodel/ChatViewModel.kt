package com.kinchat.app.features.chat.viewmodel

import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.core.notifications.builder.NotificationSummaryManager
import com.kinchat.app.domain.model.ChatMessage
import com.kinchat.app.domain.repository.AuthRepository
import com.kinchat.app.domain.repository.ChatRepository
import com.kinchat.app.data.repository.chat.sync.ChatSyncManager
import com.kinchat.app.domain.usecase.ContactsUseCases
import com.kinchat.app.domain.usecase.ChatSetupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    private val chatSetupUseCase: ChatSetupUseCase,
    private val chatSyncManager: ChatSyncManager,
    private val contactsUseCases: ContactsUseCases,
    private val authRepository: AuthRepository // 🚀 FIX: AuthRepository যোগ করা হয়েছে
) : ViewModel() {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
    private val summaryManager = notificationManager?.let { NotificationSummaryManager(context, it) }

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
    private var initialPassedId: String? = null
    var currentUserId: String = ""
        private set

    private var chatObservingJob: Job? = null
    private var roomObserveJob: Job? = null

    private var resolvedContactName: String? = null

    init {
        viewModelScope.launch {
            ForegroundChatState.activeChatId.collectLatest {
                AppLogger.d("ChatVM", "Foreground chat updated to: $it")
            }
        }
    }

    fun toggleSelection(messageId: String) {
        _selectedMessages.value = _selectedMessages.value.toMutableSet().apply {
            if (contains(messageId)) remove(messageId) else add(messageId)
        }
    }

    fun clearSelection() {
        _selectedMessages.value = emptySet()
    }

    fun initializeChat(passedId: String) {
        if (initialPassedId == passedId && currentChatId != null) return

        if (currentChatId != null && currentChatId != passedId) {
            _messages.value = emptyList()
            clearSelection()
        }

        initialPassedId = passedId
        resolvedContactName = null
        chatObservingJob?.cancel()

        chatObservingJob = viewModelScope.launch {
            // 🚀 FIX: মেসেজ অবজার্ভ করার আগেই কারেন্ট ইউজার আইডি সেট করা হলো!
            if (currentUserId.isEmpty()) {
                currentUserId = authRepository.getCurrentUserId() ?: ""
            }

            // 🚀 FAST RENDER: এখন মেসেজ লোড হলে অ্যাপ ঠিকভাবে চিনবে কোনটা তোমার মেসেজ
            roomObserveJob?.cancel()
            roomObserveJob = launch { observeMessagesForChat(passedId) }

            val instantInfoDeferred = async { chatSetupUseCase.getInstantPartnerInfo(passedId) }
            val contactsDeferred = async { contactsUseCases.getContacts().firstOrNull() ?: emptyList() }

            val (instantPartnerId, instantQuickName) = instantInfoDeferred.await()
            val contacts = contactsDeferred.await()
            val contactName = contacts.find { it.registeredUserId == instantPartnerId }?.contactName?.takeIf { it.isNotBlank() }
            resolvedContactName = contactName

            val initialName = contactName ?: instantQuickName ?: "Unknown"

            _partnerState.value = PartnerUiState.Success(id = instantPartnerId.ifEmpty { passedId }, name = initialName)

            val setupResult = chatSetupUseCase.execute(passedId, initialName)

            if (setupResult != null) {
                currentUserId = setupResult.currentUserId // Safety fallback
                val resolvedChatId = setupResult.actualChatId

                if (resolvedContactName == null && setupResult.partnerName != null && setupResult.partnerName != "Unknown") {
                    val current = _partnerState.value
                    if (current is PartnerUiState.Success && current.name == "Unknown") {
                        _partnerState.value = current.copy(name = setupResult.partnerName)
                    }
                }

                if (currentChatId != resolvedChatId) {
                    currentChatId?.let { chatSyncManager.stopRealtimeListener(it) }

                    currentChatId = resolvedChatId
                    ForegroundChatState.setActiveChat(resolvedChatId)
                    chatSyncManager.startRealtimeListener(resolvedChatId)

                    launch {
                        try {
                            chatSyncManager.fetchMissedMessages(resolvedChatId)
                        } catch (e: Exception) {
                            AppLogger.e("ChatVM", "Failed to fetch missed messages", e)
                        }
                    }

                    if (passedId != resolvedChatId) {
                        roomObserveJob?.cancel()
                        roomObserveJob = launch { observeMessagesForChat(resolvedChatId) }
                    }
                }
            }
        }
    }

    private suspend fun observeMessagesForChat(chatId: String) {
        try {
            chatRepository.observeMessages(chatId).collectLatest { msgs ->
                val filteredMsgs = msgs.distinctBy { it.id }
                _messages.value = filteredMsgs

                if (currentUserId.isNotEmpty()) {
                    try {
                        val hasUnread = filteredMsgs.any { it.senderId != currentUserId && it.receipts?.any { r -> r.userId == currentUserId && r.status == "read" } != true }
                        if (hasUnread) {
                            chatRepository.updateLastRead(chatId, currentUserId)
                        }
                        notificationManager?.cancel(chatId.hashCode())
                        summaryManager?.updateSummaryNotification()
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        AppLogger.e("ChatVM", "Error updating last read", e)
                    }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        }
    }

    suspend fun sendMessage(content: String, replyToId: String? = null): SendMessageResult {
        val chatId = currentChatId ?: return SendMessageResult.Failure("চ্যাট লোড হয়নি")
        if (currentUserId.isEmpty()) return SendMessageResult.Failure("লগইন স্ট্যাটাস পাওয়া যায়নি")

        val result = chatRepository.sendMessage(chatId, currentUserId, content, replyToId)
        return if (result.isSuccess) SendMessageResult.Success else SendMessageResult.Failure(result.exceptionOrNull()?.message ?: "অজানা সমস্যা")
    }

    fun sendAttachment(uri: Uri, replyToId: String? = null, caption: String? = null) {
        if (currentChatId == null || currentUserId.isEmpty()) return
        viewModelScope.launch { processAttachment(uri, replyToId, caption) }
    }

    fun sendAttachments(uris: List<Uri>, replyToId: String?, caption: String?) {
        if (currentChatId == null || currentUserId.isEmpty() || uris.isEmpty()) return
        viewModelScope.launch {
            uris.forEachIndexed { index, uri ->
                processAttachment(uri, replyToId, if (index == 0) caption else null)
            }
        }
    }

    private suspend fun processAttachment(uri: Uri, replyToId: String?, caption: String?) {
        val chatId = currentChatId ?: return
        if (currentUserId.isEmpty()) return
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            var fileName = "attachment_${System.currentTimeMillis()}"
            var fileSize = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val result = chatRepository.sendAttachmentMessage(chatId, currentUserId, uri.toString(), mimeType, fileName, fileSize, replyToId, caption)
            if (result.isSuccess) cleanupCameraCacheFile(uri)
        } catch (e: Exception) {
            AppLogger.e("ChatVM", "Error processing attachment for chat $chatId", e)
        }
    }

    private fun cleanupCameraCacheFile(uri: Uri) {
        try {
            if (uri.scheme != "content" || uri.authority != "${context.packageName}.fileprovider") return
            val fileName = uri.lastPathSegment ?: return
            if (fileName.isBlank() || !fileName.startsWith("IMG_")) return
            val file = File(File(context.cacheDir, "camera_images"), fileName)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            AppLogger.e("ChatVM", "Failed to cleanup temp file", e)
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch { chatRepository.editMessage(messageId, newContent) }
    }

    fun toggleSaveMessage(messageId: String) {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch { chatRepository.toggleSaveMessage(messageId, currentUserId) }
    }

    fun deleteSelectedMessages(type: String = "for_me") {
        if (currentUserId.isEmpty() || _selectedMessages.value.isEmpty()) return
        viewModelScope.launch {
            _selectedMessages.value.forEach { msgId -> chatRepository.deleteMessage(msgId, currentUserId, type) }
            clearSelection()
        }
    }

    fun addReaction(messageId: String, reactionType: String) {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch { chatRepository.addReaction(messageId, currentUserId, reactionType) }
    }

    fun updateTypingStatus(isTyping: Boolean) {}

    override fun onCleared() {
        super.onCleared()
        ForegroundChatState.clearActiveChat()
        currentChatId?.let { chatSyncManager.stopRealtimeListener(it) }
    }
}

object ForegroundChatState {
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    fun setActiveChat(chatId: String) { _activeChatId.value = chatId }
    fun clearActiveChat() { _activeChatId.value = null }
}
