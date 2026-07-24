package com.kinchat.app.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "drafts",
    primaryKeys = ["chatId", "userId"], // Composite Key
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("chatId")]
)
data class DraftEntity(
    val chatId: String,
    val userId: String,
    val content: String?,
    val replyToId: String? = null,
    val attachmentDraftJson: String? = null, // Serialize if multiple
    val mentionDraftJson: String? = null,
    val editedAt: Long? = null,
    val updatedAt: Long
)
