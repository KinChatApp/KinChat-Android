package com.kinchat.app.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chats",
    indices = [
        Index("lastMessageTime"),
        Index("updatedAt"),
        Index("lastMessageId")
    ]
)
data class ChatEntity(
    @PrimaryKey val id: String,
    val title: String?, // Null for 1-1 chats
    val avatarUrl: String?, // Null for 1-1 chats
    val isGroup: Boolean,
    val lastMessageId: String?,
    val lastMessageTime: Long?,
    val createdBy: String?,
    val createdAt: Long?,
    val updatedAt: Long
)
