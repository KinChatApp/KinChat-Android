package com.kinchat.app.data.repository.chat.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * 🚀 NEW: PendingOperationWorker ট্রিগার করার কমন লজিক এক জায়গায় আনা হলো।
 * আগে এই লজিক শুধু ChatRepositoryImpl-এর private মেথডে ডুপ্লিকেট ছিল; এখন
 * ChatRepositoryImpl এবং KinChatMessagingService দুটোই একই কনফিগারেশনে
 * sync worker enqueue করতে পারবে (DRY)।
 */
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
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
