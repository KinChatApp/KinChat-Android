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
        currentUserId: String
    ): DashboardSyncResult {
        val chats = mutableListOf<ChatEntity>()
        val participants = mutableListOf<ChatParticipantEntity>()
        val messages = mutableListOf<ChatMessageEntity>()

        val currentTime = System.currentTimeMillis()

        dtos.forEach { dto ->
            val timestamp = DashboardDateUtils.parseTimestamp(dto.last_message_time)
            val dummyMsgId = "${DashboardConstants.DUMMY_MSG_PREFIX}${dto.chat_id}${DashboardConstants.DUMMY_MSG_SUFFIX}"

            chats.add(
                ChatEntity(
                    id = dto.chat_id,
                    title = dto.other_user_name,
                    isGroup = false,
                    avatarUrl = dto.other_user_avatar,
                    lastMessageId = dummyMsgId,
                    lastMessageTime = timestamp,
                    createdBy = null,
                    createdAt = null,
                    updatedAt = currentTime
                )
            )

            // Current User Participant
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

            // 🚀 FIX: Partner Participant 
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

            if (!dto.last_message_content.isNullOrBlank()) {
                messages.add(
                    ChatMessageEntity(
                        id = dummyMsgId,
                        chatId = dto.chat_id,
                        senderId = dto.last_message_sender ?: DashboardConstants.SENDER_UNKNOWN, // 🚀 FIX: আসল সেন্ডার আইডি বসানো হলো
                        content = dto.last_message_content,
                        type = MessageType.text,
                        status = MessageStatus.DELIVERED,
                        createdAt = timestamp,
                        isDeletedForMe = false // 🚀 FIX: dummy মেসেজ হাইড করা ছিল, তা false করা হলো
                    )
                )
            }
        }

        return DashboardSyncResult(chats, participants, messages)
    }
}
