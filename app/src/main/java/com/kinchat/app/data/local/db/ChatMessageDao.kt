package com.kinchat.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Transaction
    @Query("SELECT * FROM messages WHERE chatId = :chatId AND isDeletedForMe = 0 ORDER BY createdAt ASC")
    fun observeMessagesWithDetails(chatId: String): Flow<List<MessageWithDetails>>

    // 🚀 SENIOR FIX: Explicitly exclude synthetic preview IDs
    @Query("SELECT MAX(createdAt) FROM messages WHERE chatId = :chatId AND id NOT LIKE 'msg_%_last'")
    suspend fun getLastMessageTimestamp(chatId: String): Long?

    @Query("SELECT MAX(editedAt) FROM messages WHERE chatId = :chatId")
    suspend fun getLastUpdatedTimestamp(chatId: String): Long?

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): ChatMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessageIfAbsent(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    // 🚀 FIX: Merge server fields into local row preserving local-only flags
    @Transaction
    suspend fun upsertMessageMerged(serverMessage: ChatMessageEntity) {
        val local = getMessageById(serverMessage.id)
        if (local == null) {
            insertMessage(serverMessage)
        } else {
            // Never overwrite status of a PENDING/SENT local message
            val keepLocalStatus = local.status == MessageStatus.PENDING || local.status == MessageStatus.SENT
            val mergedStatus = if (keepLocalStatus) local.status else serverMessage.status
            
            // Preserve local soft-deletes
            val mergedDeletedForMe = local.isDeletedForMe || serverMessage.isDeletedForMe
            
            updateMessageMerged(
                id = serverMessage.id,
                content = serverMessage.content,
                status = mergedStatus,
                isDeletedForMe = mergedDeletedForMe,
                editedAt = maxOf(local.editedAt ?: 0L, serverMessage.editedAt ?: 0L),
                deletedAt = serverMessage.deletedAt
            )
        }
    }

    @Query("UPDATE messages SET content = :content, status = :status, isDeletedForMe = :isDeletedForMe, editedAt = :editedAt, deletedAt = :deletedAt WHERE id = :id")
    suspend fun updateMessageMerged(id: String, content: String?, status: MessageStatus, isDeletedForMe: Boolean, editedAt: Long, deletedAt: Long?)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)

    @Query("UPDATE messages SET content = :newContent, editedAt = :timestamp WHERE id = :messageId")
    suspend fun updateMessageContent(messageId: String, newContent: String, timestamp: Long)

    @Query("UPDATE messages SET isDeletedForMe = 1, deletedAt = :timestamp WHERE id = :messageId")
    suspend fun softDeleteMessage(messageId: String, timestamp: Long)
}
