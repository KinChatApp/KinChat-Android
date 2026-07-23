package com.kinchat.app.domain.repository

import com.kinchat.app.domain.model.Chat
import com.kinchat.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    suspend fun getUserProfile(userId: String): UserProfile?
    fun getRecentChats(): Flow<List<Chat>>
    suspend fun deleteChat(chatId: String): Result<Unit>
    suspend fun getCurrentUserId(): String?
}
