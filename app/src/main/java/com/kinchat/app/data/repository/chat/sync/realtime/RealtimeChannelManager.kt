package com.kinchat.app.data.repository.chat.sync.realtime

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeChannelManager @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val realtimeMessageHandler: RealtimeMessageHandler
) {
    private val activeChannels = ConcurrentHashMap<String, RealtimeChannel>()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, "Global Realtime Coroutine Error Caught", exception)
    }

    private val realtimeScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)

    @Suppress("DEPRECATION")
    fun startRealtimeListener(chatId: String) {
        if (activeChannels.containsKey(chatId)) {
            Log.d(TAG, "Channel for $chatId is already active. Skipping.")
            return
        }

        val channel = supabaseClient.channel("${CHANNEL_PREFIX}_$chatId")
        activeChannels[chatId] = channel

        realtimeScope.launch {
            try {
                val messagesFlow = channel.postgresChangeFlow<PostgresAction>(schema = SCHEMA_PUBLIC) {
                    table = TABLE_MESSAGES
                    // Suppressed deprecation warning to ensure safe compilation 
                    // without needing exact matching import paths for FilterOperation.
                    filter = "$COLUMN_CHAT_ID=eq.$chatId"
                }

                val collectionJob = launch {
                    messagesFlow.collect { action ->
                        realtimeMessageHandler.handleAction(action, chatId)
                    }
                }

                try {
                    channel.subscribe()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to subscribe to channel $chatId", e)
                    collectionJob.cancel()
                    activeChannels.remove(chatId)
                }

            } catch (e: CancellationException) {
                Log.d(TAG, "Realtime listener cancelled for $chatId")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Realtime Error for chat $chatId", e)
            }
        }
    }

    fun stopRealtimeListener(chatId: String) {
        activeChannels.remove(chatId)?.let { channel ->
            realtimeScope.launch {
                try {
                    channel.unsubscribe()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error unsubscribing channel $chatId", e)
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
        private const val SCHEMA_PUBLIC = "public"
        private const val CHANNEL_PREFIX = "chat"
        private const val COLUMN_CHAT_ID = "chat_id"
    }
}
