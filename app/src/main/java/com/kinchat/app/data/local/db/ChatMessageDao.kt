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
    @Query("SELECT * FROM messages WHERE chatId = :chatId AND (isDeletedForMe = 0 OR isDeletedForMe IS NULL) ORDER BY createdAt ASC")
    fun observeMessagesWithDetails(chatId: String): Flow<List<MessageWithDetails>>

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

    @Transaction
    suspend fun upsertMessagesMerged(serverMessages: List<ChatMessageEntity>) {
        serverMessages.forEach { upsertMessageMerged(it) }
    }

    @Transaction
    suspend fun upsertMessageMerged(serverMessage: ChatMessageEntity) {
        val local = getMessageById(serverMessage.id)
        if (local == null) {
            insertMessage(serverMessage)
        } else {
            val isLocalTransient = local.status == MessageStatus.PENDING ||
                                   local.status == MessageStatus.SENDING ||
                                   local.status == MessageStatus.FAILED

            val mergedStatus = if (isLocalTransient) {
                serverMessage.status
            } else {
                when (local.status) {
                    MessageStatus.SENT -> {
                        if (serverMessage.status == MessageStatus.DELIVERED || serverMessage.status == MessageStatus.READ) serverMessage.status else local.status
                    }
                    MessageStatus.DELIVERED -> {
                        if (serverMessage.status == MessageStatus.READ) serverMessage.status else local.status
                    }
                    else -> local.status
                }
            }

            val mergedDeletedForMe = local.isDeletedForMe || serverMessage.isDeletedForMe

            val mergedEditedAt = if (local.editedAt != null || serverMessage.editedAt != null) {
                maxOf(local.editedAt ?: 0L, serverMessage.editedAt ?: 0L)
            } else {
                null
            }

            val mergedDeletedAt = if (local.deletedAt != null || serverMessage.deletedAt != null) {
                maxOf(local.deletedAt ?: 0L, serverMessage.deletedAt ?: 0L)
            } else {
                null
            }

            // 🚀 FIX: NO-OP WRITE SUPPRESSION
            val hasChanges = local.content != serverMessage.content ||
                             local.status != mergedStatus ||
                             local.isDeletedForMe != mergedDeletedForMe ||
                             local.editedAt != mergedEditedAt ||
                             local.deletedAt != mergedDeletedAt

            if (hasChanges) {
                updateMessageMerged(
                    id = serverMessage.id,
                    content = serverMessage.content,
                    status = mergedStatus,
                    isDeletedForMe = mergedDeletedForMe,
                    editedAt = mergedEditedAt,
                    deletedAt = mergedDeletedAt
                )
            }
        }
    }

    @Query("UPDATE messages SET content = :content, status = :status, isDeletedForMe = :isDeletedForMe, editedAt = :editedAt, deletedAt = :deletedAt WHERE id = :id")
    suspend fun updateMessageMerged(id: String, content: String?, status: MessageStatus, isDeletedForMe: Boolean, editedAt: Long?, deletedAt: Long?)

    // 🚀 FIX: Prevent unnecessary updates
    @Query("UPDATE messages SET status = :status WHERE id = :messageId AND status != :status")
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus): Int

    @Query("UPDATE messages SET content = :newContent, editedAt = :timestamp WHERE id = :messageId")
    suspend fun updateMessageContent(messageId: String, newContent: String, timestamp: Long)

    @Query("UPDATE messages SET isDeletedForMe = 1 WHERE id = :messageId")
    suspend fun softDeleteForMe(messageId: String)

    @Query("UPDATE messages SET deletedAt = :timestamp WHERE id = :messageId")
    suspend fun markAsDeletedForEveryone(messageId: String, timestamp: Long)

    @Query("UPDATE messages SET isDeletedForMe = 1, deletedAt = :timestamp WHERE id = :messageId")
    suspend fun softDeleteMessage(messageId: String, timestamp: Long)

    // 🚀 FIX: Ignored synthetic IDs generated by dashboard
    @Query("""
        SELECT id FROM messages 
        WHERE chatId = :chatId 
        AND senderId != :currentUserId 
        AND status != :readStatus 
        AND id NOT LIKE 'msg_%_last'
    """)
    suspend fun getUnreadMessageIdsFromPartner(chatId: String, currentUserId: String, readStatus: MessageStatus): List<String>

    @Query("UPDATE messages SET status = :status WHERE id IN (:messageIds)")
    suspend fun markMessagesAsReadLocal(messageIds: List<String>, status: MessageStatus)
}
