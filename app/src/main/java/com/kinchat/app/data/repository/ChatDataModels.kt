package com.kinchat.app.data.repository

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageInsertPayload(
    @SerialName("chat_id") val chatId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String,
    val type: String,
    @SerialName("reply_to_id") val replyToId: String? = null
)

// 🚀 Edge Function Payload
@Serializable
data class SendMessageRequest(
    val chatId: String,
    val messageId: String, // 🚀 FIX: messageId যুক্ত করা হলো
    val senderId: String,
    val content: String,
    val replyToId: String? = null
)
