package com.kinchat.app.core.notifications.fcm

import android.content.Context
import com.kinchat.app.core.notifications.fcm.model.FcmMessagePayload
import com.kinchat.app.core.notifications.builder.NotificationHelper
import com.kinchat.app.data.local.datastore.AuthPreferencesManager
import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.local.db.ChatMessageEntity
import com.kinchat.app.data.local.db.MessageStatus
import com.kinchat.app.domain.model.ChatMessage
import com.kinchat.app.domain.repository.ChatRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmMessageProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    private val chatMessageDao: ChatMessageDao,
    private val authPreferencesManager: AuthPreferencesManager
) {
    private val notificationHelper: NotificationHelper by lazy {
        NotificationHelper(context)
    }

    suspend fun processAndNotify(payload: FcmMessagePayload) = withContext(Dispatchers.IO) {
        val currentUserId = authPreferencesManager.meId.firstOrNull() ?: return@withContext

        // 🚀 FIX: Echo Fix - do not show notification for own messages
        if (payload.senderId == currentUserId) {
            return@withContext
        }

        // 🚀 FIX: Write incoming message to Room directly so it persists in background
        try {
            val newEntity = ChatMessageEntity(
                id = payload.messageId,
                chatId = payload.chatId,
                senderId = payload.senderId,
                content = payload.messageText,
                createdAt = Instant.now().toEpochMilli(),
                status = MessageStatus.DELIVERED,
                isDeletedForMe = false
            )
            chatMessageDao.insertMessage(newEntity)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fetch recent messages for notification summary
        val allMessages = chatRepository.observeMessages(payload.chatId)
            .firstOrNull()?.toMutableList() ?: mutableListOf()

        val recentMessages = allMessages.takeLast(MAX_RECENT_MESSAGES)

        if (recentMessages.isNotEmpty()) {
            notificationHelper.showConversationNotification(
                chatId = payload.chatId,
                senderName = payload.senderName,
                avatarUrl = payload.avatarUrl,
                currentUserId = currentUserId,
                recentMessages = recentMessages
            )
        }
    }

    fun showFallbackNotification(payload: FcmMessagePayload) {
        notificationHelper.showFallbackNotification(
            chatId = payload.chatId,
            senderName = payload.senderName,
            messageText = payload.messageText
        )
    }

    companion object {
        private const val MAX_RECENT_MESSAGES = 10
    }
}
