package com.kinchat.app.data.repository

import android.util.Log
import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.local.db.ChatMessageEntity
import com.kinchat.app.data.local.db.MessageStatus
import com.kinchat.app.data.local.db.MessageType
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
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val currentUserId: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    suspend fun fetchMissedMessages(chatId: String) {
        withContext(Dispatchers.IO) {
            try {
                val lastSyncTimeEpoch = chatMessageDao.getLastMessageTimestamp(chatId) ?: 0L
                val lastSyncIso = Instant.ofEpochMilli(lastSyncTimeEpoch).toString()

                val rawJsonArray = supabaseClient.postgrest["messages"]
                    .select {
                        filter {
                            eq("chat_id", chatId)
                            gt("created_at", lastSyncIso)
                        }
                    }
                    .decodeList<JsonObject>()

                if (rawJsonArray.isNotEmpty()) {
                    val entities = rawJsonArray.mapNotNull { parseJsonToEntity(it, chatId) }
                    chatMessageDao.insertMessages(entities)
                }
                // 🚀 Fix: if-expression error সমাধান করার জন্য
                Unit 
            } catch (e: Exception) {
                Log.e("ChatSyncManager", "Delta Sync Error: ${e.message}")
            }
        }
    }

    fun startRealtimeListener(chatId: String) {
        if (activeChannels.containsKey(chatId)) {
            Log.d("ChatSyncManager", "Channel for $chatId is already active. Skipping.")
            return
        }

        val channel = supabaseClient.channel("chat_$chatId")
        activeChannels[chatId] = channel

        scope.launch {
            try {
                val messagesFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "messages"
                    filter = "chat_id=eq.$chatId"
                }

                channel.subscribe()

                messagesFlow.collect { action ->
                    when (action) {
                        is PostgresAction.Insert -> {
                            val record = action.record
                            if (record["sender_id"]?.jsonPrimitive?.content != currentUserId) {
                                parseJsonToEntity(record, chatId)?.let {
                                    chatMessageDao.insertMessage(it)
                                }
                            }
                        }
                        is PostgresAction.Update -> {
                            parseJsonToEntity(action.record, chatId)?.let {
                                chatMessageDao.insertMessage(it)
                            }
                        }
                        is PostgresAction.Delete -> {
                            val deletedId = action.oldRecord["id"]?.jsonPrimitive?.content
                            if (deletedId != null) {
                                chatMessageDao.softDeleteMessage(deletedId, System.currentTimeMillis())
                            }
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatSyncManager", "Realtime Error: ${e.message}")
            }
        }
    }

    fun stopRealtimeListener(chatId: String) {
        activeChannels[chatId]?.let { channel ->
            scope.launch { channel.unsubscribe() }
            activeChannels.remove(chatId)
        }
    }

    private fun parseJsonToEntity(jsonObj: JsonObject, fallbackChatId: String): ChatMessageEntity? {
        return try {
            val createdAtStr = jsonObj["created_at"]?.jsonPrimitive?.content ?: return null
            val createdAtEpoch = Instant.parse(createdAtStr).toEpochMilli()

            ChatMessageEntity(
                id = jsonObj["id"]?.jsonPrimitive?.content ?: return null,
                chatId = jsonObj["chat_id"]?.jsonPrimitive?.content ?: fallbackChatId,
                senderId = jsonObj["sender_id"]?.jsonPrimitive?.content ?: return null,
                content = jsonObj["content"]?.jsonPrimitive?.content,
                type = MessageType.valueOf(jsonObj["type"]?.jsonPrimitive?.content ?: "text"),
                status = MessageStatus.DELIVERED,
                replyToId = jsonObj["reply_to_id"]?.jsonPrimitive?.content,
                createdAt = createdAtEpoch,
                isForwarded = jsonObj["is_forwarded"]?.jsonPrimitive?.content?.toBoolean() ?: false, // 🚀 Fix: boolean error সমাধান
                metadataJson = jsonObj["metadata"]?.toString()
            )
        } catch (e: Exception) {
            Log.e("ChatSyncManager", "Parse error", e)
            null
        }
    }
}
