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
