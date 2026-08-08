package com.kinchat.app.data.repository.settings

import com.kinchat.app.data.local.db.ChatParticipantDao
import com.kinchat.app.data.local.db.OperationType
import com.kinchat.app.data.local.db.PendingOperationDao
import com.kinchat.app.data.local.db.PendingOperationEntity
import java.util.UUID

internal class ChatLocalSettingsHandler(
    private val chatParticipantDao: ChatParticipantDao,
    private val pendingOperationDao: PendingOperationDao
) {
    suspend fun updatePinStatus(chatId: String, userId: String, isPinned: Boolean): Result<Unit> = runCatching {
        chatParticipantDao.updatePinStatus(chatId, userId, isPinned)
        enqueueOperation(OperationType.UPDATE_CHAT_PIN, chatId, isPinned.toString())
    }

    suspend fun updateMuteStatus(chatId: String, userId: String, isMuted: Boolean): Result<Unit> = runCatching {
        chatParticipantDao.updateMuteStatus(chatId, userId, isMuted)
        enqueueOperation(OperationType.UPDATE_CHAT_MUTE, chatId, isMuted.toString())
    }

    suspend fun updateArchiveStatus(chatId: String, userId: String, isArchived: Boolean): Result<Unit> = runCatching {
        chatParticipantDao.updateArchiveStatus(chatId, userId, isArchived)
        enqueueOperation(OperationType.UPDATE_CHAT_ARCHIVE, chatId, isArchived.toString())
    }

    suspend fun updateHiddenStatus(chatId: String, userId: String): Result<Unit> = runCatching {
        chatParticipantDao.updateHiddenStatus(chatId, userId, true)
        enqueueOperation(OperationType.UPDATE_CHAT_HIDDEN, chatId, true.toString())
    }

    suspend fun updateLastRead(chatId: String, userId: String): Result<Unit> = runCatching {
        val timestamp = System.currentTimeMillis()
        chatParticipantDao.updateLastRead(chatId, userId, timestamp)
        enqueueOperation(OperationType.UPDATE_LAST_READ, chatId, timestamp.toString())
    }

    private suspend fun enqueueOperation(type: OperationType, referenceId: String, payload: String?) {
        val operation = PendingOperationEntity(
            id = UUID.randomUUID().toString(),
            type = type,
            referenceId = referenceId,
            payloadJson = payload,
            createdAt = System.currentTimeMillis()
        )
        pendingOperationDao.insertOperation(operation)
    }
}
