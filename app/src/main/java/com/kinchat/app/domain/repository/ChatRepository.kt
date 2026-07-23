package com.kinchat.app.domain.repository

import com.kinchat.app.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun getPartnerName(chatId: String, currentUserId: String): String?
    suspend fun addReaction(messageId: String, userId: String, reactionType: String): Result<Unit>
    suspend fun checkIsSaved(messageId: String, userId: String): Boolean
    suspend fun toggleSaveMessage(messageId: String, userId: String): Result<Boolean>
    suspend fun deleteMessage(messageId: String, userId: String, deleteType: String): Result<Unit>
    suspend fun reportMessage(messageId: String, reporterId: String, reportedUserId: String, reason: String): Result<Unit>
    suspend fun updateLastRead(chatId: String, userId: String): Result<Unit>
    suspend fun sendMessage(chatId: String, senderId: String, content: String, replyToId: String? = null): Result<Unit>
    fun observeMessages(chatId: String): Flow<List<ChatMessage>>
    suspend fun createChatIfNotExists(partnerUserId: String): Result<String>

    // 🚀 Context Menu Actions (Supabase logic)
    suspend fun updateChatPinStatus(chatId: String, isPinned: Boolean): Result<Unit>
    suspend fun updateChatFavoriteStatus(chatId: String, isFavorite: Boolean): Result<Unit>
    suspend fun updateChatArchiveStatus(chatId: String, isArchived: Boolean): Result<Unit>
    suspend fun updateChatMuteStatus(chatId: String, isMuted: Boolean): Result<Unit>
    suspend fun updateChatBlockStatus(chatId: String, isBlocked: Boolean): Result<Unit>
    suspend fun deleteChatParticipant(chatId: String): Result<Unit>
}
