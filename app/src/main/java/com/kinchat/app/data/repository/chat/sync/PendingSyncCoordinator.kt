package com.kinchat.app.data.repository.chat.sync

import android.content.Context
import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.core.network.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface PendingSyncCoordinator {
    fun triggerSync()
    fun startMonitoring() // 🚀 Added to start network observation
}

@Singleton
class PendingSyncCoordinatorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkMonitor: NetworkMonitor // 🚀 Added NetworkMonitor
) : PendingSyncCoordinator {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var debounceJob: Job? = null
    private var isMonitoring = false

    override fun triggerSync() {
        // 🚀 FIXED (Phase 5): Added debounce to prevent spamming WorkManager replacement
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(500) // 500ms debounce
            AppLogger.d("SyncCoordinator", "Triggering sync worker...")
            SyncTrigger.enqueue(context)
        }
    }

    override fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        scope.launch {
            networkMonitor.isOnline.collectLatest { isOnline ->
                AppLogger.d("SyncCoordinator", "Network state changed: isOnline=$isOnline")
                if (isOnline) {
                    triggerSync()
                }
            }
        }
    }
}
