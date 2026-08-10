package com.kinchat.app.data.repository.chat.handlers

import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.local.db.MessageReactionDao
import com.kinchat.app.data.local.db.MessageReactionEntity
import com.kinchat.app.data.local.db.OperationType
import com.kinchat.app.data.local.db.ReactionType
import com.kinchat.app.data.repository.chat.ChatMessageDbHelper

class ReactionManager(
    private val messageReactionDao: MessageReactionDao,
    private val dbHelper: ChatMessageDbHelper
) {
    suspend fun addReaction(messageId: String, userId: String, reactionType: String): Result<Unit> = runCatching {
        AppLogger.d("ReactionManager", "Adding reaction $reactionType to msg: $messageId")
        val timestamp = System.currentTimeMillis()
        val reactionEnum = enumValueOf<ReactionType>(reactionType)

        val reactionEntity = MessageReactionEntity(
            messageId = messageId,
            userId = userId,
            reaction = reactionEnum,
            createdAt = timestamp,
            isSynced = false
        )
        messageReactionDao.insertReaction(reactionEntity)

        dbHelper.queuePendingOperation(OperationType.ADD_REACTION, "${messageId}_${userId}", reactionType, timestamp)
    }.onFailure {
        AppLogger.e("ReactionManager", "Failed to add reaction", it)
    }
}
