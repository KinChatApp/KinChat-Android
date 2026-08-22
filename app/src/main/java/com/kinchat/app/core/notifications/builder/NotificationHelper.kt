package com.kinchat.app.core.notifications.builder

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.Person
import androidx.core.content.getSystemService
import com.kinchat.app.domain.model.ChatMessage
import com.kinchat.app.core.notifications.actions.NotificationActionHelper
import com.kinchat.app.core.notifications.actions.NotificationShortcutHelper

/**
 * Orchestrator class that coordinates various notification components (Image, Action, Style, Summary).
 * Delegates the actual notification building to [NotificationBuilderFactory].
 */
class NotificationHelper(
    context: Context,
    private val imageHelper: NotificationImageHelper = NotificationImageHelper(context),
    private val actionHelper: NotificationActionHelper = NotificationActionHelper(context),
    private val shortcutHelper: NotificationShortcutHelper = NotificationShortcutHelper(context),
    private val builderFactory: NotificationBuilderFactory = NotificationBuilderFactory(context)
) {
    private val appContext: Context = context.applicationContext
    private val notificationManager: NotificationManager =
        requireNotNull(appContext.getSystemService()) { "NotificationManager must be available" }

    // Delegated Components
    private val channelManager = NotificationChannelManager(notificationManager)
    private val summaryManager = NotificationSummaryManager(appContext, notificationManager)
    private val styleBuilder = NotificationStyleBuilder()

    init {
        channelManager.createMessageChannel()
    }

    suspend fun showConversationNotification(
        chatId: String,
        senderName: String,
        avatarUrl: String?,
        currentUserId: String,
        recentMessages: List<ChatMessage>
    ) {
        try {
            val notificationId = chatId.hashCode()
            val senderIcon = imageHelper.loadAvatarOrInitial(avatarUrl, senderName)
            
            // 🚀 FIX: LABEL_ME বা "Me" এর পরিবর্তে "You" সেট করা হলো
            val userPerson = Person.Builder().setName("You").build() 
            val senderPerson = Person.Builder().setName(senderName).setIcon(senderIcon).build()

            val messagingStyle = styleBuilder.buildMessagingStyle(
                senderName = senderName,
                recentMessages = recentMessages,
                currentUserId = currentUserId,
                userPerson = userPerson,
                senderPerson = senderPerson
            )

            val contentIntent = actionHelper.getContentIntent(chatId, notificationId)
            val replyAction = actionHelper.buildReplyAction(chatId, notificationId)
            val markReadAction = actionHelper.buildMarkReadAction(chatId, notificationId)

            shortcutHelper.pushShortcutIfNeeded(
                chatId = chatId,
                senderName = senderName,
                senderIcon = senderIcon,
                contentIntent = actionHelper.getRawContentIntent(chatId),
                senderPerson = senderPerson
            )

            val notification = builderFactory.createConversationBuilder(
                chatId = chatId,
                messagingStyle = messagingStyle,
                contentIntent = contentIntent,
                replyAction = replyAction,
                markReadAction = markReadAction
            ).build()

            notificationManager.notify(notificationId, notification)
            summaryManager.updateSummaryNotification()

        } catch (e: Exception) {
            Log.e(TAG, "showConversationNotification failed for chatId=$chatId, falling back to plain notification", e)
            val fallbackText = recentMessages.lastOrNull()?.content ?: "New message"
            showFallbackNotification(chatId, senderName, fallbackText)
        }
    }

    fun showFallbackNotification(
        chatId: String,
        senderName: String,
        messageText: String
    ) {
        try {
            val notificationId = chatId.hashCode()
            val contentIntent = actionHelper.getContentIntent(chatId, notificationId)

            val notification = builderFactory.createFallbackBuilder(
                senderName = senderName,
                messageText = messageText,
                contentIntent = contentIntent
            ).build()

            notificationManager.notify(notificationId, notification)
            summaryManager.updateSummaryNotification()

        } catch (e: Exception) {
            Log.e(TAG, "showFallbackNotification failed for chatId=$chatId", e)
        }
    }

    companion object {
        private const val TAG = "NotificationHelper"
    }
}
