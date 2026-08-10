package com.kinchat.app.data.repository.chat.delegates

import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.local.db.OperationType
import com.kinchat.app.data.local.db.PendingOperationDao
import com.kinchat.app.data.local.db.PendingOperationEntity
import com.kinchat.app.data.repository.chat.sync.PendingSyncCoordinator
import java.util.UUID
import javax.inject.Inject

class MessageEditor @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val pendingOperationDao: PendingOperationDao,
    private val syncCoordinator: PendingSyncCoordinator
) {
    suspend fun editMessage(messageId: String, newContent: String): Result<Unit> {
        AppLogger.d("MessageEditor", "Editing message: $messageId")
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
}
