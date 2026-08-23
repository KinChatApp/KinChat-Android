package com.kinchat.app.core.notifications.fcm

import android.content.Context
import android.util.Log
import com.kinchat.app.core.notifications.fcm.model.FcmMessagePayload
import com.kinchat.app.core.notifications.builder.NotificationHelper
import com.kinchat.app.data.local.datastore.AuthPreferencesManager
import com.kinchat.app.data.local.db.ChatParticipantDao
import com.kinchat.app.data.repository.chat.delegates.PartnerInfoProvider
import com.kinchat.app.domain.model.ChatMessage
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
            showFallbackNotification(payload)
            return@withContext
        }

        if (payload.senderId == currentUserId) return@withContext

        val isMuted = chatParticipantDao.isChatMuted(payload.chatId, currentUserId) ?: false
        if (isMuted) return@withContext

        val activeChat = ForegroundChatState.activeChatId.value
        if (activeChat == payload.chatId) return@withContext

        val resolvedSenderName = partnerInfoProvider.getPartnerName(payload.chatId, currentUserId) ?: payload.senderName
        
        // 🚀 FIX: Use payload's createdAt if available, otherwise fallback to current time
        val timestamp = payload.createdAt ?: System.currentTimeMillis()

        val notificationMessage = ChatMessage(
            id = payload.messageId,
            content = payload.messageText,
            senderId = payload.senderId,
            createdAt = timestamp.toString()
        )

        notificationHelper.showConversationNotification(
            chatId = payload.chatId,
            senderName = resolvedSenderName,
            avatarUrl = payload.avatarUrl,
            currentUserId = currentUserId,
            recentMessages = listOf(notificationMessage)
        )
    }

    fun showFallbackNotification(payload: FcmMessagePayload, customName: String? = null) {
        notificationHelper.showFallbackNotification(
            chatId = payload.chatId,
            senderName = customName ?: payload.senderName,
            messageText = payload.messageText
        )
    }
}
