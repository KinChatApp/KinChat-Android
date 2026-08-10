package com.kinchat.app.core.notifications.builder

import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.kinchat.app.R

/**
 * Factory class responsible for constructing [NotificationCompat.Builder] instances.
 * Separates the visual construction logic from the notification orchestration logic.
 */
class NotificationBuilderFactory(private val context: Context) {

    fun createConversationBuilder(
        chatId: String,
        messagingStyle: NotificationCompat.MessagingStyle,
        contentIntent: PendingIntent,
        replyAction: NotificationCompat.Action,
        markReadAction: NotificationCompat.Action
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, NotificationConstants.CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setStyle(messagingStyle)
            .setColor(Color.parseColor(NotificationConstants.BRAND_COLOR))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setShortcutId(chatId)
            .setContentIntent(contentIntent)
            .addAction(replyAction)
            .addAction(markReadAction)
            .setAutoCancel(true)
            .setGroup(NotificationConstants.GROUP_KEY_MESSAGES)
    }

    fun createFallbackBuilder(
        senderName: String,
        messageText: String,
        contentIntent: PendingIntent
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, NotificationConstants.CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle(senderName)
            .setContentText(messageText)
            .setColor(Color.parseColor(NotificationConstants.BRAND_COLOR))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setGroup(NotificationConstants.GROUP_KEY_MESSAGES)
    }
}
