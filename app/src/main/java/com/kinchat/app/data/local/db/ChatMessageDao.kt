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

    @Query("SELECT MAX(createdAt) FROM messages WHERE chatId = :chatId")
    suspend fun getLastMessageTimestamp(chatId: String): Long?

    // 🚀 Fixed: Background Worker-এর জন্য মেসেজ ফেচ করার মেথড
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): ChatMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)

    // 🚀 Added for edit Message
    @Query("UPDATE messages SET content = :newContent, editedAt = :timestamp WHERE id = :messageId")
    suspend fun updateMessageContent(messageId: String, newContent: String, timestamp: Long)

    @Query("UPDATE messages SET isDeletedForMe = 1, deletedAt = :timestamp WHERE id = :messageId")
    suspend fun softDeleteMessage(messageId: String, timestamp: Long)
}
