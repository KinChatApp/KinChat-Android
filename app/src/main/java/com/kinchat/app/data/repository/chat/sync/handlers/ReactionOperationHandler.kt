package com.kinchat.app.data.repository.chat.sync.handlers

import android.util.Log
import com.kinchat.app.data.local.db.OperationType
import com.kinchat.app.data.local.db.PendingOperationEntity
// 🚀 Fixed import for WorkerSyncDtos
import com.kinchat.app.data.repository.chat.sync.models.WorkerMessageReactionDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

class ReactionOperationHandler @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    suspend fun handle(op: PendingOperationEntity) {
        when (op.type) {
            OperationType.ADD_REACTION -> handleAddReaction(op)
            OperationType.REMOVE_REACTION -> handleRemoveReaction(op)
            else -> { /* Ignore other types */ } // FIX: exhaustive when
        }
    }

    private suspend fun handleAddReaction(op: PendingOperationEntity) {
        val (messageId, userId) = splitReactionReferenceId(op.referenceId)
        val reactionValue = op.payloadJson

        if (messageId != null && userId != null && reactionValue != null) {
            val reactionDto = WorkerMessageReactionDto(
                message_id = messageId,
                user_id = userId,
                reaction = reactionValue
            )
            supabaseClient.postgrest["message_reactions"].upsert(reactionDto)
        } else {
            Log.e("PendingWorker", "ADD_REACTION: referenceId parse failed -> ${op.referenceId}")
        }
    }

    private suspend fun handleRemoveReaction(op: PendingOperationEntity) {
        val (messageId, userId) = splitReactionReferenceId(op.referenceId)
        if (messageId != null && userId != null) {
            supabaseClient.postgrest["message_reactions"].delete {
                filter {
                    eq("message_id", messageId)
                    eq("user_id", userId)
                }
            }
        }
    }

    private fun splitReactionReferenceId(referenceId: String): Pair<String?, String?> {
        val parts = referenceId.split("_")
        return if (parts.size == 2) parts[0] to parts[1] else null to null
    }
}
