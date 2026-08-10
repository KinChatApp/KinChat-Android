package com.kinchat.app.data.repository.chat.handlers

import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.local.db.ChatInsightsDao
import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.local.db.ChatMessageEntity
import com.kinchat.app.data.local.db.MessageStatus
import com.kinchat.app.data.local.db.MessageType
import com.kinchat.app.data.local.db.OperationType
import com.kinchat.app.data.repository.chat.ChatMessageDbHelper
import java.time.Instant

class MessageSender(
    private val chatMessageDao: ChatMessageDao,
    private val chatInsightsDao: ChatInsightsDao,
    private val dbHelper: ChatMessageDbHelper
) {
    suspend fun sendMessage(
        messageId: String,
        chatId: String,
        senderId: String,
        content: String,
        replyToId: String?
    ): Result<Unit> = runCatching {
        AppLogger.d("MessageSender", "Saving new message to local DB: $messageId")
        val timestamp = System.currentTimeMillis()

        val entity = ChatMessageEntity(
            id = messageId,
            chatId = chatId,
            senderId = senderId,
            content = content,
            type = MessageType.text,
            status = MessageStatus.PENDING,
            replyToId = replyToId,
            createdAt = timestamp
        )
        chatMessageDao.insertMessage(entity)
        
        dbHelper.ensureChatExistsAndUpdateLastMessage(chatId, senderId, messageId, timestamp)

        val wordCount = content.split("\\s+".toRegex()).count { it.isNotEmpty() }
        val charCount = content.length
        val isoTimestamp = Instant.ofEpochMilli(timestamp).toString()

        chatInsightsDao.incrementMyMessageCount(chatId, wordCount, charCount, isoTimestamp)
        
        dbHelper.queuePendingOperation(OperationType.SEND_MESSAGE, messageId, null, timestamp)
    }.onSuccess {
        AppLogger.i("MessageSender", "✅ Message $messageId saved locally & added to PendingSync")
    }.onFailure {
        AppLogger.e("MessageSender", "❌ Failed to save message locally: $messageId", it)
    }
}
