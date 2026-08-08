package com.kinchat.app.core.utils

import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.getSystemService
import com.kinchat.app.R
import com.kinchat.app.domain.model.ChatMessage

/**
 * Facade class that coordinates various notification components (Image, Action, Style, Summary).
 * The public API remains unchanged to prevent breaking changes in Service classes.
 */
class NotificationHelper(
    context: Context,
    private val imageHelper: NotificationImageHelper = NotificationImageHelper(context),
    private val actionHelper: NotificationActionHelper = NotificationActionHelper(context),
    private val shortcutHelper: NotificationShortcutHelper = NotificationShortcutHelper(context)
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

            val userPerson = Person.Builder().setName(NotificationConstants.LABEL_ME).build()
            val senderPerson = Person.Builder().setName(senderName).setIcon(senderIcon).build()

            val messagingStyle = styleBuilder.buildMessagingStyle(
                senderName = senderName,
                recentMessages = recentMessages,
                currentUserId = currentUserId,
                userPerson = userPerson,
                senderPerson = senderPerson
            )

            val contentIntent = actionHelper.getContentIntent(chatId, notificationId)

            shortcutHelper.pushShortcutIfNeeded(
                chatId = chatId,
                senderName = senderName,
                senderIcon = senderIcon,
                contentIntent = actionHelper.getRawContentIntent(chatId),
                senderPerson = senderPerson
            )

            val builder = NotificationCompat.Builder(appContext, NotificationConstants.CHANNEL_MESSAGES)
                .setSmallIcon(R.drawable.ic_notification_small)
                .setStyle(messagingStyle)
                .setColor(Color.parseColor(NotificationConstants.BRAND_COLOR))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setShortcutId(chatId)
                .setContentIntent(contentIntent)
                .addAction(actionHelper.buildReplyAction(chatId, notificationId))
                .addAction(actionHelper.buildMarkReadAction(chatId, notificationId))
                .setAutoCancel(true)
                .setGroup(NotificationConstants.GROUP_KEY_MESSAGES)

            notificationManager.notify(notificationId, builder.build())
            summaryManager.updateSummaryNotification()

        } catch (e: Exception) {
            Log.e(TAG, "showConversationNotification failed for chatId=$chatId", e)
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

            val builder = NotificationCompat.Builder(appContext, NotificationConstants.CHANNEL_MESSAGES)
                .setSmallIcon(R.drawable.ic_notification_small)
                .setContentTitle(senderName)
                .setContentText(messageText)
                .setColor(Color.parseColor(NotificationConstants.BRAND_COLOR))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setGroup(NotificationConstants.GROUP_KEY_MESSAGES)

            notificationManager.notify(notificationId, builder.build())
            summaryManager.updateSummaryNotification()
            
        } catch (e: Exception) {
            Log.e(TAG, "showFallbackNotification failed for chatId=$chatId", e)
        }
    }

    companion object {
        private const val TAG = "NotificationHelper"
    }
}
