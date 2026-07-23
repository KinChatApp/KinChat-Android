package com.kinchat.app.data.repository

import android.util.Log
import com.kinchat.app.domain.model.ChatMessage
import com.kinchat.app.domain.model.MessageReaction
import com.kinchat.app.domain.repository.ChatRepository
import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.local.db.ChatMessageEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val supabaseClient: SupabaseClient,
    private val chatMessageDao: ChatMessageDao // 🚀 Room DAO Inject করা হলো
) : ChatRepository {

    private val savedMessagesCache = ConcurrentHashMap<String, Boolean>()
    private val partnerNameCache = ConcurrentHashMap<String, String>()

    private val safeScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, e ->
        Log.e("ChatRepository", "SafeScope caught error: ${e.message}")
    })

    private val safeJsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val currentUserId: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

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

    // 🚀 Offline-first আর্কিটেকচার ইমপ্লিমেন্ট করা হয়েছে
    override fun observeMessages(chatId: String): Flow<List<ChatMessage>> {
        // ১. ব্যাকগ্রাউন্ডে Supabase থেকে ডেটা সিঙ্ক করা
        safeScope.launch {
            try {
                // সার্ভার থেকে লেটেস্ট চ্যাট লোড করে Room-এ সেভ করা
                val rawJsonArray = supabaseClient.postgrest["messages"]
                    .select { filter { eq("chat_id", chatId) } }
                    .decodeList<JsonObject>()
                
                val entities = rawJsonArray.mapNotNull { jsonObj ->
                    try {
                        val id = jsonObj["id"]?.toString()?.replace("\"", "") ?: return@mapNotNull null
                        val createdAt = jsonObj["created_at"]?.toString()?.replace("\"", "") ?: ""
                        ChatMessageEntity(id, chatId, createdAt, jsonObj.toString())
                    } catch (e: Exception) { null }
                }
                chatMessageDao.insertMessages(entities)

                // রিয়েল-টাইম চ্যানেল দিয়ে লিসেন করা
                val channel = supabaseClient.channel("realtime_messages_$chatId")
                val messagesFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "messages"
                    filter = "chat_id=eq.$chatId"
                }

                messagesFlow.collect { action ->
                    when (action) {
                        is PostgresAction.Insert -> {
                            val id = action.record["id"]?.toString()?.replace("\"", "") ?: return@collect
                            val createdAt = action.record["created_at"]?.toString()?.replace("\"", "") ?: ""
                            chatMessageDao.insertMessage(ChatMessageEntity(id, chatId, createdAt, action.record.toString()))
                        }
                        is PostgresAction.Update -> {
                            val id = action.record["id"]?.toString()?.replace("\"", "") ?: return@collect
                            val createdAt = action.record["created_at"]?.toString()?.replace("\"", "") ?: ""
                            chatMessageDao.insertMessage(ChatMessageEntity(id, chatId, createdAt, action.record.toString()))
                        }
                        is PostgresAction.Delete -> {
                            val deletedId = action.oldRecord["id"]?.toString()?.replace("\"", "")
                            if (deletedId != null) chatMessageDao.deleteMessage(deletedId)
                        }
                        else -> {}
                    }
                }
                channel.subscribe()
            } catch (e: Exception) {
                Log.e("ChatRepository", "Sync Error: ${e.message}")
            }
        }

        // ২. সরাসরি Room ডেটাবেস থেকে Flow রিটার্ন করা
        return chatMessageDao.observeMessages(chatId).map { entities ->
            entities.mapNotNull { entity ->
                try {
                    val jsonObj = safeJsonParser.parseToJsonElement(entity.messageJson)
                    safeJsonParser.decodeFromJsonElement<ChatMessage>(jsonObj)
                } catch (e: Exception) { null }
            }
        }
    }

    override suspend fun createChatIfNotExists(partnerUserId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = currentUserId ?: return@withContext Result.failure(Exception("User not authenticated."))

                val response = supabaseClient.postgrest.rpc(
                    "create_chat_if_not_exists",
                    mapOf("user1_id" to userId, "user2_id" to partnerUserId)
                ).decodeAsOrNull<String>()

                val chatId = response?.replace("\"", "")

                if (!chatId.isNullOrBlank()) {
                    Result.success(chatId)
                } else {
                    Result.failure(Exception("Failed to create chat room."))
                }
            } catch (e: Exception) {
                Log.e("ChatRepositoryImpl", "Error creating chat: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun updateChatPinStatus(chatId: String, isPinned: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = currentUserId ?: throw Exception("Unauthorized")
            supabaseClient.postgrest["chat_participants"].update(mapOf("is_pinned" to isPinned)) {
                filter { eq("chat_id", chatId); eq("user_id", userId) }
            }
            Unit
        }
    }

    override suspend fun updateChatFavoriteStatus(chatId: String, isFavorite: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = currentUserId ?: throw Exception("Unauthorized")
            supabaseClient.postgrest["chat_participants"].update(mapOf("is_favorite" to isFavorite)) {
                filter { eq("chat_id", chatId); eq("user_id", userId) }
            }
            Unit
        }
    }

    override suspend fun updateChatArchiveStatus(chatId: String, isArchived: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = currentUserId ?: throw Exception("Unauthorized")
            supabaseClient.postgrest["chat_participants"].update(mapOf("is_archived" to isArchived)) {
                filter { eq("chat_id", chatId); eq("user_id", userId) }
            }
            Unit
        }
    }

    override suspend fun updateChatMuteStatus(chatId: String, isMuted: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = currentUserId ?: throw Exception("Unauthorized")
            supabaseClient.postgrest["chat_participants"].update(mapOf("is_muted" to isMuted)) {
                filter { eq("chat_id", chatId); eq("user_id", userId) }
            }
            Unit
        }
    }

    override suspend fun updateChatBlockStatus(chatId: String, isBlocked: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = currentUserId ?: throw Exception("Unauthorized")
            val participants = supabaseClient.postgrest["chat_participants"]
                .select { filter { eq("chat_id", chatId); neq("user_id", userId) } }
                .decodeList<JsonObject>()

            val partnerId = participants.firstOrNull()?.get("user_id")?.toString()?.replace("\"", "")
                ?: throw Exception("Partner not found")

            if (isBlocked) {
                supabaseClient.postgrest["user_blocks"].insert(
                    mapOf("blocker_id" to userId, "blocked_id" to partnerId)
                )
            } else {
                supabaseClient.postgrest["user_blocks"].delete {
                    filter { eq("blocker_id", userId); eq("blocked_id", partnerId) }
                }
            }
            Unit
        }
    }

    override suspend fun deleteChatParticipant(chatId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = currentUserId ?: throw Exception("Unauthorized")
            supabaseClient.postgrest["chat_participants"].update(mapOf("is_deleted" to true)) {
                filter { eq("chat_id", chatId); eq("user_id", userId) }
            }
            Unit
        }
    }
}
