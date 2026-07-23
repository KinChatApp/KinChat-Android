package com.kinchat.app.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    @SerialName("id") val id: String,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class ChatDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("last_message") val lastMessage: String? = null,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@kotlinx.serialization.Serializable
data class ChatPreviewDto(
    val chat_id: String,
    val is_blocked: Boolean? = false,
    val is_muted: Boolean? = false,
    val last_message_content: String? = null,
    val last_message_sender: String? = null,
    val last_message_time: String? = null,
    val last_message_type: String? = null,
    val other_user_avatar: String? = null,
    val other_user_id: String? = null,
    val other_user_name: String? = null,
    val unread_count: Int? = 0
)
