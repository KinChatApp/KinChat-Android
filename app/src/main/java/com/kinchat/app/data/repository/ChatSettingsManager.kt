package com.kinchat.app.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

class ChatSettingsManager(private val supabaseClient: SupabaseClient) {

    private val currentUserId: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    suspend fun updateLastRead(chatId: String, userId: String): Result<Unit> = runCatching {
        supabaseClient.postgrest["chat_participants"].update(mapOf("last_read_at" to java.time.Instant.now().toString())) { filter { eq("chat_id", chatId) ; eq("user_id", userId) } }
    }

    suspend fun updateChatPinStatus(chatId: String, isPinned: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = currentUserId ?: throw Exception("Unauthorized")
            supabaseClient.postgrest["chat_participants"].update(mapOf("is_pinned" to isPinned)) { filter { eq("chat_id", chatId); eq("user_id", userId) } }
            Unit
        }
    }

    suspend fun updateChatFavoriteStatus(chatId: String, isFavorite: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = currentUserId ?: throw Exception("Unauthorized")
            supabaseClient.postgrest["chat_participants"].update(mapOf("is_favorite" to isFavorite)) { filter { eq("chat_id", chatId); eq("user_id", userId) } }
            Unit
        }
    }

    suspend fun updateChatArchiveStatus(chatId: String, isArchived: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = currentUserId ?: throw Exception("Unauthorized")
            supabaseClient.postgrest["chat_participants"].update(mapOf("is_archived" to isArchived)) { filter { eq("chat_id", chatId); eq("user_id", userId) } }
            Unit
        }
    }

    suspend fun updateChatMuteStatus(chatId: String, isMuted: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = currentUserId ?: throw Exception("Unauthorized")
            supabaseClient.postgrest["chat_participants"].update(mapOf("is_muted" to isMuted)) { filter { eq("chat_id", chatId); eq("user_id", userId) } }
            Unit
        }
    }

    suspend fun updateChatBlockStatus(chatId: String, isBlocked: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = currentUserId ?: throw Exception("Unauthorized")
            val participants = supabaseClient.postgrest["chat_participants"]
                .select { filter { eq("chat_id", chatId); neq("user_id", userId) } }
                .decodeList<JsonObject>()

            val partnerId = participants.firstOrNull()?.get("user_id")?.toString()?.replace("\"", "")
                ?: throw Exception("Partner not found")

            if (isBlocked) {
                supabaseClient.postgrest["user_blocks"].insert(mapOf("blocker_id" to userId, "blocked_id" to partnerId))
            } else {
                supabaseClient.postgrest["user_blocks"].delete { filter { eq("blocker_id", userId); eq("blocked_id", partnerId) } }
            }
            Unit
        }
    }

    suspend fun deleteChatParticipant(chatId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = currentUserId ?: throw Exception("Unauthorized")
            supabaseClient.postgrest["chat_participants"].update(mapOf("is_deleted" to true)) { filter { eq("chat_id", chatId); eq("user_id", userId) } }
            Unit
        }
    }
}
