package com.kinchat.app.data.repository.chat.delegates

import com.kinchat.app.data.repository.chat.ChatSessionProvider
import com.kinchat.app.data.repository.chat.settings.ChatSettingsManager
import com.kinchat.app.data.repository.chat.sync.PendingSyncCoordinator
import javax.inject.Inject

class ChatSettingsDelegate @Inject constructor(
    private val settingsManager: ChatSettingsManager,
    private val syncCoordinator: PendingSyncCoordinator,
    private val sessionProvider: ChatSessionProvider
) {
    suspend fun updateChatPinStatus(chatId: String, isPinned: Boolean): Result<Unit> =
        executeWithUser { userId -> settingsManager.updateChatPinStatus(chatId, userId, isPinned) }

    suspend fun updateChatArchiveStatus(chatId: String, isArchived: Boolean): Result<Unit> =
        executeWithUser { userId -> settingsManager.updateChatArchiveStatus(chatId, userId, isArchived) }

    suspend fun updateChatMuteStatus(chatId: String, isMuted: Boolean): Result<Unit> =
        executeWithUser { userId -> settingsManager.updateChatMuteStatus(chatId, userId, isMuted) }

    suspend fun deleteChatParticipant(chatId: String): Result<Unit> =
        executeWithUser { userId -> settingsManager.deleteChatParticipant(chatId, userId) }

    suspend fun updateLastRead(chatId: String, userId: String): Result<Unit> {
        val result = settingsManager.updateLastRead(chatId, userId)
        syncCoordinator.triggerSync()
        return result
    }

    private suspend fun executeWithUser(action: suspend (String) -> Result<Unit>): Result<Unit> {
        val userId = sessionProvider.getCurrentUserId() ?: return Result.failure(Exception("Unauthorized"))
        val result = action(userId)
        syncCoordinator.triggerSync()
        return result
    }
}
