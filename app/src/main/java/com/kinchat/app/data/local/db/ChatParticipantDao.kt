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
}
