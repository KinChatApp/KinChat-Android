package com.kinchat.app.data.repository.chat

import com.kinchat.app.data.repository.chat.delegates.ChatSettingsDelegate
import com.kinchat.app.data.repository.chat.delegates.MessageEditor
import com.kinchat.app.data.repository.chat.delegates.MessageManagerDelegate
import com.kinchat.app.data.repository.chat.delegates.MessageObserver
import com.kinchat.app.data.repository.chat.delegates.PartnerInfoProvider
import com.kinchat.app.domain.model.ChatMessage
import com.kinchat.app.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val messageObserver: MessageObserver,
    private val messageManagerDelegate: MessageManagerDelegate,
    private val messageEditor: MessageEditor,
    private val partnerInfoProvider: PartnerInfoProvider,
    private val chatSettingsDelegate: ChatSettingsDelegate
) : ChatRepository {

    override fun observeMessages(chatId: String): Flow<List<ChatMessage>> =
        messageObserver.observeMessages(chatId)

    override suspend fun sendMessage(chatId: String, senderId: String, content: String, replyToId: String?): Result<Unit> =
        messageManagerDelegate.sendMessage(chatId, senderId, content, replyToId)

    override suspend fun sendAttachmentMessage(
        chatId: String, senderId: String, localUri: String, mimeType: String,
        fileName: String, fileSize: Long, fileBytes: ByteArray, replyToId: String?
    ): Result<Unit> =
        messageManagerDelegate.sendAttachmentMessage(chatId, senderId, localUri, mimeType, fileName, fileSize, fileBytes, replyToId)

    override suspend fun editMessage(messageId: String, newContent: String): Result<Unit> =
        messageEditor.editMessage(messageId, newContent)

    override suspend fun getPartnerName(chatId: String, currentUserId: String): String? =
        partnerInfoProvider.getPartnerName(chatId, currentUserId)

    override suspend fun deleteMessage(messageId: String, userId: String, deleteType: String): Result<Unit> =
        messageManagerDelegate.deleteMessage(messageId, userId, deleteType)

    override suspend fun addReaction(messageId: String, userId: String, reactionType: String): Result<Unit> =
        messageManagerDelegate.addReaction(messageId, userId, reactionType)

    override suspend fun updateChatPinStatus(chatId: String, isPinned: Boolean): Result<Unit> =
        chatSettingsDelegate.updateChatPinStatus(chatId, isPinned)

    override suspend fun updateChatArchiveStatus(chatId: String, isArchived: Boolean): Result<Unit> =
        chatSettingsDelegate.updateChatArchiveStatus(chatId, isArchived)

    override suspend fun updateChatMuteStatus(chatId: String, isMuted: Boolean): Result<Unit> =
        chatSettingsDelegate.updateChatMuteStatus(chatId, isMuted)

    override suspend fun deleteChatParticipant(chatId: String): Result<Unit> =
        chatSettingsDelegate.deleteChatParticipant(chatId)

    override suspend fun updateLastRead(chatId: String, userId: String): Result<Unit> =
        chatSettingsDelegate.updateLastRead(chatId, userId)

    // --- Placeholders for unimplemented interface methods ---
    override suspend fun createChatIfNotExists(partnerUserId: String): Result<String> = Result.success("")
    override suspend fun checkIsSaved(messageId: String, userId: String): Boolean = false
    override suspend fun toggleSaveMessage(messageId: String, userId: String): Result<Boolean> = Result.success(true)
    override suspend fun reportMessage(messageId: String, reporterId: String, reportedUserId: String, reason: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatFavoriteStatus(chatId: String, isFavorite: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatBlockStatus(chatId: String, isBlocked: Boolean): Result<Unit> = Result.success(Unit)
}
