package com.kinchat.app.data.local.db

import com.kinchat.app.domain.model.Chat
import com.kinchat.app.domain.model.ChatMessage
import com.kinchat.app.domain.model.MessageAttachment
import com.kinchat.app.domain.model.MessageReaction
import com.kinchat.app.domain.model.TickState
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
        editedAt = this.message.editedAt?.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it).toString() },
        deletedAt = this.message.deletedAt?.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it).toString() },
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
                fileUrl = attachment.fileUrl,
                fileName = attachment.fileName,
                fileSize = attachment.fileSize,
                fileType = attachment.mimeType,
                localUri = attachment.localUri,
                uploadState = attachment.uploadState.name,
                imageKitFileId = attachment.imageKitFileId
            )
        },
        isSending = this.message.status == MessageStatus.PENDING || this.message.status == MessageStatus.SENDING,
        isFailed = this.message.status == MessageStatus.FAILED,
        localStatus = this.message.status.name // 🚀 FIX: Room-এর স্ট্যাটাস ম্যাপ করা হলো
    )
}

fun ChatPreview.toDomainModel(currentUserId: String): Chat {
    val cleanUserId = currentUserId.replace("\"", "").trim()
    val participantInfo = this.participants.firstOrNull { it.userId.replace("\"", "").trim() == cleanUserId }
    val partnerInfo = this.participants.firstOrNull { it.userId.replace("\"", "").trim() != cleanUserId }

    val cleanSenderId = this.lastMessage?.senderId?.replace("\"", "")?.trim()
    val isLastMsgFromMe = cleanSenderId == cleanUserId

    val mappedTickState = if (isLastMsgFromMe) {
        when (this.lastMessage?.status) {
            MessageStatus.PENDING, MessageStatus.SENDING -> TickState.SENDING
            MessageStatus.SENT -> TickState.SENT
            MessageStatus.DELIVERED -> TickState.DELIVERED
            MessageStatus.READ -> TickState.READ
            MessageStatus.FAILED -> TickState.FAILED
            else -> TickState.SENT 
        }
    } else null

    val isSavedChat = partnerInfo == null || partnerInfo.userId.replace("\"", "").trim() == cleanUserId

    return Chat(
        id = this.chat.id,
        name = this.chat.title ?: "Unknown",
        partnerId = partnerInfo?.userId,
        lastMessage = this.lastMessage?.content ?: "Attachment",
        timestamp = this.chat.lastMessageTime ?: this.chat.updatedAt,
        unreadCount = participantInfo?.unreadCount ?: 0,
        avatarUrl = this.chat.avatarUrl,
        isPinned = participantInfo?.isPinned ?: false,
        isFavorite = false,
        isArchived = participantInfo?.isArchived ?: false,
        isMuted = participantInfo?.isMuted ?: false,
        isBlocked = false,
        isLastMessageFromMe = isLastMsgFromMe,
        tickState = mappedTickState,
        isSaved = isSavedChat
    )
}
