package com.kinchat.app.data.repository.chat.sync.models

import kotlinx.serialization.Serializable

@Serializable
data class WorkerMessageInsertDto(
    val id: String,
    val chat_id: String,
    val sender_id: String,
    val content: String?,
    val type: String,
    val reply_to_id: String? = null
)

@Serializable
data class WorkerMessageUpdateDto(
    val content: String? = null,
    val edited_at: String? = null,
    val deleted_at: String? = null
)

@Serializable
data class WorkerMessageReactionDto(
    val message_id: String,
    val user_id: String,
    val reaction: String
)

@Serializable
data class WorkerChatParticipantUpdateDto(
    val is_pinned: Boolean? = null,
    val is_muted: Boolean? = null,
    val is_archived: Boolean? = null,
    val is_deleted: Boolean? = null,
    val last_read_at: String? = null
)
