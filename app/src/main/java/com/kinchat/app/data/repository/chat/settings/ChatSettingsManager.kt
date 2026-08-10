package com.kinchat.app.data.repository.chat.settings

import com.kinchat.app.data.local.db.ChatParticipantDao
import com.kinchat.app.data.local.db.PendingOperationDao
import com.kinchat.app.data.repository.chat.settings.ChatLocalSettingsHandler
import com.kinchat.app.data.repository.chat.settings.ChatRemoteSettingsHandler
import io.github.jan.supabase.SupabaseClient

/**
 * 🚀 Facade Manager for Chat Settings.
 * Implementation logic has been aggressively extracted into smaller components
 * (ChatLocalSettingsHandler & ChatRemoteSettingsHandler) following SRP.
 */
class ChatSettingsManager(
    supabaseClient: SupabaseClient,
    chatParticipantDao: ChatParticipantDao,
    pendingOperationDao: PendingOperationDao
) {
    // Extracted components instantiated internally to prevent breaking existing DI definitions.
    private val localSettingsHandler = ChatLocalSettingsHandler(chatParticipantDao, pendingOperationDao)
    private val remoteSettingsHandler = ChatRemoteSettingsHandler(supabaseClient)

    suspend fun updateChatPinStatus(chatId: String, userId: String, isPinned: Boolean): Result<Unit> {
        return localSettingsHandler.updatePinStatus(chatId, userId, isPinned)
    }

    suspend fun updateChatMuteStatus(chatId: String, userId: String, isMuted: Boolean): Result<Unit> {
        return localSettingsHandler.updateMuteStatus(chatId, userId, isMuted)
    }

    suspend fun updateChatArchiveStatus(chatId: String, userId: String, isArchived: Boolean): Result<Unit> {
        return localSettingsHandler.updateArchiveStatus(chatId, userId, isArchived)
    }

    suspend fun deleteChatParticipant(chatId: String, userId: String): Result<Unit> {
        return localSettingsHandler.updateHiddenStatus(chatId, userId)
    }

    suspend fun updateLastRead(chatId: String, userId: String): Result<Unit> {
        return localSettingsHandler.updateLastRead(chatId, userId)
    }

    suspend fun updateChatFavoriteStatus(chatId: String, isFavorite: Boolean): Result<Unit> {
        return remoteSettingsHandler.updateFavoriteStatus(chatId, isFavorite)
    }

    suspend fun updateChatBlockStatus(chatId: String, isBlocked: Boolean): Result<Unit> {
        return remoteSettingsHandler.updateBlockStatus(chatId, isBlocked)
    }
}
