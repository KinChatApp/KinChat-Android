package com.kinchat.app.data.repository.chat.delegates

import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.remote.api.ChatNotificationService
import com.kinchat.app.data.repository.chat.ChatMessageManager
import com.kinchat.app.data.repository.chat.sync.PendingSyncCoordinator
import java.util.UUID
import javax.inject.Inject

class MessageManagerDelegate @Inject constructor(
    private val messageManager: ChatMessageManager,
    private val syncCoordinator: PendingSyncCoordinator,
    private val notificationService: ChatNotificationService
) {
    suspend fun sendMessage(chatId: String, senderId: String, content: String, replyToId: String?): Result<Unit> {
        val messageId = UUID.randomUUID().toString()
        AppLogger.i("MessageManagerDelegate", "Attempting to send message [$messageId] to chatId: $chatId")

        val result = messageManager.sendMessage(messageId, chatId, senderId, content, replyToId)
        syncCoordinator.triggerSync()

        notificationService.sendNotification(chatId, messageId, senderId, content, replyToId)

        return result
    }

    suspend fun sendAttachmentMessage(
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
        AppLogger.i("MessageManagerDelegate", "Attempting to send attachment [$messageId] to chatId: $chatId")

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

        syncCoordinator.triggerSync()

        val notificationContent = when {
            mimeType.startsWith("image/") -> "[Image] $fileName"
            mimeType.startsWith("video/") -> "[Video] $fileName"
            else -> "[File] $fileName"
        }

        notificationService.sendNotification(chatId, messageId, senderId, notificationContent, replyToId)

        return result
    }

    suspend fun deleteMessage(messageId: String, userId: String, deleteType: String): Result<Unit> {
        AppLogger.d("MessageManagerDelegate", "Deleting message: $messageId, type: $deleteType")
        val result = messageManager.deleteMessage(messageId, userId, deleteType)
        if (deleteType == "for_everyone") {
            syncCoordinator.triggerSync()
        }
        return result
    }

    suspend fun addReaction(messageId: String, userId: String, reactionType: String): Result<Unit> {
        val result = messageManager.addReaction(messageId, userId, reactionType)
        syncCoordinator.triggerSync()
        return result
    }
}
