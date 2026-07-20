package com.tuktak.app.features.chat.info.domain.repository

import com.tuktak.app.features.chat.info.domain.model.ChatInfoSettings
import com.tuktak.app.features.chat.info.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ChatInfoRepository {
    fun getUserProfile(userId: String): Flow<UserProfile?>
    suspend fun getChatSettings(partnerId: String): Result<ChatInfoSettings>
    suspend fun toggleMute(chatId: String, isMuted: Boolean): Result<Unit>
    suspend fun toggleBlock(partnerId: String, isBlocked: Boolean): Result<Unit>
    suspend fun clearChat(chatId: String): Result<Unit>
    suspend fun reportUser(partnerId: String): Result<Unit>
}
