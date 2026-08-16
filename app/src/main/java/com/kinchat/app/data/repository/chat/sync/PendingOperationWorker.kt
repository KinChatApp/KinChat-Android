package com.kinchat.app.data.repository.chat.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kinchat.app.core.logging.DebugLogger
import com.kinchat.app.data.local.db.PendingOperationDao
import com.kinchat.app.data.repository.chat.sync.handlers.PendingOperationDispatcher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth

@HiltWorker
class PendingOperationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val supabaseClient: SupabaseClient,
    private val pendingOperationDao: PendingOperationDao,
    private val operationDispatcher: PendingOperationDispatcher
) : CoroutineWorker(context, params) {

    private val currentUserId: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    override suspend fun doWork(): Result {
        val pendingOps = pendingOperationDao.getAllPendingOperations()
        DebugLogger.log(
            applicationContext,
            "PendingWorker",
            "doWork triggered, pendingOps=${pendingOps.size}, currentUserId=$currentUserId"
        )

        if (pendingOps.isEmpty()) return Result.success()

        var hasRecoverableFailure = false
        // 🚀 FIX: Track failed referenceIds to guarantee FIFO order across dependent ops (e.g. Send -> Edit -> Delete)
        val failedReferenceIds = mutableSetOf<String>()

        for (op in pendingOps) {
            if (failedReferenceIds.contains(op.referenceId)) {
                Log.d("PendingWorker", "Skipping dependent op ${op.id} due to previous failure on ref ${op.referenceId}")
                continue
            }

            try {
                // Dispatch the operation to the appropriate dedicated handler
                operationDispatcher.dispatch(op)

                // If operation was handled successfully, remove it from the pending queue
                pendingOperationDao.deleteOperation(op.id)

            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown exception"
                Log.e("PendingWorker", "Sync Failed: $errorMsg")
                DebugLogger.log(
                    applicationContext,
                    "PendingWorker",
                    "Sync Failed on type=${op.type} ref=${op.referenceId} attempt=${op.attempt}: $errorMsg",
                    e
                )

                failedReferenceIds.add(op.referenceId)

                // 🚀 FIX: Improve error categorization instead of relying solely on string matching
                val isUnrecoverable = e is IllegalArgumentException || errorMsg.contains("invalid input syntax for type uuid")
                
                if (isUnrecoverable) {
                    // Dead letter: Mark as DEAD immediately without further retries
                    val deadOp = op.copy(status = "DEAD", lastError = errorMsg)
                    pendingOperationDao.updateOperation(deadOp)
                } else {
                    // Recoverable error: Increment attempt and check maxAttempts
                    hasRecoverableFailure = true
                    val updatedOp = op.incrementAttempt(errorMsg)
                    pendingOperationDao.updateOperation(updatedOp)
                }
            }
        }

        val finalResult = if (hasRecoverableFailure) Result.retry() else Result.success()
        DebugLogger.log(
            applicationContext,
            "PendingWorker",
            "doWork finished: ${if (hasRecoverableFailure) "RETRY" else "SUCCESS"}"
        )

        return finalResult
    }
}
