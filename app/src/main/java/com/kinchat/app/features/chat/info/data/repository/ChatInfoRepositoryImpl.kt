package com.kinchat.app.features.chat.info.data.repository

import com.kinchat.app.features.chat.info.data.repository.handlers.ChatInfoModerationHandler
import com.kinchat.app.features.chat.info.data.repository.handlers.ChatInfoProfileHandler
import com.kinchat.app.features.chat.info.data.repository.handlers.ChatInfoSettingsHandler
import com.kinchat.app.features.chat.info.domain.model.ChatInfoSettings
import com.kinchat.app.features.chat.info.domain.model.UserProfile
import com.kinchat.app.features.chat.info.domain.repository.ChatInfoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatInfoRepositoryImpl @Inject constructor(
    private val profileHandler: ChatInfoProfileHandler,
    private val settingsHandler: ChatInfoSettingsHandler,
    private val moderationHandler: ChatInfoModerationHandler
) : ChatInfoRepository {

    override fun getUserProfile(userId: String): Flow<UserProfile?> {
        return profileHandler.getUserProfile(userId)
    }

    override suspend fun getChatSettings(partnerId: String): Result<ChatInfoSettings> {
        return settingsHandler.getChatSettings(partnerId)
    }

    override suspend fun toggleMute(chatId: String, isMuted: Boolean): Result<Unit> {
        return settingsHandler.toggleMute(chatId, isMuted)
    }

    override suspend fun toggleBlock(partnerId: String, isBlocked: Boolean): Result<Unit> {
        return moderationHandler.toggleBlock(partnerId, isBlocked)
    }

    override suspend fun clearChat(chatId: String): Result<Unit> {
        return settingsHandler.clearChat(chatId)
    }

    override suspend fun reportUser(partnerId: String): Result<Unit> {
        return moderationHandler.reportUser(partnerId)
    }
}
