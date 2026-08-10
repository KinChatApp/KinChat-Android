package com.kinchat.app.data.repository.chat.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface PendingSyncCoordinator {
    fun triggerSync()
}

@Singleton
class PendingSyncCoordinatorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PendingSyncCoordinator {
    override fun triggerSync() {
        SyncTrigger.enqueue(context)
    }
}
