package com.kinchat.app.data.repository

import android.util.Log
import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.repository.mapper.ChatSyncMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatSyncManager @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val chatMessageDao: ChatMessageDao
) {
    private val activeChannels = ConcurrentHashMap<String, RealtimeChannel>()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, "Global Coroutine Error Caught", exception)
    }

    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)

    private val currentUserId: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    suspend fun fetchMissedMessages(chatId: String) {
        withContext(Dispatchers.IO) {
            try {
                retryWithBackoff {
                    val lastSyncTimeEpoch = chatMessageDao.getLastMessageTimestamp(chatId)

                    val rawJsonArray = supabaseClient.postgrest[TABLE_MESSAGES]
                        .select {
                            filter {
                                eq(COLUMN_CHAT_ID, chatId)
                                lastSyncTimeEpoch?.let {
                                    val lastSyncIso = Instant.ofEpochMilli(it).toString()
                                    gt(COLUMN_CREATED_AT, lastSyncIso)
                                }
                            }
                        }
                        .decodeList<JsonObject>()

                    if (rawJsonArray.isNotEmpty()) {
                        val entities = rawJsonArray.mapNotNull {
                            ChatSyncMapper.mapJsonToEntity(it, chatId)
                        }
                        chatMessageDao.insertMessages(entities)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Delta Sync Error for chat $chatId after retries", e)
            }
        }
    }

    fun startRealtimeListener(chatId: String) {
        if (activeChannels.containsKey(chatId)) {
            Log.d(TAG, "Channel for $chatId is already active. Skipping.")
            return
        }

        val channel = supabaseClient.channel("${CHANNEL_PREFIX}_$chatId")
        activeChannels[chatId] = channel

        syncScope.launch {
            try {
                val messagesFlow = channel.postgresChangeFlow<PostgresAction>(schema = SCHEMA_PUBLIC) {
                    table = TABLE_MESSAGES
                    filter = "$COLUMN_CHAT_ID=eq.$chatId"
                }

                // 🚀 FIX: ফ্লো কালেকশন আগে শুরু করা হলো যেন সাবস্ক্রাইব হওয়ার সাথে সাথে ইভেন্ট রিসিভ করতে পারে
                val collectionJob = launch {
                    messagesFlow.collect { action ->
                        handlePostgresAction(action, chatId)
                    }
                }

                // 🚀 FIX: নেটওয়ার্ক ফেইলিউর হ্যান্ডেল করার জন্য সাবস্ক্রিপশন ট্রাই/ক্যাচ ব্লকে রাখা হলো
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
            syncScope.launch {
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

    // 🚀 FIX: মেমোরি লিক রোধ করার জন্য স্কোপ ক্যান্সেলেশন যোগ করা হলো (LifeCycle Aware)
    fun destroy() {
        stopAllListeners()
        syncScope.cancel()
    }

    private suspend fun handlePostgresAction(action: PostgresAction, chatId: String) {
        when (action) {
            is PostgresAction.Insert -> handleInsert(action.record, chatId)
            is PostgresAction.Update -> handleUpdate(action.record, chatId)
            is PostgresAction.Delete -> handleDelete(action.oldRecord)
            else -> { /* Ignore unhandled Postgres actions */ }
        }
    }

    // 🚀 FIX: Readability বাড়ানোর জন্য লজিক ছোট ফাংশনে বিভক্ত করা হলো
    private suspend fun handleInsert(record: JsonObject, chatId: String) {
        if (record[COLUMN_SENDER_ID]?.jsonPrimitive?.content != currentUserId) {
            ChatSyncMapper.mapJsonToEntity(record, chatId)?.let {
                chatMessageDao.insertMessage(it)
            }
        }
    }

    private suspend fun handleUpdate(record: JsonObject, chatId: String) {
        ChatSyncMapper.mapJsonToEntity(record, chatId)?.let {
            chatMessageDao.insertMessage(it)
        }
    }

    private suspend fun handleDelete(oldRecord: JsonObject) {
        val deletedId = oldRecord[COLUMN_ID]?.jsonPrimitive?.content
        if (deletedId != null) {
            chatMessageDao.softDeleteMessage(deletedId, System.currentTimeMillis())
        }
    }

    // 🚀 FIX: Network Reconnect / Backoff হেল্পার
    private suspend fun <T> retryWithBackoff(
        times: Int = 3,
        initialDelay: Long = 1000L,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) { attempt ->
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Operation failed on attempt ${attempt + 1}, retrying in $currentDelay ms", e)
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong()
            }
        }
        return block() // Last attempt
    }

    companion object {
        private const val TAG = "ChatSyncManager"
        private const val TABLE_MESSAGES = "messages"
        private const val SCHEMA_PUBLIC = "public"
        private const val CHANNEL_PREFIX = "chat"
        
        private const val COLUMN_ID = "id"
        private const val COLUMN_CHAT_ID = "chat_id"
        private const val COLUMN_SENDER_ID = "sender_id"
        private const val COLUMN_CREATED_AT = "created_at"
    }
}
