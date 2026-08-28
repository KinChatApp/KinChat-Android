package com.kinchat.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject

@Serializable
data class ChatMessage(
    val id: String,
    @SerialName("chat_id") val chatId: String? = null,
    @SerialName("sender_id") val senderId: String? = null,
    val content: String? = null,
    val type: String? = "text",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("deleted_by") val deletedBy: String? = null,
    @SerialName("deleted_for_users") val deletedForUsers: List<String>? = null,
    @SerialName("is_forwarded") val isForwarded: Boolean? = false,
    @SerialName("forwarded_from_id") val forwardedFromId: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
    val metadata: JsonObject? = null,
    val messageReactions: List<MessageReaction>? = null,
    val receipts: List<MessageReceipt>? = null,
    val attachments: List<MessageAttachment>? = null,

    @Transient val isSending: Boolean = false,
    @Transient val isFailed: Boolean = false,
    @Transient val localStatus: String? = null // 🚀 FIX: লোকাল ডাটাবেস স্ট্যাটাস পাস করার জন্য
)

@Serializable
data class MessageReaction(
    val id: String? = null,
    @SerialName("message_id") val messageId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val reaction: String
)

@Serializable
data class MessageReceipt(
    @SerialName("message_id") val messageId: String? = null,
    @SerialName("user_id") val userId: String,
    val status: String,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class MessageAttachment(
    val id: String? = null,
    @SerialName("message_id") val messageId: String? = null,
    @SerialName("file_url") val fileUrl: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("file_size") val fileSize: Long? = null,
    @SerialName("file_type") val fileType: String? = null,
    @SerialName("local_uri") val localUri: String? = null,
    @SerialName("upload_state") val uploadState: String? = null,
    @SerialName("imagekit_file_id") val imageKitFileId: String? = null
)
