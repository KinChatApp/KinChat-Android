package com.kinchat.app.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "message_reactions",
    primaryKeys = ["messageId", "userId"], // Composite Key (No surrogate id)
    foreignKeys = [
        ForeignKey(
            entity = ChatMessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("messageId")]
)
data class MessageReactionEntity(
    val messageId: String,
    val userId: String,
    val reaction: ReactionType,
    val createdAt: Long,
    val isSynced: Boolean = true
)
