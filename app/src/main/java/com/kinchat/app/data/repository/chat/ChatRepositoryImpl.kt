package com.kinchat.app.data.repository.chat

import com.kinchat.app.data.local.db.ChatDao
import com.kinchat.app.data.local.db.ChatEntity
import com.kinchat.app.data.local.db.OperationType
import com.kinchat.app.data.local.db.PendingOperationDao
import com.kinchat.app.data.local.db.PendingOperationEntity
import com.kinchat.app.data.repository.chat.delegates.ChatSettingsDelegate
import com.kinchat.app.data.repository.chat.delegates.MessageEditor
import com.kinchat.app.data.repository.chat.delegates.MessageManagerDelegate
import com.kinchat.app.data.repository.chat.delegates.MessageObserver
import com.kinchat.app.data.repository.chat.delegates.PartnerInfoProvider
import com.kinchat.app.domain.model.ChatMessage
import com.kinchat.app.domain.repository.AuthRepository
import com.kinchat.app.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val messageObserver: MessageObserver,
    private val messageManagerDelegate: MessageManagerDelegate,
    private val messageEditor: MessageEditor,
    private val partnerInfoProvider: PartnerInfoProvider,
    private val chatSettingsDelegate: ChatSettingsDelegate,
    private val authRepository: AuthRepository,
    private val chatDao: ChatDao,
    private val pendingOperationDao: PendingOperationDao 
) : ChatRepository {

    override fun observeMessages(chatId: String): Flow<List<ChatMessage>> = messageObserver.observeMessages(chatId)
    override suspend fun sendMessage(chatId: String, senderId: String, content: String, replyToId: String?): Result<Unit> = messageManagerDelegate.sendMessage(chatId, senderId, content, replyToId)
    override suspend fun sendAttachmentMessage(chatId: String, senderId: String, localUri: String, mimeType: String, fileName: String, fileSize: Long, replyToId: String?, caption: String?): Result<Unit> = messageManagerDelegate.sendAttachmentMessage(chatId, senderId, localUri, mimeType, fileName, fileSize, replyToId, caption)
    override suspend fun editMessage(messageId: String, newContent: String): Result<Unit> = messageEditor.editMessage(messageId, newContent)
    override suspend fun getPartnerName(chatId: String, currentUserId: String): String? = partnerInfoProvider.getPartnerName(chatId, currentUserId)
    override suspend fun deleteMessage(messageId: String, userId: String, deleteType: String): Result<Unit> = messageManagerDelegate.deleteMessage(messageId, userId, deleteType)
    override suspend fun addReaction(messageId: String, userId: String, reactionType: String): Result<Unit> = messageManagerDelegate.addReaction(messageId, userId, reactionType)

    override suspend fun updateChatPinStatus(chatId: String, isPinned: Boolean): Result<Unit> = chatSettingsDelegate.updateChatPinStatus(chatId, isPinned)
    override suspend fun updateChatArchiveStatus(chatId: String, isArchived: Boolean): Result<Unit> = chatSettingsDelegate.updateChatArchiveStatus(chatId, isArchived)
    override suspend fun updateChatMuteStatus(chatId: String, isMuted: Boolean): Result<Unit> = chatSettingsDelegate.updateChatMuteStatus(chatId, isMuted)
    override suspend fun deleteChatParticipant(chatId: String): Result<Unit> = chatSettingsDelegate.deleteChatParticipant(chatId)
    override suspend fun updateLastRead(chatId: String, userId: String): Result<Unit> = chatSettingsDelegate.updateLastRead(chatId, userId)

    override suspend fun createChatIfNotExists(partnerUserId: String): Result<String> {
        val currentUserId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User not authenticated."))

        // 🚀 FIX: প্রথমে চেক করা হচ্ছে ডাটাবেসে এই ইউজারের সাথে আগে থেকে চ্যাট আছে কিনা
        val existingChatId = chatDao.getDirectChatId(currentUserId, partnerUserId)
        if (existingChatId != null) {
            return Result.success(existingChatId) // যদি থাকে, আগের চ্যাট আইডি রিটার্ন করবে।
        }

        // যদি না থাকে, তবেই নতুন আইডি তৈরি করবে।
        val sortedIds = listOf(currentUserId, partnerUserId).sorted()
        val deterministicString = "chat_${sortedIds[0]}_${sortedIds[1]}"
        val deterministicChatId = UUID.nameUUIDFromBytes(deterministicString.toByteArray()).toString()
        val time = System.currentTimeMillis()

        val chatEntity = ChatEntity(
            id = deterministicChatId,
            title = null,
            avatarUrl = null,
            isGroup = false,
            lastMessageId = null,
            lastMessageTime = time,
            createdBy = currentUserId,
            createdAt = time,
            updatedAt = time
        )
        chatDao.insertChatIfNotExists(chatEntity)
        chatDao.insertLocalParticipant(deterministicChatId, currentUserId, time)
        chatDao.insertLocalParticipant(deterministicChatId, partnerUserId, time)

        val payload = """{"partner_id":"$partnerUserId"}"""
        queueOperation(OperationType.CREATE_CHAT, deterministicChatId, payload)

        return Result.success(deterministicChatId)
    }

    override suspend fun checkIsSaved(messageId: String, userId: String): Boolean = false

    override suspend fun toggleSaveMessage(messageId: String, userId: String): Result<Boolean> {
        queueOperation(OperationType.TOGGLE_SAVE_MESSAGE, messageId)
        return Result.success(true)
    }

    override suspend fun reportMessage(messageId: String, reporterId: String, reportedUserId: String, reason: String): Result<Unit> {
        queueOperation(OperationType.REPORT_MESSAGE, messageId, """{"reason":"$reason", "reported_user":"$reportedUserId"}""")
        return Result.success(Unit)
    }

    override suspend fun updateChatFavoriteStatus(chatId: String, isFavorite: Boolean): Result<Unit> {
        queueOperation(OperationType.UPDATE_CHAT_FAVORITE, chatId, isFavorite.toString())
        return Result.success(Unit)
    }

    override suspend fun updateChatBlockStatus(chatId: String, isBlocked: Boolean): Result<Unit> {
        queueOperation(OperationType.UPDATE_CHAT_BLOCK, chatId, isBlocked.toString())
        return Result.success(Unit)
    }

    private suspend fun queueOperation(type: OperationType, referenceId: String, payload: String? = null) {
        val sequence = pendingOperationDao.getNextSequence()
        pendingOperationDao.insertOperation(
            PendingOperationEntity(
                id = UUID.randomUUID().toString(),
                type = type,
                referenceId = referenceId,
                payloadJson = payload,
                createdAt = System.currentTimeMillis(),
                sequence = sequence,
                status = "PENDING"
            )
        )
    }
}
