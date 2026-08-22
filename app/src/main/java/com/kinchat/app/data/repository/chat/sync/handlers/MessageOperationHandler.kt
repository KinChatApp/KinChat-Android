package com.kinchat.app.data.repository.chat.sync.handlers

import android.content.Context
import com.kinchat.app.core.logging.DebugLogger
import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.local.db.MessageStatus
import com.kinchat.app.data.local.db.OperationType
import com.kinchat.app.data.local.db.PendingOperationEntity
import com.kinchat.app.data.remote.api.ChatNotificationService
import com.kinchat.app.data.repository.chat.sync.models.WorkerMessageInsertDto
import com.kinchat.app.data.repository.chat.sync.models.WorkerMessageUpdateDto
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

class MessageOperationHandler @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val chatMessageDao: ChatMessageDao,
    private val chatNotificationService: ChatNotificationService,
    @ApplicationContext private val context: Context
) {
    suspend fun handle(op: PendingOperationEntity) {
        when (op.type) {
            OperationType.SEND_MESSAGE -> handleSendMessage(op)
            OperationType.EDIT_MESSAGE -> handleEditMessage(op)
            OperationType.DELETE_MESSAGE -> handleDeleteMessage(op)
            else -> { /* Ignore other types */ }
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
                // 1. Insert message to Supabase
                supabaseClient.postgrest["messages"].insert(messageDto)
                chatMessageDao.updateMessageStatus(op.referenceId, MessageStatus.SENT)
                DebugLogger.log(context, "PendingWorker", "SEND_MESSAGE success for ${op.referenceId}")
                
                // 🚀 PRO-FIX: Trigger Edge Function ONLY after successful DB insert
                try {
                    chatNotificationService.sendNotification(
                        chatId = message.chatId,
                        messageId = message.id,
                        senderId = message.senderId,
                        content = message.content ?: "", // 🚀 FIX: Handle Nullable String Content
                        replyToId = cleanReplyToId
                    )
                } catch (e: Exception) {
                    DebugLogger.log(context, "PendingWorker", "Failed to send notification: ${e.message}")
                }

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
            val offlineEditTime = java.time.Instant.ofEpochMilli(op.createdAt).toString()
            val updateDto = WorkerMessageUpdateDto(
                content = newContent,
                edited_at = offlineEditTime
            )
            supabaseClient.postgrest["messages"].update(updateDto) {
                filter { eq("id", op.referenceId) }
            }
        }
    }

    private suspend fun handleDeleteMessage(op: PendingOperationEntity) {
        val offlineDeleteTime = java.time.Instant.ofEpochMilli(op.createdAt).toString()
        val updateDto = WorkerMessageUpdateDto(
            deleted_at = offlineDeleteTime
        )
        supabaseClient.postgrest["messages"].update(updateDto) {
            filter { eq("id", op.referenceId) }
        }
    }
}
