package com.kinchat.app.domain.repository

import com.kinchat.app.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    // Message Actions
    fun observeMessages(chatId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(chatId: String, senderId: String, content: String, replyToId: String? = null): Result<Unit>
    suspend fun editMessage(messageId: String, newContent: String): Result<Unit> // 🚀 Added
    suspend fun deleteMessage(messageId: String, userId: String, deleteType: String): Result<Unit>
    suspend fun addReaction(messageId: String, userId: String, reactionType: String): Result<Unit>
    
    // Check & Status
    suspend fun getPartnerName(chatId: String, currentUserId: String): String?
    suspend fun createChatIfNotExists(partnerUserId: String): Result<String>
    suspend fun checkIsSaved(messageId: String, userId: String): Boolean
    suspend fun toggleSaveMessage(messageId: String, userId: String): Result<Boolean>
    suspend fun reportMessage(messageId: String, reporterId: String, reportedUserId: String, reason: String): Result<Unit>
    suspend fun updateLastRead(chatId: String, userId: String): Result<Unit>
    
    // Chat Actions
    suspend fun updateChatPinStatus(chatId: String, isPinned: Boolean): Result<Unit>
    suspend fun updateChatFavoriteStatus(chatId: String, isFavorite: Boolean): Result<Unit>
    suspend fun updateChatArchiveStatus(chatId: String, isArchived: Boolean): Result<Unit>
    suspend fun updateChatMuteStatus(chatId: String, isMuted: Boolean): Result<Unit>
    suspend fun updateChatBlockStatus(chatId: String, isBlocked: Boolean): Result<Unit>
    suspend fun deleteChatParticipant(chatId: String): Result<Unit>
}
