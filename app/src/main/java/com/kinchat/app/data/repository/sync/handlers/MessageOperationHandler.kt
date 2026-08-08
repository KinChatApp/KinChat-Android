package com.kinchat.app.data.repository.sync.handlers

import android.content.Context
import com.kinchat.app.core.utils.DebugLogger
import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.local.db.MessageStatus
import com.kinchat.app.data.local.db.OperationType
import com.kinchat.app.data.local.db.PendingOperationEntity
import com.kinchat.app.data.repository.sync.models.WorkerMessageInsertDto
import com.kinchat.app.data.repository.sync.models.WorkerMessageUpdateDto
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

class MessageOperationHandler @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val chatMessageDao: ChatMessageDao,
    @ApplicationContext private val context: Context
) {
    suspend fun handle(op: PendingOperationEntity) {
        when (op.type) {
            OperationType.SEND_MESSAGE -> handleSendMessage(op)
            OperationType.EDIT_MESSAGE -> handleEditMessage(op)
            OperationType.DELETE_MESSAGE -> handleDeleteMessage(op)
            else -> { /* Ignore other types */ } // FIX: exhaustive when
        }
    }

    private suspend fun handleSendMessage(op: PendingOperationEntity) {
        val message = chatMessageDao.getMessageById(op.referenceId)
        if (message != null) {
            val cleanReplyToId = if (message.replyToId == "null" || message.replyToId?.isBlank() == true) null else message.replyToId

            val messageDto = WorkerMessageInsertDto(
                id = message.id,
                chat_id = message.chatId,
                sender_id = message.senderId,
                content = message.content,
                type = message.type.name,
                reply_to_id = cleanReplyToId
            )

            DebugLogger.log(context, "PendingWorker", "SEND_MESSAGE attempting insert: $messageDto")

            try {
                supabaseClient.postgrest["messages"].insert(messageDto)
                chatMessageDao.updateMessageStatus(op.referenceId, MessageStatus.SENT)
                DebugLogger.log(context, "PendingWorker", "SEND_MESSAGE success for ${op.referenceId}")
            } catch (insertError: Exception) {
                val errMsg = insertError.message ?: ""
                if (errMsg.contains("duplicate key value") || errMsg.contains("messages_pkey")) {
                    DebugLogger.log(context, "PendingWorker", "Message already exists. Marking as SENT.")
                    chatMessageDao.updateMessageStatus(op.referenceId, MessageStatus.SENT)
                } else {
                    throw insertError
                }
            }
        } else {
            DebugLogger.log(context, "PendingWorker", "SEND_MESSAGE: message not found in Room for ${op.referenceId}")
        }
    }

    private suspend fun handleEditMessage(op: PendingOperationEntity) {
        val newContent = op.payloadJson
        if (newContent != null) {
            val updateDto = WorkerMessageUpdateDto(
                content = newContent,
                edited_at = java.time.Instant.now().toString()
            )
            supabaseClient.postgrest["messages"].update(updateDto) {
                filter { eq("id", op.referenceId) }
            }
        }
    }

    private suspend fun handleDeleteMessage(op: PendingOperationEntity) {
        val updateDto = WorkerMessageUpdateDto(
            deleted_at = java.time.Instant.now().toString()
        )
        supabaseClient.postgrest["messages"].update(updateDto) {
            filter { eq("id", op.referenceId) }
        }
    }
}
