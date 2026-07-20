package com.tuktak.app.features.chat.info.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val display_name: String? = null,
    val avatar_url: String? = null,
    val phone: String? = null,
    val bio: String? = null,
    val is_online: Boolean = false,
    val last_seen: String? = null
)

@Serializable
data class ChatParticipantDto(
    val chat_id: String,
    val user_id: String,
    val is_muted: Boolean = false,
    val cleared_at: String? = null
)

@Serializable
data class UserBlockDto(
    val blocker_id: String,
    val blocked_id: String
)

@Serializable
data class ReportDto(
    val reporter_id: String,
    val reported_user_id: String,
    val reason: String,
    val target_type: String,
    val status: String
)

data class ChatInfoSettings(
    val chatId: String? = null,
    val isMuted: Boolean = false,
    val isBlocked: Boolean = false,
    val mediaCount: Int = 0
)
