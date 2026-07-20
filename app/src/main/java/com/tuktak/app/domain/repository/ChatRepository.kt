package com.tuktak.app.domain.repository

import com.tuktak.app.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    // 🚀 আপডেট: হার্ডকোড স্ট্রিং এড়াতে Nullable String রিটার্ন করবে
    suspend fun getPartnerName(chatId: String, currentUserId: String): String?
    suspend fun addReaction(messageId: String, userId: String, reactionType: String): Result<Unit>
    suspend fun checkIsSaved(messageId: String, userId: String): Boolean
    suspend fun toggleSaveMessage(messageId: String, userId: String): Result<Boolean>
    suspend fun deleteMessage(messageId: String, userId: String, deleteType: String): Result<Unit>
    suspend fun reportMessage(messageId: String, reporterId: String, reportedUserId: String, reason: String): Result<Unit>
    suspend fun updateLastRead(chatId: String, userId: String): Result<Unit>
    suspend fun sendMessage(chatId: String, senderId: String, content: String, replyToId: String? = null): Result<Unit>
    fun observeMessages(chatId: String): Flow<List<ChatMessage>>
}
