package com.kinchat.app.core.utils

import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.kinchat.app.domain.model.ChatMessage

/**
 * Handles the construction of complex Notification Styles (e.g., MessagingStyle).
 */
class NotificationStyleBuilder {

    fun buildMessagingStyle(
        senderName: String,
        recentMessages: List<ChatMessage>,
        currentUserId: String,
        userPerson: Person,
        senderPerson: Person
    ): NotificationCompat.MessagingStyle {
        
        // Explicitly storing the builder to avoid Kotlin scope function (apply) type inference issues.
        val style = NotificationCompat.MessagingStyle(userPerson)
            .setConversationTitle(senderName)
            .setGroupConversation(false)

        recentMessages.forEach { msg ->
            val isMe = msg.senderId == currentUserId
            val person = if (isMe) userPerson else senderPerson
            val time = NotificationTimeFormatter.parseTimestampSafe(msg.createdAt)

            val displayContent = msg.content?.takeIf { it.isNotBlank() }
                ?: NotificationConstants.FALLBACK_MEDIA_CONTENT
                
            style.addMessage(displayContent, time, person)
        }

        return style
    }
}
