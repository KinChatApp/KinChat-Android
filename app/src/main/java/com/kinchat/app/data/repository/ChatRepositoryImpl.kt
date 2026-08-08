package com.kinchat.app.data.repository

import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.local.db.*
import com.kinchat.app.data.remote.api.ChatNotificationService
import com.kinchat.app.data.remote.api.ChatRpcService
import com.kinchat.app.domain.model.ChatMessage
import com.kinchat.app.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val chatDao: ChatDao,
    private val pendingOperationDao: PendingOperationDao,
    private val syncManager: ChatSyncManager,
    private val messageManager: ChatMessageManager,
    private val settingsManager: ChatSettingsManager,
    private val notificationService: ChatNotificationService,
    private val rpcService: ChatRpcService,
    private val syncCoordinator: PendingSyncCoordinator,
    private val sessionProvider: ChatSessionProvider
) : ChatRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeMessages(chatId: String): Flow<List<ChatMessage>> {
        AppLogger.d("ChatRepo", "Observing messages for chatId: $chatId")
        scope.launch {
            try {
                syncManager.fetchMissedMessages(chatId)
                syncManager.startRealtimeListener(chatId)
            } catch (e: Exception) {
                AppLogger.e("ChatRepo", "Failed to start real-time listener or fetch missed messages", e)
            }
        }
        return chatMessageDao.observeMessagesWithDetails(chatId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun sendMessage(chatId: String, senderId: String, content: String, replyToId: String?): Result<Unit> {
        val messageId = UUID.randomUUID().toString()
        AppLogger.i("ChatRepo", "Attempting to send message [$messageId] to chatId: $chatId")

        val result = messageManager.sendMessage(messageId, chatId, senderId, content, replyToId)
        syncCoordinator.triggerSync()

        notificationService.sendNotification(chatId, messageId, senderId, content, replyToId)

        return result
    }

    // 🚀 New implementation for sending attachments
    override suspend fun sendAttachmentMessage(
        chatId: String,
        senderId: String,
        localUri: String,
        mimeType: String,
        fileName: String,
        fileSize: Long,
        fileBytes: ByteArray,
        replyToId: String?
    ): Result<Unit> {
        val messageId = UUID.randomUUID().toString()
        AppLogger.i("ChatRepo", "Attempting to send attachment [$messageId] to chatId: $chatId")

        val result = messageManager.sendAttachmentMessage(
            messageId = messageId,
            chatId = chatId,
            senderId = senderId,
            localUri = localUri,
            mimeType = mimeType,
            fileName = fileName,
            fileSize = fileSize,
            fileBytes = fileBytes,
            replyToId = replyToId
        )
        
        // Sync triggers when the background ImageKit upload completes, but we call this anyway for other pending operations
        syncCoordinator.triggerSync()

        // Notification for attachment
        val notificationContent = if (mimeType.startsWith("image/")) "[Image] $fileName" 
                                  else if (mimeType.startsWith("video/")) "[Video] $fileName" 
                                  else "[File] $fileName"
        
        notificationService.sendNotification(chatId, messageId, senderId, notificationContent, replyToId)

        return result
    }

    override suspend fun editMessage(messageId: String, newContent: String): Result<Unit> {
        AppLogger.d("ChatRepo", "Editing message: $messageId")
        val timestamp = System.currentTimeMillis()
        chatMessageDao.updateMessageContent(messageId, newContent, timestamp)

        val pendingOp = PendingOperationEntity(
            id = UUID.randomUUID().toString(),
            type = OperationType.EDIT_MESSAGE,
            referenceId = messageId,
            payloadJson = newContent,
            createdAt = timestamp
        )
        pendingOperationDao.insertOperation(pendingOp)

        syncCoordinator.triggerSync()
        return Result.success(Unit)
    }

    override suspend fun getPartnerName(chatId: String, currentUserId: String): String? {
        try {
            val localTitle = chatDao.getChatTitle(chatId)
            if (!localTitle.isNullOrBlank()) {
                AppLogger.d("ChatRepo", "Fetched partner name from Local DB")
                return localTitle
            }
        } catch (e: Exception) {
            AppLogger.e("ChatRepo", "Local DB Error getting partner name", e)
        }

        return rpcService.getPartnerName(chatId, currentUserId)
    }

    override suspend fun deleteMessage(messageId: String, userId: String, deleteType: String): Result<Unit> {
        AppLogger.d("ChatRepo", "Deleting message: $messageId, type: $deleteType")
        val result = messageManager.deleteMessage(messageId, userId, deleteType)
        if (deleteType == "for_everyone") {
            syncCoordinator.triggerSync()
        }
        return result
    }

    override suspend fun addReaction(messageId: String, userId: String, reactionType: String): Result<Unit> {
        val result = messageManager.addReaction(messageId, userId, reactionType)
        syncCoordinator.triggerSync()
        return result
    }

    override suspend fun updateChatPinStatus(chatId: String, isPinned: Boolean): Result<Unit> {
        val userId = sessionProvider.getCurrentUserId() ?: return Result.failure(Exception("Unauthorized"))
        val result = settingsManager.updateChatPinStatus(chatId, userId, isPinned)
        syncCoordinator.triggerSync()
        return result
    }

    override suspend fun updateChatArchiveStatus(chatId: String, isArchived: Boolean): Result<Unit> {
        val userId = sessionProvider.getCurrentUserId() ?: return Result.failure(Exception("Unauthorized"))
        val result = settingsManager.updateChatArchiveStatus(chatId, userId, isArchived)
        syncCoordinator.triggerSync()
        return result
    }

    override suspend fun updateChatMuteStatus(chatId: String, isMuted: Boolean): Result<Unit> {
        val userId = sessionProvider.getCurrentUserId() ?: return Result.failure(Exception("Unauthorized"))
        val result = settingsManager.updateChatMuteStatus(chatId, userId, isMuted)
        syncCoordinator.triggerSync()
        return result
    }

    override suspend fun deleteChatParticipant(chatId: String): Result<Unit> {
        val userId = sessionProvider.getCurrentUserId() ?: return Result.failure(Exception("Unauthorized"))
        val result = settingsManager.deleteChatParticipant(chatId, userId)
        syncCoordinator.triggerSync()
        return result
    }

    override suspend fun updateLastRead(chatId: String, userId: String): Result<Unit> {
        val result = settingsManager.updateLastRead(chatId, userId)
        syncCoordinator.triggerSync()
        return result
    }

    // --- Placeholders for unimplemented interface methods ---
    override suspend fun createChatIfNotExists(partnerUserId: String): Result<String> = Result.success("")
    override suspend fun checkIsSaved(messageId: String, userId: String): Boolean = false
    override suspend fun toggleSaveMessage(messageId: String, userId: String): Result<Boolean> = Result.success(true)
    override suspend fun reportMessage(messageId: String, reporterId: String, reportedUserId: String, reason: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatFavoriteStatus(chatId: String, isFavorite: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatBlockStatus(chatId: String, isBlocked: Boolean): Result<Unit> = Result.success(Unit)
}
