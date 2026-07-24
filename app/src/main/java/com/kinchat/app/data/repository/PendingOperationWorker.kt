package com.kinchat.app.data.repository

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.local.db.MessageStatus
import com.kinchat.app.data.local.db.OperationType
import com.kinchat.app.data.local.db.PendingOperationDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

@HiltWorker
class PendingOperationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val supabaseClient: SupabaseClient,
    private val pendingOperationDao: PendingOperationDao,
    private val chatMessageDao: ChatMessageDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pendingOps = pendingOperationDao.getAllPendingOperations()
        if (pendingOps.isEmpty()) return Result.success()

        var hasFailure = false

        for (op in pendingOps) {
            try {
                when (op.type) {
                    OperationType.SEND_MESSAGE -> {
                        // 🚀 FIXED: লোকাল ডিবি থেকে মেসেজ এনে Supabase-এ ইনসার্ট করা হচ্ছে
                        val message = chatMessageDao.getMessageById(op.referenceId)
                        if (message != null) {
                            val messageDto = mapOf(
                                "id" to message.id,
                                "chat_id" to message.chatId,
                                "sender_id" to message.senderId,
                                "content" to message.content,
                                "type" to message.type.name
                            )
                            supabaseClient.postgrest["messages"].insert(messageDto)
                            chatMessageDao.updateMessageStatus(op.referenceId, MessageStatus.SENT)
                        }
                        pendingOperationDao.deleteOperation(op.id)
                    }
                    OperationType.EDIT_MESSAGE -> {
                        val newContent = op.payloadJson
                        if (newContent != null) {
                            supabaseClient.postgrest["messages"]
                                .update(mapOf("content" to newContent, "edited_at" to java.time.Instant.now().toString())) {
                                    filter { eq("id", op.referenceId) }
                                }
                        }
                        pendingOperationDao.deleteOperation(op.id)
                    }
                    OperationType.DELETE_MESSAGE -> {
                        supabaseClient.postgrest["messages"]
                            .update(mapOf("deleted_at" to java.time.Instant.now().toString())) {
                                filter { eq("id", op.referenceId) }
                            }
                        pendingOperationDao.deleteOperation(op.id)
                    }
                    OperationType.ADD_REACTION -> {
                        pendingOperationDao.deleteOperation(op.id)
                    }
                    else -> {
                        pendingOperationDao.deleteOperation(op.id)
                    }
                }
            } catch (e: Exception) {
                Log.e("PendingWorker", "Sync Failed: ${e.message}")
                hasFailure = true
                val updatedOp = op.incrementRetryCount()
                pendingOperationDao.updateOperation(updatedOp)
            }
        }

        return if (hasFailure) Result.retry() else Result.success()
    }
}
