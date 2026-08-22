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
import java.io.IOException

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
                
                // 🚀 PRO-FIX: Strict categorization to maintain WorkManager and DB semantics parity
                val isUnrecoverable = e is IllegalArgumentException || errorMsg.contains("invalid input syntax") || errorMsg.contains("400")
                val isNetworkError = e is IOException || errorMsg.contains("timeout", ignoreCase = true) || errorMsg.contains("network", ignoreCase = true) || errorMsg.contains("Failed to connect", ignoreCase = true)

                if (isUnrecoverable) {
                    val deadOp = op.copy(status = "DEAD", lastError = errorMsg)
                    pendingOperationDao.updateOperation(deadOp)
                } else if (isNetworkError) {
                    hasRecoverableFailure = true
                    // Do NOT increment attempt or change state to FAILED.
                    // Keep it PENDING so the DAO fetches it again, but WorkManager will handle the delay via Result.retry().
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
