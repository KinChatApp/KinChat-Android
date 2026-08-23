package com.kinchat.app.data.repository.chat.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kinchat.app.core.logging.DebugLogger
import com.kinchat.app.data.local.db.OperationType
import com.kinchat.app.data.local.db.PendingOperationDao
import com.kinchat.app.data.repository.chat.sync.handlers.PendingOperationDispatcher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import java.io.IOException

@HiltWorker
class PendingOperationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val supabaseClient: SupabaseClient,
    private val pendingOperationDao: PendingOperationDao,
    private val operationDispatcher: PendingOperationDispatcher
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val rawOps = pendingOperationDao.getAllPendingOperations()
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id

        // 🚀 FIX: CREATE_CHAT যেন SEND_MESSAGE এর আগে এক্সিকিউট হয় সেজন্য সর্টিং করা হলো
        val pendingOps = rawOps.sortedWith(
            compareBy<com.kinchat.app.data.local.db.PendingOperationEntity> {
                when (it.type) {
                    OperationType.CREATE_CHAT -> 1
                    OperationType.SEND_MESSAGE -> 2
                    else -> 3
                }
            }.thenBy { it.createdAt }
        )

        DebugLogger.log(
            applicationContext,
            "PendingWorker",
            "doWork triggered, pendingOps=${pendingOps.size}, currentUserId=$currentUserId"
        )

        if (pendingOps.isEmpty()) return Result.success()

        var hasRecoverableFailure = false
        val failedReferenceIds = mutableSetOf<String>()

        for (op in pendingOps) {
            if (failedReferenceIds.contains(op.referenceId)) {
                continue
            }

            try {
                operationDispatcher.dispatch(op)
                pendingOperationDao.deleteOperation(op.id)
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown exception"
                failedReferenceIds.add(op.referenceId)
                DebugLogger.log(applicationContext, "PendingWorker", "Operation failed [${op.type}]: $errorMsg")

                val isUnrecoverable = e is IllegalArgumentException || (errorMsg.contains("invalid input syntax") && !errorMsg.contains("foreign key", ignoreCase = true))
                val isNetworkError = e is IOException || errorMsg.contains("timeout", ignoreCase = true) || errorMsg.contains("network", ignoreCase = true) || errorMsg.contains("Failed to connect", ignoreCase = true)

                if (isUnrecoverable) {
                    val deadOp = op.copy(status = "DEAD", lastError = errorMsg)
                    pendingOperationDao.updateOperation(deadOp)
                } else if (isNetworkError) {
                    hasRecoverableFailure = true
                    val networkOp = op.copy(lastError = "Network error: $errorMsg", status = "PENDING")
                    pendingOperationDao.updateOperation(networkOp)
                } else {
                    hasRecoverableFailure = true
                    val updatedOp = op.incrementAttempt(errorMsg)
                    pendingOperationDao.updateOperation(updatedOp)
                }
            }
        }

        return if (hasRecoverableFailure) Result.retry() else Result.success()
    }
}
