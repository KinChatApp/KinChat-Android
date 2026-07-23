package com.kinchat.app.domain.repository

import com.kinchat.app.domain.model.Chat
import com.kinchat.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    suspend fun getUserProfile(userId: String): UserProfile?
    fun getRecentChats(): Flow<List<Chat>>
    suspend fun deleteChat(chatId: String): Result<Unit>
    suspend fun getCurrentUserId(): String?
    
    suspend fun updateChatPinStatus(chatId: String, isPinned: Boolean): Result<Unit>
    suspend fun updateChatFavoriteStatus(chatId: String, isFavorite: Boolean): Result<Unit>
    suspend fun updateChatArchiveStatus(chatId: String, isArchived: Boolean): Result<Unit>
    suspend fun updateChatMuteStatus(chatId: String, isMuted: Boolean): Result<Unit>
    suspend fun updateChatBlockStatus(chatId: String, isBlocked: Boolean): Result<Unit>
}
