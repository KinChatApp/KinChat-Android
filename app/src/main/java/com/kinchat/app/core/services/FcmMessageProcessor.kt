package com.kinchat.app.core.services

import android.content.Context
import com.kinchat.app.core.services.models.FcmMessagePayload
import com.kinchat.app.core.utils.NotificationHelper
import com.kinchat.app.data.local.datastore.AuthPreferencesManager
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
    private val authPreferencesManager: AuthPreferencesManager
) {
    private val notificationHelper: NotificationHelper by lazy {
        NotificationHelper(context)
    }

    suspend fun processAndNotify(payload: FcmMessagePayload) = withContext(Dispatchers.IO) {
        val currentUserId = authPreferencesManager.meId.firstOrNull() ?: return@withContext

        val allMessages = chatRepository.observeMessages(payload.chatId)
            .firstOrNull()?.toMutableList() ?: mutableListOf()

        val isMessageAlreadyInDb = allMessages.any { it.id == payload.messageId }
        
        if (!isMessageAlreadyInDb) {
            allMessages.add(
                ChatMessage(
                    id = payload.messageId,
                    chatId = payload.chatId,
                    senderId = payload.senderId,
                    content = payload.messageText,
                    createdAt = Instant.now().toString()
                )
            )
        }

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
