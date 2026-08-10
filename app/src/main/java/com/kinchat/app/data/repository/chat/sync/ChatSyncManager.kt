package com.kinchat.app.data.repository.chat.sync

import com.kinchat.app.data.repository.chat.sync.fetcher.MissedMessageFetcher
import com.kinchat.app.data.repository.chat.sync.realtime.RealtimeChannelManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facade coordinator for chat synchronization.
 * Delegates historical fetch and realtime listening to respective components
 * to maintain Clean Architecture and Single Responsibility Principle.
 */
@Singleton
class ChatSyncManager @Inject constructor(
    private val missedMessageFetcher: MissedMessageFetcher,
    private val realtimeChannelManager: RealtimeChannelManager
) {
    suspend fun fetchMissedMessages(chatId: String) {
        missedMessageFetcher.fetchMissedMessages(chatId)
    }

    fun startRealtimeListener(chatId: String) {
        realtimeChannelManager.startRealtimeListener(chatId)
    }

    fun stopRealtimeListener(chatId: String) {
        realtimeChannelManager.stopRealtimeListener(chatId)
    }

    fun stopAllListeners() {
        realtimeChannelManager.stopAllListeners()
    }

    fun destroy() {
        realtimeChannelManager.destroy()
    }
}
