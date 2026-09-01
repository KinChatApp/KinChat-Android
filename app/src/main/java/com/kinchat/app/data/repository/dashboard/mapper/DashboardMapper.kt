package com.kinchat.app.data.repository.dashboard.mapper

import com.kinchat.app.data.local.db.ChatEntity
import com.kinchat.app.data.local.db.ChatMessageEntity
import com.kinchat.app.data.local.db.ChatParticipantEntity
import com.kinchat.app.data.local.db.MessageStatus
import com.kinchat.app.data.local.db.MessageType
import com.kinchat.app.data.remote.model.ChatPreviewDto
import com.kinchat.app.data.repository.dashboard.utils.DashboardConstants
import com.kinchat.app.data.repository.dashboard.utils.DashboardDateUtils

internal data class DashboardSyncResult(
    val chats: List<ChatEntity>,
    val participants: List<ChatParticipantEntity>,
    val messages: List<ChatMessageEntity>
)

internal object DashboardMapper {
    fun mapPreviewsToEntities(
        dtos: List<ChatPreviewDto>,
        currentUserId: String,
        realLastMessages: Map<String, ChatMessageEntity>
    ): DashboardSyncResult {
        val chats = mutableListOf<ChatEntity>()
        val participants = mutableListOf<ChatParticipantEntity>()
        val messages = mutableListOf<ChatMessageEntity>()

        val currentTime = System.currentTimeMillis()

        dtos.forEach { dto ->
            val timestamp = DashboardDateUtils.parseTimestamp(dto.last_message_time)
            val realMsg = realLastMessages[dto.chat_id]

            // 🚀 FIX: Verify real message by matching EXACT CONTENT or close timestamp.
            // This prevents replacing a real message with a dummy just because timezones skewed the timestamp.
            val contentMatches = realMsg != null && dto.last_message_content != null && realMsg.content?.trim() == dto.last_message_content.trim()
            val timeMatches = realMsg != null && Math.abs(realMsg.createdAt - timestamp) < 60000

            val shouldUseRealMsg = contentMatches || timeMatches

            val finalMsgId = if (shouldUseRealMsg) {
                realMsg!!.id
            } else {
                "${DashboardConstants.DUMMY_MSG_PREFIX}${dto.chat_id}${DashboardConstants.DUMMY_MSG_SUFFIX}"
            }

            chats.add(
                ChatEntity(
                    id = dto.chat_id,
                    title = dto.other_user_name,
                    isGroup = false,
                    avatarUrl = dto.other_user_avatar,
                    lastMessageId = finalMsgId,
                    lastMessageTime = if (shouldUseRealMsg) realMsg!!.createdAt else timestamp,
                    createdBy = null,
                    createdAt = null,
                    updatedAt = currentTime
                )
            )

            participants.add(
                ChatParticipantEntity(
                    chatId = dto.chat_id,
                    userId = currentUserId,
                    role = DashboardConstants.ROLE_MEMBER,
                    joinedAt = currentTime,
                    lastReadAt = null,
                    clearedAt = null,
                    unreadCount = dto.unread_count ?: 0,
                    isPinned = dto.is_pinned ?: false,
                    isMuted = dto.is_muted ?: false,
                    isArchived = dto.is_archived ?: false,
                    isHidden = false,
                    isLocked = false
                )
            )

            if (!dto.other_user_id.isNullOrBlank() && dto.other_user_id != currentUserId) {
                participants.add(
                    ChatParticipantEntity(
                        chatId = dto.chat_id,
                        userId = dto.other_user_id,
                        role = DashboardConstants.ROLE_MEMBER,
                        joinedAt = currentTime,
                        lastReadAt = null,
                        clearedAt = null,
                        unreadCount = 0,
                        isPinned = false,
                        isMuted = false,
                        isArchived = false,
                        isHidden = false,
                        isLocked = false
                    )
                )
            }

            // ONLY create dummy if we don't have the real message
            if (!shouldUseRealMsg && !dto.last_message_content.isNullOrBlank()) {
                messages.add(
                    ChatMessageEntity(
                        id = finalMsgId,
                        chatId = dto.chat_id,
                        senderId = dto.last_message_sender ?: DashboardConstants.SENDER_UNKNOWN,
                        content = dto.last_message_content,
                        type = MessageType.text,
                        status = MessageStatus.DELIVERED,
                        createdAt = timestamp,
                        isDeletedForMe = false
                    )
                )
            }
        }

        return DashboardSyncResult(chats, participants, messages)
    }
}
