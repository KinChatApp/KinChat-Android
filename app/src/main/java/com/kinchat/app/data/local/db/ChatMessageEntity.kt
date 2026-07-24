package com.kinchat.app.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MessageStatus { PENDING, SENDING, SENT, DELIVERED, READ, FAILED }
enum class MessageType { text, image, audio, video, document }

@Entity(
    tableName = "messages",
    indices = [Index("chatId"), Index("senderId"), Index("status"), Index("createdAt")]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val content: String?, 
    val type: MessageType = MessageType.text,
    val status: MessageStatus = MessageStatus.PENDING,
    
    // Metadata & Relations
    val replyToId: String? = null,
    val forwardedFromId: String? = null,
    val isForwarded: Boolean = false,
    val metadataJson: String? = null, // JSON string for extra data
    
    // Timestamps (Long for fast sorting)
    val createdAt: Long,
    val updatedAt: Long? = null,
    val editedAt: Long? = null,
    val deletedAt: Long? = null,
    
    // Local States
    val isDeletedForMe: Boolean = false,
    val isUploaded: Boolean = true
)
