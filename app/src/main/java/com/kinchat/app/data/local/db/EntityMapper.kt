package com.kinchat.app.data.local.db

import com.kinchat.app.domain.model.Chat
import com.kinchat.app.domain.model.ChatMessage
import com.kinchat.app.domain.model.MessageAttachment
import com.kinchat.app.domain.model.MessageReaction
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.time.Instant

fun MessageWithDetails.toDomainModel(): ChatMessage {
    return ChatMessage(
        id = this.message.id,
        chatId = this.message.chatId,
        senderId = this.message.senderId,
        content = this.message.content,
        type = this.message.type.name,
        createdAt = Instant.ofEpochMilli(this.message.createdAt).toString(),
        editedAt = this.message.editedAt?.let { Instant.ofEpochMilli(it).toString() },
        deletedAt = this.message.deletedAt?.let { Instant.ofEpochMilli(it).toString() },
        isForwarded = this.message.isForwarded,
        forwardedFromId = this.message.forwardedFromId,
        replyToId = this.message.replyToId,
        metadata = this.message.metadataJson?.let { 
            try { Json.decodeFromString<JsonObject>(it) } catch (e: Exception) { null } 
        },
        messageReactions = this.reactions.map { reaction ->
            MessageReaction(
                messageId = reaction.messageId,
                userId = reaction.userId,
                reaction = reaction.reaction.name
            )
        },
        attachments = this.attachments.map { attachment ->
            MessageAttachment(
                id = attachment.id,
                messageId = attachment.messageId,
                fileUrl = attachment.fileUrl ?: attachment.localUri ?: "",
                fileName = attachment.fileName,
                fileSize = attachment.fileSize,
                fileType = attachment.mimeType
            )
        },
        isSending = this.message.status == MessageStatus.PENDING || this.message.status == MessageStatus.SENDING
    )
}

// 🚀 Dashboard-এর জন্য ম্যাপিং
fun ChatPreview.toDomainModel(currentUserId: String): Chat {
    val participantInfo = this.participants.firstOrNull { it.userId == currentUserId }
    
    return Chat(
        id = this.chat.id,
        name = this.chat.title ?: "Unknown",
        lastMessage = this.lastMessage?.content ?: "Attachment",
        timestamp = this.chat.lastMessageTime ?: this.chat.updatedAt,
        unreadCount = participantInfo?.unreadCount ?: 0,
        avatarUrl = this.chat.avatarUrl,
        isPinned = participantInfo?.isPinned ?: false,
        isFavorite = false, 
        isArchived = participantInfo?.isArchived ?: false,
        isMuted = participantInfo?.isMuted ?: false,
        isBlocked = false
    )
}
