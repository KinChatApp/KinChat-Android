package com.kinchat.app.core.notifications.fcm.model

data class FcmMessagePayload(
    val type: String?,
    val chatId: String,
    val senderName: String,
    val messageText: String,
    val senderId: String,
    val avatarUrl: String?,
    val messageId: String,
    val createdAt: Long? = null
) {
    companion object {
        private const val KEY_TYPE = "type"
        private const val KEY_CHAT_ID = "chat_id"
        private const val KEY_TITLE = "title"
        private const val KEY_SENDER_NAME = "sender_name"
        private const val KEY_BODY = "body"
        private const val KEY_MESSAGE = "message"
        private const val KEY_SENDER_ID = "sender_id"
        private const val KEY_AVATAR_URL = "avatar_url"
        private const val KEY_SENDER_AVATAR = "sender_avatar"
        private const val KEY_MESSAGE_ID = "message_id"
        private const val KEY_CREATED_AT = "created_at"
        
        // 🚀 FIX: Accept both types for backward compatibility during migration
        private val CHAT_TYPES = setOf("chat", "chat_message")

        fun from(data: Map<String, String>): FcmMessagePayload? {
            if (data.isEmpty()) return null

            val type = data[KEY_TYPE]
            if (type !in CHAT_TYPES) return null

            val chatId = data[KEY_CHAT_ID] ?: return null
            val createdAtStr = data[KEY_CREATED_AT]
            val createdAt = createdAtStr?.toLongOrNull()

            return FcmMessagePayload(
                type = type,
                chatId = chatId,
                senderName = data[KEY_TITLE] ?: data[KEY_SENDER_NAME] ?: "Unknown",
                messageText = data[KEY_BODY] ?: data[KEY_MESSAGE] ?: "New message",
                senderId = data[KEY_SENDER_ID] ?: "",
                avatarUrl = data[KEY_AVATAR_URL] ?: data[KEY_SENDER_AVATAR],
                messageId = data[KEY_MESSAGE_ID] ?: System.currentTimeMillis().toString(),
                createdAt = createdAt
            )
        }
    }
}
