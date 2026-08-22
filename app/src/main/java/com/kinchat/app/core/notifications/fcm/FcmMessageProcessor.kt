package com.kinchat.app.core.notifications.fcm

import android.content.Context
import android.util.Log
import com.kinchat.app.core.notifications.fcm.model.FcmMessagePayload
import com.kinchat.app.core.notifications.builder.NotificationHelper
import com.kinchat.app.data.local.datastore.AuthPreferencesManager
import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.local.db.ChatMessageEntity
import com.kinchat.app.data.local.db.ChatParticipantDao
import com.kinchat.app.data.local.db.MessageStatus
import com.kinchat.app.data.local.db.MessageType
import com.kinchat.app.data.repository.chat.delegates.PartnerInfoProvider
import com.kinchat.app.domain.model.ChatMessage
import com.kinchat.app.domain.repository.ChatRepository
import com.kinchat.app.features.chat.viewmodel.ForegroundChatState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmMessageProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    private val chatMessageDao: ChatMessageDao,
    private val chatParticipantDao: ChatParticipantDao,
    private val authPreferencesManager: AuthPreferencesManager,
    private val partnerInfoProvider: PartnerInfoProvider
) {
    private val notificationHelper: NotificationHelper by lazy {
        NotificationHelper(context)
    }

    suspend fun processAndNotify(payload: FcmMessagePayload) = withContext(Dispatchers.IO) {
        val currentUserId = authPreferencesManager.meId.firstOrNull()

        if (currentUserId.isNullOrEmpty()) {
            Log.w("FcmMessageProcessor", "meId is null or empty, falling back to plain notification")
            showFallbackNotification(payload)
            return@withContext
        }

        if (payload.senderId == currentUserId) {
            return@withContext
        }

        val isMuted = chatParticipantDao.isChatMuted(payload.chatId, currentUserId) ?: false
        if (isMuted) {
            Log.d("FcmMessageProcessor", "Chat ${payload.chatId} is muted. Skipping notification.")
            return@withContext
        }

        val activeChat = ForegroundChatState.activeChatId.value
        if (activeChat == payload.chatId) {
            Log.d("FcmMessageProcessor", "User is currently viewing chat ${payload.chatId}. Suppressing notification.")
            return@withContext
        }

        val resolvedSenderName = partnerInfoProvider.getPartnerName(payload.chatId, currentUserId) ?: payload.senderName

        // 🚀 BUG FIX: Fallback to System.currentTimeMillis() since payload.createdAt doesn't exist
        val serverTimestamp = System.currentTimeMillis()

        try {
            val newEntity = ChatMessageEntity(
                id = payload.messageId,
                chatId = payload.chatId,
                senderId = payload.senderId,
                content = payload.messageText,
                createdAt = serverTimestamp, 
                type = MessageType.text,
                status = MessageStatus.DELIVERED,
                isDeletedForMe = false
            )
            chatMessageDao.upsertMessageMerged(newEntity)
        } catch (e: Exception) {
            Log.e("FcmMessageProcessor", "Failed to insert FCM message into Room", e)
        }

        val allMessagesEntities = try {
            chatRepository.observeMessages(payload.chatId).firstOrNull() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val lastReadIndex = allMessagesEntities.indexOfLast { msg ->
            msg.senderId == currentUserId ||
            msg.receipts?.any { it.userId == currentUserId && it.status == "read" } == true
        }

        val startIndex = if (lastReadIndex >= 0) lastReadIndex + 1 else 0
        var unreadMessages = allMessagesEntities.subList(startIndex, allMessagesEntities.size).toMutableList()

        if (unreadMessages.none { it.id == payload.messageId }) {
            unreadMessages.add(ChatMessage(
                id = payload.messageId,
                content = payload.messageText,
                senderId = payload.senderId,
                createdAt = serverTimestamp.toString()
            ))
        }

        val recentMessages = unreadMessages.takeLast(MAX_RECENT_MESSAGES)

        if (recentMessages.isNotEmpty()) {
            notificationHelper.showConversationNotification(
                chatId = payload.chatId,
                senderName = resolvedSenderName,
                avatarUrl = payload.avatarUrl,
                currentUserId = currentUserId,
                recentMessages = recentMessages
            )
        } else {
            showFallbackNotification(payload, resolvedSenderName)
        }
    }

    fun showFallbackNotification(payload: FcmMessagePayload, customName: String? = null) {
        notificationHelper.showFallbackNotification(
            chatId = payload.chatId,
            senderName = customName ?: payload.senderName,
            messageText = payload.messageText
        )
    }

    companion object {
        private const val MAX_RECENT_MESSAGES = 10
    }
}
