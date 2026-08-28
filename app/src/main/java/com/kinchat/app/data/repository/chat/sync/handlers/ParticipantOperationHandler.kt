package com.kinchat.app.data.repository.chat.sync.handlers

import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.local.db.MessageStatus
import com.kinchat.app.data.local.db.OperationType
import com.kinchat.app.data.local.db.PendingOperationEntity
import com.kinchat.app.data.repository.chat.sync.models.WorkerChatParticipantUpdateDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import org.json.JSONObject
import javax.inject.Inject

@Serializable
private data class ChatInsertDto(val id: String, val created_by: String, val is_group: Boolean)

@Serializable
private data class ChatParticipantInsertDto(val chat_id: String, val user_id: String, val role: String)

@Serializable
private data class BlockInsertDto(val blocker_id: String, val blocked_id: String)

@Serializable
private data class ReportInsertDto(val reporter_id: String, val reported_user_id: String, val message_id: String, val reason: String)

@Serializable
private data class ReceiptInsertDto(val message_id: String, val user_id: String, val status: String)

class ParticipantOperationHandler @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val chatMessageDao: ChatMessageDao
) {
    suspend fun handle(op: PendingOperationEntity) {
        when (op.type) {
            OperationType.UPDATE_CHAT_PIN -> syncParticipantField(op.referenceId, WorkerChatParticipantUpdateDto(is_pinned = op.payloadJson?.toBoolean()))
            OperationType.UPDATE_CHAT_MUTE -> syncParticipantField(op.referenceId, WorkerChatParticipantUpdateDto(is_muted = op.payloadJson?.toBoolean()))
            OperationType.UPDATE_CHAT_ARCHIVE -> syncParticipantField(op.referenceId, WorkerChatParticipantUpdateDto(is_archived = op.payloadJson?.toBoolean()))
            OperationType.UPDATE_CHAT_HIDDEN -> syncParticipantField(op.referenceId, WorkerChatParticipantUpdateDto(is_deleted = op.payloadJson?.toBoolean()))
            OperationType.UPDATE_LAST_READ -> handleUpdateLastRead(op)
            OperationType.CREATE_CHAT -> handleCreateChat(op)
            OperationType.UPDATE_CHAT_FAVORITE -> syncParticipantField(op.referenceId, WorkerChatParticipantUpdateDto(is_favorite = op.payloadJson?.toBoolean()))
            OperationType.UPDATE_CHAT_BLOCK -> handleUpdateBlock(op)
            OperationType.REPORT_MESSAGE -> handleReportMessage(op)
            else -> { /* Handled by other dispatchers */ }
        }
    }

    private suspend fun handleCreateChat(op: PendingOperationEntity) {
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: return
        val payload = JSONObject(op.payloadJson ?: "{}")
        val partnerId = payload.optString("partner_id")

        if (partnerId.isNotEmpty()) {
            try {
                supabaseClient.postgrest["chats"].insert(
                    ChatInsertDto(id = op.referenceId, created_by = currentUserId, is_group = false)
                )
            } catch (e: Exception) {
                val errorMsg = e.message ?: ""
                if (!errorMsg.contains("duplicate key value", ignoreCase = true) && !errorMsg.contains("23505")) {
                    throw e
                }
            }

            try {
                val participants = listOf(
                    ChatParticipantInsertDto(chat_id = op.referenceId, user_id = currentUserId, role = "member"),
                    ChatParticipantInsertDto(chat_id = op.referenceId, user_id = partnerId, role = "member")
                )
                supabaseClient.postgrest["chat_participants"].insert(participants)
            } catch (e: Exception) {
                val errorMsg = e.message ?: ""
                if (!errorMsg.contains("duplicate key value", ignoreCase = true) && !errorMsg.contains("23505")) {
                    throw e
                }
            }
        }
    }

    private suspend fun handleUpdateBlock(op: PendingOperationEntity) {
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: return
        val isBlocked = op.payloadJson?.toBoolean() ?: false

        if (isBlocked) {
            supabaseClient.postgrest["user_blocks"].insert(
                BlockInsertDto(blocker_id = currentUserId, blocked_id = op.referenceId)
            )
        } else {
            supabaseClient.postgrest["user_blocks"].delete {
                filter {
                    eq("blocker_id", currentUserId)
                    eq("blocked_id", op.referenceId)
                }
            }
        }
    }

    private suspend fun handleReportMessage(op: PendingOperationEntity) {
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: return
        val payload = JSONObject(op.payloadJson ?: "{}")

        supabaseClient.postgrest["reports"].insert(
            ReportInsertDto(
                reporter_id = currentUserId,
                reported_user_id = payload.optString("reported_user"),
                message_id = op.referenceId,
                reason = payload.optString("reason")
            )
        )
    }

    private suspend fun handleUpdateLastRead(op: PendingOperationEntity) {
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: return
        val timestampMillis = op.payloadJson?.toLongOrNull()

        if (timestampMillis != null) {
            val isoTime = java.time.Instant.ofEpochMilli(timestampMillis).toString()
            syncParticipantField(op.referenceId, WorkerChatParticipantUpdateDto(last_read_at = isoTime))
        }

        val chatId = op.referenceId
        val unreadIds = chatMessageDao.getUnreadMessageIdsFromPartner(chatId, currentUserId, MessageStatus.READ)
        
        // 🚀 FIX: Prevent Postgres UUID Cast Error by filtering synthetics just in case
        val validUnreadIds = unreadIds.filterNot { it.startsWith("msg_") && it.endsWith("_last") }

        // 🚀 FIX: Remote FIRST logic
        if (validUnreadIds.isNotEmpty()) {
            val receipts = validUnreadIds.map { msgId ->
                ReceiptInsertDto(
                    message_id = msgId,
                    user_id = currentUserId,
                    status = "read"
                )
            }

            try {
                supabaseClient.postgrest["message_receipts"].upsert(receipts)
                chatMessageDao.markMessagesAsReadLocal(validUnreadIds, MessageStatus.READ)
            } catch (e: Exception) {
                val errorMsg = e.message ?: ""
                if (!errorMsg.contains("duplicate key value", ignoreCase = true) && !errorMsg.contains("23505")) {
                    throw e
                }
                // Duplicate means the remote receipt is effectively synced.
                chatMessageDao.markMessagesAsReadLocal(validUnreadIds, MessageStatus.READ)
            }
        }
    }

    private suspend fun syncParticipantField(chatId: String, updateDto: WorkerChatParticipantUpdateDto) {
        val userId = supabaseClient.auth.currentUserOrNull()?.id
        if (userId != null) {
            supabaseClient.postgrest["chat_participants"].update(updateDto) {
                filter {
                    eq("chat_id", chatId)
                    eq("user_id", userId)
                }
            }
        }
    }
}
