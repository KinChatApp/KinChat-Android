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
        LEFT JOIN messages ON chats.lastMessageId = messages.id
        WHERE chat_participants.userId = :currentUserId AND chat_participants.isHidden = 0
        ORDER BY chat_participants.isPinned DESC, chats.lastMessageTime DESC
    """) // 🚀 FIX: LEFT JOIN messages যোগ করা হয়েছে যাতে মেসেজের স্টেট পালটালে রুম রিঅ্যাক্ট করে
    fun observeAllChatsFlow(currentUserId: String): Flow<List<ChatPreview>>

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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChatIfNotExists(chat: ChatEntity)

    @Query("""
        INSERT OR IGNORE INTO chat_participants
        (chatId, userId, role, joinedAt, unreadCount, isPinned, isMuted, isArchived, isHidden, isLocked)
        VALUES (:chatId, :userId, 'member', :time, 0, 0, 0, 0, 0, 0)
    """)
    suspend fun insertLocalParticipant(chatId: String, userId: String, time: Long)

    @Query("""
        SELECT c.id FROM chats c
        INNER JOIN chat_participants cp1 ON c.id = cp1.chatId
        INNER JOIN chat_participants cp2 ON c.id = cp2.chatId
        WHERE c.isGroup = 0
          AND cp1.userId = :currentUserId
          AND cp2.userId = :partnerUserId
        LIMIT 1
    """)
    suspend fun getDirectChatId(currentUserId: String, partnerUserId: String): String?
}
