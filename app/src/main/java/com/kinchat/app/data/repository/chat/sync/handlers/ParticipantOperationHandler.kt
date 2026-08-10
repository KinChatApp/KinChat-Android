package com.kinchat.app.data.repository.chat.sync.handlers

import com.kinchat.app.data.local.db.OperationType
import com.kinchat.app.data.local.db.PendingOperationEntity
// 🚀 Fixed import for WorkerSyncDtos
import com.kinchat.app.data.repository.chat.sync.models.WorkerChatParticipantUpdateDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

class ParticipantOperationHandler @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    suspend fun handle(op: PendingOperationEntity) {
        when (op.type) {
            OperationType.UPDATE_CHAT_PIN -> syncParticipantField(op.referenceId, WorkerChatParticipantUpdateDto(is_pinned = op.payloadJson?.toBoolean()))
            OperationType.UPDATE_CHAT_MUTE -> syncParticipantField(op.referenceId, WorkerChatParticipantUpdateDto(is_muted = op.payloadJson?.toBoolean()))
            OperationType.UPDATE_CHAT_ARCHIVE -> syncParticipantField(op.referenceId, WorkerChatParticipantUpdateDto(is_archived = op.payloadJson?.toBoolean()))
            OperationType.UPDATE_CHAT_HIDDEN -> syncParticipantField(op.referenceId, WorkerChatParticipantUpdateDto(is_deleted = op.payloadJson?.toBoolean()))
            OperationType.UPDATE_LAST_READ -> handleUpdateLastRead(op)
            else -> { /* Ignore other types */ } // FIX: exhaustive when
        }
    }

    private suspend fun handleUpdateLastRead(op: PendingOperationEntity) {
        val timestampMillis = op.payloadJson?.toLongOrNull()
        if (timestampMillis != null) {
            val isoTime = java.time.Instant.ofEpochMilli(timestampMillis).toString()
            syncParticipantField(op.referenceId, WorkerChatParticipantUpdateDto(last_read_at = isoTime))
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
