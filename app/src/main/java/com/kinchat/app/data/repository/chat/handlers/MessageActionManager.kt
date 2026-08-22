package com.kinchat.app.data.repository.chat.handlers

import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.local.db.OperationType
import com.kinchat.app.data.repository.chat.ChatMessageDbHelper
import java.util.concurrent.ConcurrentHashMap

class MessageActionManager(
    private val chatMessageDao: ChatMessageDao,
    private val dbHelper: ChatMessageDbHelper
) {
    private val savedMessagesCache = ConcurrentHashMap<String, Boolean>()

    suspend fun deleteMessage(messageId: String, userId: String, deleteType: String): Result<Unit> = runCatching {
        AppLogger.d("MessageActionManager", "Deleting message: $messageId, type: $deleteType")
        val timestamp = System.currentTimeMillis()
        
        if (deleteType == "for_everyone") {
            chatMessageDao.markAsDeletedForEveryone(messageId, timestamp)
            dbHelper.queuePendingOperation(OperationType.DELETE_MESSAGE, messageId, null, timestamp)
        } else {
            chatMessageDao.softDeleteForMe(messageId)
        }
    }.onFailure {
        AppLogger.e("MessageActionManager", "Failed to delete message", it)
    }

    suspend fun toggleSaveMessage(messageId: String, userId: String): Result<Boolean> = Result.success(true)

    suspend fun checkIsSaved(messageId: String, userId: String): Boolean = false

    suspend fun reportMessage(messageId: String, reporterId: String, reportedUserId: String, reason: String): Result<Unit> = Result.success(Unit)
}
