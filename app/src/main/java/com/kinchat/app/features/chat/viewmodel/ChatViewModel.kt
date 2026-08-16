package com.kinchat.app.features.chat.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.domain.model.ChatMessage
import com.kinchat.app.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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
        AppLogger.i("ChatVM", "Initializing chat flow for passedId: $passedId")
        chatObservingJob?.cancel()

        chatObservingJob = viewModelScope.launch {
            val quickName = chatRepository.getPartnerName(passedId, "")
            if (!quickName.isNullOrBlank()) {
                _partnerState.value = PartnerUiState.Success(id = passedId, name = quickName)
            } else {
                _partnerState.value = PartnerUiState.Loading
            }
            
            // 🚀 FIXED (Phase 3): Removed `_messages.value = emptyList()` to prevent UI flashing.
            currentChatId = passedId

            // 🚀 FIXED (Phase 3): Start observing Room IMMEDIATELY for offline support
            var roomObserveJob = launch {
                observeMessagesForChat(passedId)
            }

            // Execute network setup in background
            val setupResult = chatSetupUseCase.execute(passedId, quickName)

            if (setupResult != null) {
                currentUserId = setupResult.currentUserId
                val resolvedChatId = setupResult.actualChatId
                currentChatId = resolvedChatId
                AppLogger.d("ChatVM", "Chat setup complete. actualChatId: $currentChatId, currentUserId: $currentUserId")

                if (setupResult.partnerName != null) {
                    _partnerState.value = PartnerUiState.Success(
                        id = setupResult.partnerId.ifEmpty { passedId },
                        name = setupResult.partnerName
                    )
                } else {
                    // Fallback to error only if we have no prior name
                    if (quickName.isNullOrBlank()) _partnerState.value = PartnerUiState.Error
                }

                // If actual chat ID from backend is different, re-observe Room with the correct ID
                if (resolvedChatId != passedId) {
                    roomObserveJob.cancel()
                    roomObserveJob = launch {
                        observeMessagesForChat(resolvedChatId)
                    }
                }
            } else {
                AppLogger.w("ChatVM", "Chat Setup Failed! Result is null.")
                if (quickName.isNullOrBlank()) {
                    _partnerState.value = PartnerUiState.Error
                }
            }
        }
    }

    private suspend fun observeMessagesForChat(chatId: String) {
        try {
            chatRepository.observeMessages(chatId).collectLatest { msgs ->
                _messages.value = msgs.distinctBy { it.id }.filter { msg ->
                    msg.deletedForUsers?.contains(currentUserId) != true
                }
                if (currentUserId.isNotEmpty()) {
                    chatRepository.updateLastRead(chatId, currentUserId)
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e("ChatVM", "Error observing messages for $chatId", e)
        }
    }

    suspend fun sendMessage(content: String, replyToId: String? = null): SendMessageResult {
        AppLogger.i("ChatVM", "UI Request: Sending new message")
        val chatId = currentChatId

        if (chatId == null) {
            AppLogger.w("ChatVM", "SendMessage Failed: currentChatId is null")
            return SendMessageResult.Failure("চ্যাট এখনো লোড হয়নি, একটু অপেক্ষা করে আবার চেষ্টা করুন")
        }

        if (currentUserId.isEmpty()) {
            AppLogger.w("ChatVM", "SendMessage Failed: currentUserId is empty")
            return SendMessageResult.Failure("ইউজার লগইন স্ট্যাটাস পাওয়া যায়নি, একটু অপেক্ষা করে আবার চেষ্টা করুন")
        }

        val result = chatRepository.sendMessage(chatId, currentUserId, content, replyToId)

        return if (result.isSuccess) {
            AppLogger.i("ChatVM", "✅ SendMessage UI Result: Success")
            SendMessageResult.Success
        } else {
            val ex = result.exceptionOrNull()
            val rawMessage = ex?.message ?: "অজানা সমস্যা"
            val cleanMessage = rawMessage.substringBefore("URL:").trim().ifBlank {
                rawMessage.take(300)
            }
            AppLogger.e("ChatVM", "❌ sendMessage UI Result: Failed - $rawMessage", ex)
            SendMessageResult.Failure(cleanMessage)
        }
    }

    fun sendAttachment(uri: Uri, replyToId: String? = null, caption: String? = null) {
        val chatId = currentChatId ?: return
        if (currentUserId.isEmpty()) return

        AppLogger.i("ChatVM", "UI Request: Processing attachment $uri")

        viewModelScope.launch {
            processAttachment(uri, replyToId, caption)
        }
    }

    fun sendAttachments(uris: List<Uri>, replyToId: String?, caption: String?) {
        val chatId = currentChatId ?: return
        if (currentUserId.isEmpty() || uris.isEmpty()) return

        AppLogger.i("ChatVM", "UI Request: Processing ${uris.size} attachments for chat $chatId")

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

            val result = chatRepository.sendAttachmentMessage(
                chatId = chatId,
                senderId = currentUserId,
                localUri = uri.toString(),
                mimeType = mimeType,
                fileName = fileName,
                fileSize = fileSize,
                replyToId = replyToId,
                caption = caption
            )

            if (result.isSuccess) {
                cleanupCameraCacheFile(uri)
            }
        } catch (e: Exception) {
            AppLogger.e("ChatVM", "Error processing attachment", e)
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
            AppLogger.e("ChatVM", "Failed to clean up camera cache file", e)
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        if (currentUserId.isEmpty()) return
        AppLogger.d("ChatVM", "UI Request: Edit message $messageId")
        viewModelScope.launch {
            try {
                chatRepository.editMessage(messageId, newContent)
            } catch (e: Exception) {
                AppLogger.e("ChatVM", "Error editing message from UI", e)
            }
        }
    }

    fun toggleSaveMessage(messageId: String) {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch { chatRepository.toggleSaveMessage(messageId, currentUserId) }
    }

    fun deleteSelectedMessages(type: String = "for_me") {
        if (currentUserId.isEmpty() || _selectedMessages.value.isEmpty()) return
        AppLogger.d("ChatVM", "UI Request: Delete selected messages (${_selectedMessages.value.size}), type: $type")
        viewModelScope.launch {
            _selectedMessages.value.forEach { msgId ->
                chatRepository.deleteMessage(msgId, currentUserId, type)
            }
            clearSelection()
        }
    }

    fun addReaction(messageId: String, reactionType: String) {
        if (currentUserId.isEmpty()) return
        AppLogger.d("ChatVM", "UI Request: Add reaction $reactionType to $messageId")
        viewModelScope.launch { chatRepository.addReaction(messageId, currentUserId, reactionType) }
    }

    fun updateTypingStatus(isTyping: Boolean) {}
}
