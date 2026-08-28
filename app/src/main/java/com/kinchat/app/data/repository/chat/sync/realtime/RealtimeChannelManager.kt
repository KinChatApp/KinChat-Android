package com.kinchat.app.data.repository.chat.sync.realtime

import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.repository.chat.sync.fetcher.MissedMessageFetcher
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeChannelManager @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val realtimeMessageHandler: RealtimeMessageHandler,
    private val missedMessageFetcher: MissedMessageFetcher
) {
    private val activeChannels = ConcurrentHashMap<String, RealtimeChannel>()
    private val channelJobs = ConcurrentHashMap<String, Job>()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        AppLogger.e(TAG, "Global Realtime Coroutine Error Caught", exception)
    }

    private val realtimeScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)

    @Suppress("DEPRECATION")
    fun startRealtimeListener(chatId: String) {
        if (activeChannels.containsKey(chatId)) {
            AppLogger.d(TAG, "Channel for $chatId is already active. Skipping.")
            return
        }

        AppLogger.d(TAG, "Starting Realtime listener for exactly: $chatId")
        val channel = supabaseClient.channel("${CHANNEL_PREFIX}_$chatId")
        activeChannels[chatId] = channel

        val job = realtimeScope.launch {
            try {
                // ১. Messages টেবিলের রিয়েলটাইম লিসেনার
                val messagesFlow = channel.postgresChangeFlow<PostgresAction>(schema = SCHEMA_PUBLIC) {
                    table = TABLE_MESSAGES
                    filter = "$COLUMN_CHAT_ID=eq.$chatId"
                }

                launch {
                    try {
                        messagesFlow.collect { action ->
                            AppLogger.d(TAG, "Realtime action received for $chatId: ${action::class.simpleName}")
                            realtimeMessageHandler.handleAction(action, chatId)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "Socket error in messages for $chatId", e)
                    }
                }

                // 🚀 FIX: ২. Message Receipts টেবিলের রিয়েলটাইম লিসেনার (Tick Marks এর জন্য)
                val receiptsFlow = channel.postgresChangeFlow<PostgresAction>(schema = SCHEMA_PUBLIC) {
                    table = TABLE_RECEIPTS
                }

                launch {
                    try {
                        receiptsFlow.collect { action ->
                            AppLogger.d(TAG, "📥 RECEIPT EVENT | chatId=$chatId | event=${action::class.simpleName}")
                            realtimeMessageHandler.handleReceiptAction(action)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "❌ RECEIPT CHANNEL ERROR in receipts for $chatId", e)
                    }
                }

                // ৩. Connection Status লিসেনার
                launch {
                    try {
                        channel.status.collect { status ->
                            AppLogger.d(TAG, "Channel status for $chatId is now: $status")
                            val statusStr = status.name.uppercase()
                            if (statusStr == "SUBSCRIBED" || statusStr == "JOINED") {
                                AppLogger.d(TAG, "🔌 RECEIPT CHANNEL SUBSCRIBED | chatId=$chatId")
                                try {
                                    AppLogger.d(TAG, "Reconnected! Fetching any missed messages for $chatId...")
                                    missedMessageFetcher.fetchMissedMessages(chatId)
                                } catch (e: Exception) {
                                    AppLogger.e(TAG, "Failed to fetch missed messages upon reconnect for $chatId", e)
                                }
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "Status collection error for $chatId", e)
                    }
                }

                try {
                    channel.subscribe()
                    AppLogger.d(TAG, "Successfully subscribed to channel: $chatId")
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to subscribe to channel $chatId", e)
                    stopRealtimeListener(chatId)
                }

            } catch (e: CancellationException) {
                AppLogger.d(TAG, "Realtime listener cancelled for $chatId")
                throw e
            } catch (e: Exception) {
                AppLogger.e(TAG, "Realtime Error for chat $chatId", e)
            }
        }

        channelJobs[chatId] = job
    }

    fun stopRealtimeListener(chatId: String) {
        channelJobs.remove(chatId)?.cancel()
        activeChannels.remove(chatId)?.let { channel ->
            realtimeScope.launch {
                try {
                    channel.unsubscribe()
                    AppLogger.d(TAG, "Successfully unsubscribed from channel: $chatId")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Error unsubscribing channel $chatId", e)
                }
            }
        }
    }

    fun stopAllListeners() {
        val channels = activeChannels.keys().toList()
        channels.forEach { stopRealtimeListener(it) }
    }

    fun destroy() {
        stopAllListeners()
        realtimeScope.cancel()
    }

    companion object {
        private const val TAG = "RealtimeChannelManager"
        private const val TABLE_MESSAGES = "messages"
        private const val TABLE_RECEIPTS = "message_receipts"
        private const val SCHEMA_PUBLIC = "public"
        private const val CHANNEL_PREFIX = "chat"
        private const val COLUMN_CHAT_ID = "chat_id"
    }
}
