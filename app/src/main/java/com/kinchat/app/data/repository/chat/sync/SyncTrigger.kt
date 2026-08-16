package com.kinchat.app.data.repository.chat.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

object SyncTrigger {
    fun enqueue(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<PendingOperationWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "SyncPendingOperations",
            // 🚀 FIX: Prevent cancelling in-flight worker by using KEEP instead of REPLACE
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }
}
