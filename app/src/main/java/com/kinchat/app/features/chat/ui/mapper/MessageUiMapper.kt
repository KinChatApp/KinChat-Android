package com.kinchat.app.features.chat.ui.mapper

import com.kinchat.app.R
import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.core.utils.DurationFormatter
import com.kinchat.app.core.utils.FileFormatter
import com.kinchat.app.domain.model.*
import com.kinchat.app.features.chat.ui.models.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

object MessageUiMapper {
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()).withZone(ZoneId.systemDefault())

    fun mapToUiModel(
        entity: ChatMessage,
        currentUserId: String,
        partnerName: String,
        isTopInGroup: Boolean = false,
        showTail: Boolean = true,
        replyMessage: ChatMessage? = null
    ): MessageUiModel {
        val isMe = entity.senderId == currentUserId
        val type = MessageType.from(entity.type ?: "text")

        val isDeletedMessage = entity.deletedAt != null ||
                entity.content?.contains("This message was deleted") == true ||
                entity.content?.contains("You deleted a message") == true ||
                entity.content?.contains("You deleted this message") == true

        val tickState = determineTickState(entity, currentUserId)
        
        // 🚀 DIAGNOSTIC LOG (FIXED: removed invalid entity.status reference)
        if (isMe) {
            AppLogger.d("MessageUiMapper", "🎨 UI MAP | messageId=${entity.id} | localStatus=${entity.localStatus} | tick=$tickState")
        }

        return MessageUiModel(
            id = entity.id ?: "",
            content = entity.content ?: "",
            rawTimestamp = parseTimestamp(entity.createdAt),
            formattedTime = formatTime(entity.createdAt),
            type = type,
            isMe = isMe,
            isTopInGroup = isTopInGroup,
            showTail = showTail,
            status = MessageStatusUiState(
                isDeleted = isDeletedMessage,
                isForwarded = entity.isForwarded == true,
                isEdited = entity.editedAt != null,
                tickState = tickState
            ),
            senderName = if (isMe) "You" else partnerName,
            media = mapMediaState(entity),
            audio = mapAudioState(entity),
            call = mapCallState(entity, isMe),
            reply = mapReplyState(entity, replyMessage, partnerName, currentUserId),
            reactions = mapReactions(entity, currentUserId)
        )
    }

    private fun parseTimestamp(dateStr: String?): Long {
        return try {
            if (dateStr != null) Instant.parse(dateStr).toEpochMilli() else System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun formatTime(dateStr: String?): String {
        return try {
            if (dateStr != null) timeFormatter.format(Instant.parse(dateStr)) else ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun determineTickState(entity: ChatMessage, currentUserId: String): TickState {
        if (entity.isFailed) return TickState.FAILED
        if (entity.isSending) return TickState.SENDING

        when (entity.localStatus?.uppercase()) {
            "READ" -> return TickState.READ
            "DELIVERED" -> return TickState.DELIVERED
            "SENT" -> return TickState.SENT
        }

        val otherReceipts = entity.receipts?.filter { it.userId != entity.senderId } ?: emptyList()
        return when {
            otherReceipts.any { it.status == "read" } -> TickState.READ
            otherReceipts.any { it.status == "delivered" } -> TickState.DELIVERED
            else -> TickState.SENT
        }
    }

    private fun mapMediaState(entity: ChatMessage): MediaUiState? {
        val attachment = entity.attachments?.firstOrNull() ?: return null

        val mediaUrl = attachment.fileUrl?.takeIf { it.isNotBlank() && it != "null" }
            ?: attachment.localUri?.takeIf { it.isNotBlank() && it != "null" }
            ?: ""

        return MediaUiState(
            url = mediaUrl,
            fileName = attachment.fileName ?: "",
            rawSizeBytes = attachment.fileSize,
            formattedSize = FileFormatter.formatSize(attachment.fileSize ?: 0L)
        )
    }

    private fun mapAudioState(entity: ChatMessage): AudioUiState? {
        if (entity.type != "audio") return null
        val duration = entity.metadata?.get("duration")?.jsonPrimitive?.intOrNull ?: 0

        val attachment = entity.attachments?.firstOrNull()
        val url = attachment?.fileUrl?.takeIf { it.isNotBlank() && it != "null" }
            ?: attachment?.localUri?.takeIf { it.isNotBlank() && it != "null" }
            ?: return null

        return AudioUiState(url = url, durationSeconds = duration)
    }

    private fun mapCallState(entity: ChatMessage, isMe: Boolean): CallUiState? {
        val hasCallMeta = entity.metadata?.containsKey("call_type") == true
        val content = entity.content ?: ""
        val isLegacyCall = content.startsWith("📞") || content.contains("Voice call", ignoreCase = true) || content.contains("Video call", ignoreCase = true)

        if (!hasCallMeta && !isLegacyCall) return null

        val callTypeStr = entity.metadata?.get("call_type")?.jsonPrimitive?.contentOrNull ?: if (content.contains("Video", ignoreCase = true)) "video" else "audio"
        val callStatusStr = entity.metadata?.get("call_status")?.jsonPrimitive?.contentOrNull ?: when {
            content.contains("Missed", ignoreCase = true) -> "missed"
            content.contains("Incoming", ignoreCase = true) -> "ringing"
            else -> "ended"
        }

        val type = CallType.from(callTypeStr)
        val status = CallStatus.from(callStatusStr)
        val isVideo = type == CallType.VIDEO
        val isMissedOrFailed = status == CallStatus.MISSED || status == CallStatus.FAILED
        val isIncomingRinging = !isMe && status == CallStatus.RINGING

        val statusTextRes = when (status) {
            CallStatus.MISSED -> if (isVideo) R.string.chat_call_missed_video else R.string.chat_call_missed_voice
            CallStatus.REJECTED -> R.string.chat_call_declined
            CallStatus.CANCELLED -> R.string.chat_call_cancelled
            CallStatus.FAILED -> R.string.chat_call_failed
            CallStatus.RINGING -> if (isMe) R.string.chat_calling else if (isVideo) R.string.chat_incoming_video else R.string.chat_incoming_voice
            else -> if (isVideo) R.string.chat_call_video else R.string.chat_call_voice
        }

        return CallUiState(
            type = type,
            status = status,
            durationSeconds = entity.metadata?.get("duration")?.jsonPrimitive?.intOrNull,
            isMissedOrFailed = isMissedOrFailed,
            isIncomingRinging = isIncomingRinging,
            statusTextRes = statusTextRes
        )
    }

    private fun mapReplyState(entity: ChatMessage, replyMessage: ChatMessage?, partnerName: String, currentUserId: String): ReplyPreviewUiState? {
        val replyId = entity.replyToId?.takeIf { it.isNotBlank() && it != "null" } ?: return null

        if (replyMessage != null) {
            val isReplyMe = replyMessage.senderId == currentUserId
            val senderName = if (isReplyMe) "You" else partnerName

            val previewText = when (replyMessage.type) {
                "image" -> "📷 Photo"
                "video" -> "📹 Video"
                "audio" -> "🎵 Voice Message"
                "document" -> "📄 Document"
                else -> replyMessage.content ?: "Message"
            }

            return ReplyPreviewUiState(
                messageId = replyId,
                senderName = senderName,
                previewText = previewText
            )
        }

        return ReplyPreviewUiState(
            messageId = replyId,
            senderName = "User",
            previewText = "Message"
        )
    }

    private fun mapReactions(entity: ChatMessage, currentUserId: String): List<ReactionUiState> {
        val reactions = entity.messageReactions ?: return emptyList()
        return reactions.groupBy { it.reaction }
            .map { (typeStr, list) ->
                ReactionUiState(
                    type = ReactionType.from(typeStr ?: ""),
                    count = list.size,
                    isSelectedByMe = list.any { it.userId == currentUserId }
                )
            }.sortedByDescending { it.count }
    }
}
