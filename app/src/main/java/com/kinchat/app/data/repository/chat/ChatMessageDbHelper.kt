package com.kinchat.app.data.repository.chat

import com.kinchat.app.data.local.db.*
import java.util.UUID
import javax.inject.Inject

class ChatMessageDbHelper @Inject constructor(
    private val chatDao: ChatDao,
    private val pendingOperationDao: PendingOperationDao
) {
    suspend fun ensureChatExistsAndUpdateLastMessage(
        chatId: String,
        senderId: String,
        messageId: String,
        timestamp: Long
    ) {
        val dummyChat = ChatEntity(
            id = chatId,
            title = "New Chat",
            isGroup = false,
            avatarUrl = null,
            lastMessageId = messageId,
            lastMessageTime = timestamp,
            createdBy = senderId,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        chatDao.insertChatIfNotExists(dummyChat)
        chatDao.insertLocalParticipant(chatId, senderId, timestamp)
        chatDao.updateLastMessageInfo(chatId, messageId, timestamp)
    }

    suspend fun queuePendingOperation(
        type: OperationType,
        referenceId: String,
        payloadJson: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val pendingOp = PendingOperationEntity(
            id = UUID.randomUUID().toString(),
            type = type,
            referenceId = referenceId,
            payloadJson = payloadJson,
            createdAt = timestamp
        )
        pendingOperationDao.insertOperation(pendingOp)
    }
}
