package com.kinchat.app.data.local.db

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "message_reactions",
    primaryKeys = ["messageId", "userId"], // Composite Key (No surrogate id)
    indices = [Index("userId"), Index("messageId")]
)
data class MessageReactionEntity(
    val messageId: String,
    val userId: String,
    val reaction: ReactionType,
    val createdAt: Long,
    val isSynced: Boolean = true
)
