package com.tuktak.app.data.repository

import android.util.Log
import com.tuktak.app.domain.model.ChatMessage
import com.tuktak.app.domain.model.MessageReaction
import com.tuktak.app.domain.repository.ChatRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class MessageInsertPayload(
    @SerialName("chat_id") val chatId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String,
    val type: String,
    @SerialName("reply_to_id") val replyToId: String? = null
)

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ChatRepository {

    private val savedMessagesCache = ConcurrentHashMap<String, Boolean>()
    private val partnerNameCache = ConcurrentHashMap<String, String>()
    
    // ক্র্যাশ ঠেকানোর জন্য সেফ স্কোপ
    private val safeScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, e ->
        Log.e("ChatRepository", "SafeScope caught error: ${e.message}")
    })

    private val safeJsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override suspend fun getPartnerName(chatId: String, currentUserId: String): String? {
        val cacheKey = "${chatId}_${currentUserId}"
        partnerNameCache[cacheKey]?.let { return it }

        return try {
            val result = supabaseClient.postgrest.rpc(
                "get_chat_partner_name",
                mapOf("p_chat_id" to chatId, "p_current_user_id" to currentUserId)
            ).decodeAsOrNull<String>()

            result?.let { partnerNameCache[cacheKey] = it }
            result
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error fetching partner name via RPC", e)
            null
        }
    }

    override suspend fun addReaction(messageId: String, userId: String, reactionType: String): Result<Unit> = runCatching {
        val existingReactions = supabaseClient.postgrest["message_reactions"]
            .select { filter { eq("message_id", messageId); eq("user_id", userId) } }
            .decodeList<MessageReaction>()

        if (existingReactions.isNotEmpty()) {
            val firstExisting = existingReactions.first()
            supabaseClient.postgrest["message_reactions"].delete { filter { eq("message_id", messageId); eq("user_id", userId) } }
            if (firstExisting.reaction != reactionType) {
                supabaseClient.postgrest["message_reactions"].insert(mapOf("message_id" to messageId, "user_id" to userId, "reaction" to reactionType))
            }
        } else {
            supabaseClient.postgrest["message_reactions"].insert(mapOf("message_id" to messageId, "user_id" to userId, "reaction" to reactionType))
        }
    }

    override suspend fun checkIsSaved(messageId: String, userId: String): Boolean {
        savedMessagesCache[messageId]?.let { return it }
        return try {
            val result = supabaseClient.postgrest["starred_messages"]
                .select { filter { eq("message_id", messageId) ; eq("user_id", userId) } }
                .decodeSingleOrNull<Any>()
            val isSaved = result != null
            savedMessagesCache[messageId] = isSaved
            isSaved
        } catch (e: Exception) { false }
    }

    override suspend fun toggleSaveMessage(messageId: String, userId: String): Result<Boolean> = runCatching {
        val existing = supabaseClient.postgrest["starred_messages"]
            .select { filter { eq("message_id", messageId) ; eq("user_id", userId) } }
            .decodeSingleOrNull<Map<String, String>>()

        if (existing != null) {
            val id = existing["id"] ?: return@runCatching false
            supabaseClient.postgrest["starred_messages"].delete { filter { eq("id", id) } }
            savedMessagesCache[messageId] = false
            false
        } else {
            supabaseClient.postgrest["starred_messages"].insert(mapOf("message_id" to messageId, "user_id" to userId))
            savedMessagesCache[messageId] = true
            true
        }
    }

    override suspend fun deleteMessage(messageId: String, userId: String, deleteType: String): Result<Unit> = runCatching {
        if (deleteType == "for_me") {
            supabaseClient.postgrest.rpc("delete_message_for_me", mapOf("p_message_id" to messageId, "p_user_id" to userId))
        } else {
            supabaseClient.postgrest["messages"].update(mapOf("deleted_at" to java.time.Instant.now().toString(), "deleted_by" to userId)) { filter { eq("id", messageId) } }
        }
    }

    override suspend fun reportMessage(messageId: String, reporterId: String, reportedUserId: String, reason: String): Result<Unit> = runCatching {
        supabaseClient.postgrest["reports"].insert(mapOf("reporter_id" to reporterId, "reported_user_id" to reportedUserId, "reported_message_id" to messageId, "reason" to reason, "target_type" to "message", "status" to "pending"))
    }

    override suspend fun updateLastRead(chatId: String, userId: String): Result<Unit> = runCatching {
        supabaseClient.postgrest["chat_participants"].update(mapOf("last_read_at" to java.time.Instant.now().toString())) { filter { eq("chat_id", chatId) ; eq("user_id", userId) } }
    }

    override suspend fun sendMessage(chatId: String, senderId: String, content: String, replyToId: String?): Result<Unit> = runCatching {
        val payload = MessageInsertPayload(
            chatId = chatId,
            senderId = senderId,
            content = content,
            type = "text",
            replyToId = replyToId
        )
        supabaseClient.postgrest["messages"].insert(payload)
    }

    override fun observeMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val currentMessages = mutableListOf<ChatMessage>()

        launch {
            try {
                val rawJsonArray = supabaseClient.postgrest["messages"]
                    .select { filter { eq("chat_id", chatId) } }
                    .decodeList<JsonObject>()

                for (jsonObj in rawJsonArray) {
                    try {
                        currentMessages.add(safeJsonParser.decodeFromJsonElement<ChatMessage>(jsonObj))
                    } catch (e: Exception) { Log.e("ChatError", "Parse error: ${e.message}") }
                }
                trySend(currentMessages.toList())
            } catch (e: Exception) {
                Log.e("ChatRepository", "Initial fetch error", e)
            }
        }

        val channel = supabaseClient.channel("realtime_messages_$chatId")
        val messagesFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "messages"
            filter = "chat_id=eq.$chatId"
        }

        val realtimeJob = launch {
            try {
                messagesFlow.collect { action ->
                    when (action) {
                        is PostgresAction.Insert -> {
                            try {
                                val newMsg = safeJsonParser.decodeFromJsonElement<ChatMessage>(action.record)
                                currentMessages.add(newMsg)
                                trySend(currentMessages.toList())
                            } catch (e: Exception) { /* Ignore */ }
                        }
                        is PostgresAction.Update -> {
                            try {
                                val updatedMsg = safeJsonParser.decodeFromJsonElement<ChatMessage>(action.record)
                                val index = currentMessages.indexOfFirst { it.id == updatedMsg.id }
                                if (index != -1) {
                                    currentMessages[index] = updatedMsg
                                    trySend(currentMessages.toList())
                                }
                            } catch (e: Exception) { /* Ignore */ }
                        }
                        is PostgresAction.Delete -> {
                            val deletedId = action.oldRecord["id"]?.toString()?.replace("\"", "")
                            currentMessages.removeAll { it.id == deletedId }
                            trySend(currentMessages.toList())
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatRepository", "WebSocket or Network Error: ${e.message}")
            }
        }

        try {
            channel.subscribe()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Channel subscribe error", e)
        }

        awaitClose {
            realtimeJob.cancel()
            safeScope.launch {
                try {
                    channel.unsubscribe()
                } catch (e: Exception) {
                    Log.e("ChatRepository", "Error unsubscribing", e)
                }
            }
        }
    }
}
