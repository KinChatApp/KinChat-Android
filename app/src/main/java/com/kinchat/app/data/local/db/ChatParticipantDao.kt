package com.kinchat.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatParticipantDao {
    @Query("SELECT * FROM chat_participants WHERE chatId = :chatId")
    fun observeParticipants(chatId: String): Flow<List<ChatParticipantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ChatParticipantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ChatParticipantEntity>)

    @Query("UPDATE chat_participants SET unreadCount = 0 WHERE chatId = :chatId AND userId = :userId")
    suspend fun clearUnreadCount(chatId: String, userId: String)

    @Query("UPDATE chat_participants SET isPinned = :isPinned WHERE chatId = :chatId AND userId = :userId")
    suspend fun updatePinStatus(chatId: String, userId: String, isPinned: Boolean)

    @Query("UPDATE chat_participants SET isMuted = :isMuted WHERE chatId = :chatId AND userId = :userId")
    suspend fun updateMuteStatus(chatId: String, userId: String, isMuted: Boolean)

    @Query("UPDATE chat_participants SET isArchived = :isArchived WHERE chatId = :chatId AND userId = :userId")
    suspend fun updateArchiveStatus(chatId: String, userId: String, isArchived: Boolean)

    @Query("UPDATE chat_participants SET isHidden = :isHidden WHERE chatId = :chatId AND userId = :userId")
    suspend fun updateHiddenStatus(chatId: String, userId: String, isHidden: Boolean)

    @Query("UPDATE chat_participants SET lastReadAt = :timestamp, unreadCount = 0 WHERE chatId = :chatId AND userId = :userId")
    suspend fun updateLastRead(chatId: String, userId: String, timestamp: Long)

    @Query("SELECT isMuted FROM chat_participants WHERE chatId = :chatId AND userId = :userId LIMIT 1")
    suspend fun isChatMuted(chatId: String, userId: String): Boolean?

    // 🚀 FIX: ChatInfoSettingsHandler-এর জন্য নতুন মেথড অ্যাড করা হলো
    @Query("SELECT * FROM chat_participants WHERE chatId = :chatId AND userId = :userId LIMIT 1")
    suspend fun getParticipant(chatId: String, userId: String): ChatParticipantEntity?
}
