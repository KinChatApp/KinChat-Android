package com.kinchat.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val createdAt: String,
    val messageJson: String // API থেকে পাওয়া পুরো JSON অবজেক্ট এখানে স্টোর করা হবে
)
