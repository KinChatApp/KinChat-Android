package com.kinchat.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @RewriteQueriesToDropUnusedColumns
    @Transaction
    @Query("""
        SELECT chats.* FROM chats
        INNER JOIN chat_participants ON chats.id = chat_participants.chatId
        WHERE chat_participants.userId = :currentUserId AND chat_participants.isHidden = 0
        ORDER BY chat_participants.isPinned DESC, chats.lastMessageTime DESC
    """)
    fun observeAllChatsFlow(currentUserId: String): Flow<List<ChatPreview>> // 🚀 FIXED: SELECT chats.* ব্যবহার করা হয়েছে

    @Transaction
    @Query("SELECT * FROM chats WHERE id = :chatId")
    fun observeChatPreview(chatId: String): Flow<ChatPreview?>

    @Query("SELECT title FROM chats WHERE id = :chatId")
    suspend fun getChatTitle(chatId: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Query("UPDATE chats SET lastMessageId = :messageId, lastMessageTime = :time WHERE id = :chatId")
    suspend fun updateLastMessageInfo(chatId: String, messageId: String, time: Long)

    // --- Add these two new methods ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChatIfNotExists(chat: ChatEntity)

    @Query("""
        INSERT OR IGNORE INTO chat_participants 
        (chatId, userId, role, joinedAt, unreadCount, isPinned, isMuted, isArchived, isHidden, isLocked) 
        VALUES (:chatId, :userId, 'member', :time, 0, 0, 0, 0, 0, 0)
    """)
    suspend fun insertLocalParticipant(chatId: String, userId: String, time: Long)
}
