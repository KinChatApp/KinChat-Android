package com.tuktak.app.features.chat.ui.mapper

import com.tuktak.app.R
import com.tuktak.app.core.utils.DurationFormatter
import com.tuktak.app.core.utils.FileFormatter
import com.tuktak.app.domain.model.*
import com.tuktak.app.features.chat.ui.models.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object MessageUiMapper {
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()).withZone(ZoneId.systemDefault())

    fun mapToUiModel(
        entity: ChatMessage,
        currentUserId: String,
        partnerName: String,
        isTopInGroup: Boolean = false,
        showTail: Boolean = true
    ): MessageUiModel {
        val isMe = entity.senderId == currentUserId
        val type = MessageType.from(entity.type)

        return MessageUiModel(
            id = entity.id,
            content = entity.content ?: "",
            rawTimestamp = parseTimestamp(entity.createdAt),
            formattedTime = formatTime(entity.createdAt),
            type = type,
            isMe = isMe,
            isTopInGroup = isTopInGroup,
            showTail = showTail,
            status = MessageStatusUiState(
                isDeleted = entity.deletedAt != null,
                isForwarded = entity.isForwarded == true,
                isEdited = entity.editedAt != null,
                tickState = determineTickState(entity, currentUserId)
            ),
            senderName = if (isMe) "You" else partnerName,
            media = mapMediaState(entity),
            audio = mapAudioState(entity),
            call = mapCallState(entity, isMe),
            reply = mapReplyState(entity),
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
        if (entity.isSending) return TickState.SENDING
        val otherReceipts = entity.receipts?.filter { it.userId != entity.senderId } ?: emptyList()
        return when {
            otherReceipts.any { it.status == "read" } -> TickState.READ
            otherReceipts.any { it.status == "delivered" } -> TickState.DELIVERED
            else -> TickState.SENT
        }
    }

    private fun mapMediaState(entity: ChatMessage): MediaUiState? {
        val attachment = entity.attachments?.firstOrNull() ?: return null
        return MediaUiState(
            url = attachment.fileUrl,
            fileName = attachment.fileName,
            rawSizeBytes = attachment.fileSize,
            formattedSize = FileFormatter.formatSize(attachment.fileSize ?: 0L) // Fix: Added ?: 0L
        )
    }

    private fun mapAudioState(entity: ChatMessage): AudioUiState? {
        if (entity.type != "audio") return null
        val meta = entity.metadata as? Map<*, *>
        val duration = meta?.get("duration")?.toString()?.toIntOrNull() ?: 0
        val url = entity.attachments?.firstOrNull()?.fileUrl ?: return null
        return AudioUiState(url = url, durationSeconds = duration)
    }

    private fun mapCallState(entity: ChatMessage, isMe: Boolean): CallUiState? {
        val meta = entity.metadata as? Map<*, *>

        // ১. চেক করুন এটি আসলেই কল কিনা
        val hasCallMeta = meta?.containsKey("call_type") == true
        val content = entity.content ?: ""
        val isLegacyCall = content.startsWith("📞") || content.contains("Voice call", ignoreCase = true) || content.contains("Video call", ignoreCase = true)

        // যদি কল না হয়, তাহলে null রিটার্ন করুন
        if (!hasCallMeta && !isLegacyCall) {
            return null
        }

        // ২. যদি কল হয়, তাহলে ডেটা ম্যাপ করুন
        val callTypeStr = (meta?.get("call_type") as? String) ?: if (content.contains("Video", ignoreCase = true)) "video" else "audio"
        val callStatusStr = (meta?.get("call_status") as? String) ?: when {
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
            durationSeconds = (meta?.get("duration") as? String)?.toIntOrNull(),
            isMissedOrFailed = isMissedOrFailed,
            isIncomingRinging = isIncomingRinging,
            statusTextRes = statusTextRes
        )
    }

    private fun mapReplyState(entity: ChatMessage): ReplyPreviewUiState? {
        val replyId = entity.replyToId ?: return null
        return ReplyPreviewUiState(messageId = replyId, senderName = "User", previewText = "Attachment")
    }

    private fun mapReactions(entity: ChatMessage, currentUserId: String): List<ReactionUiState> {
        val reactions = entity.messageReactions ?: return emptyList()
        return reactions.groupBy { it.reaction }
            .map { (typeStr, list) ->
                ReactionUiState(
                    type = ReactionType.from(typeStr),
                    count = list.size,
                    isSelectedByMe = list.any { it.userId == currentUserId }
                )
            }.sortedByDescending { it.count }
    }
}
