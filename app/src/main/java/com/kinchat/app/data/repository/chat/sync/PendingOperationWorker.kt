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
        
        var hasFailure = false
        
        for (op in pendingOps) {
            try {
                // Dispatch the operation to the appropriate dedicated handler
                operationDispatcher.dispatch(op)
                
                // If operation was handled successfully, remove it from the pending queue
                pendingOperationDao.deleteOperation(op.id)
                
            } catch (e: Exception) {
                val errorMsg = e.message ?: ""
                Log.e("PendingWorker", "Sync Failed: $errorMsg")
                DebugLogger.log(
                    applicationContext,
                    "PendingWorker",
                    "Sync Failed on type=${op.type} ref=${op.referenceId} retryCount=${op.retryCount}: $errorMsg",
                    e
                )

                if (errorMsg.contains("invalid input syntax for type uuid")) {
                    // Unrecoverable UUID format error; discard the operation
                    pendingOperationDao.deleteOperation(op.id)
                } else {
                    // Recoverable error, increment retry count and mark for retry
                    hasFailure = true
                    val updatedOp = op.incrementRetryCount()
                    pendingOperationDao.updateOperation(updatedOp)
                }
            }
        }

        val finalResult = if (hasFailure) Result.retry() else Result.success()
        DebugLogger.log(
            applicationContext, 
            "PendingWorker", 
            "doWork finished: ${if (hasFailure) "RETRY" else "SUCCESS"}"
        )
        
        return finalResult
    }
}
