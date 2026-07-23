package com.kinchat.app.data.repository

import android.util.Log
import com.kinchat.app.data.remote.model.ChatPreviewDto
import com.kinchat.app.data.remote.model.UserProfileDto
import com.kinchat.app.domain.model.Chat
import com.kinchat.app.domain.model.UserProfile
import com.kinchat.app.domain.repository.DashboardRepository
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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : DashboardRepository {

    private val safeScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, e ->
        Log.e("DashboardRepo", "SafeScope caught error: ${e.message}")
    })

    override suspend fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }

    override suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            val dto = supabase.postgrest["users"]
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<UserProfileDto>()

            dto?.let { UserProfile(id = it.id, avatarUrl = it.avatarUrl) }
        } catch (e: Exception) {
            Log.e("DashboardRepo", "Error fetching user profile", e)
            null
        }
    }

    override fun getRecentChats(): Flow<List<Chat>> = callbackFlow {
        val currentUserId = supabase.auth.currentUserOrNull()?.id
        if (currentUserId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        suspend fun fetchAndEmitChats() {
            try {
                val dtos = supabase.postgrest.rpc(
                    function = "get_user_chat_previews",
                    parameters = mapOf("current_user_id" to currentUserId)
                ).decodeList<ChatPreviewDto>()

                val chats = dtos.map { dto ->
                    Chat(
                        id = dto.chat_id,
                        name = dto.other_user_name ?: "Unknown",
                        lastMessage = dto.last_message_content,
                        timestamp = parseTimestamp(dto.last_message_time),
                        unreadCount = dto.unread_count ?: 0,
                        avatarUrl = dto.other_user_avatar
                    )
                }.sortedByDescending { it.timestamp }

                trySend(chats)
            } catch (e: Exception) {
                Log.e("DashboardRepo", "Error fetching chats", e)
            }
        }

        launch { fetchAndEmitChats() }

        val channel = supabase.channel("dashboard_updates_$currentUserId")
        val messageFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "messages"
        }

        val realtimeJob = launch {
            try {
                messageFlow.collect {
                    fetchAndEmitChats()
                }
            } catch (e: Exception) {
                Log.e("DashboardRepo", "Realtime collect error", e)
            }
        }

        try {
            channel.subscribe()
        } catch (e: Exception) {
            Log.e("DashboardRepo", "Channel subscribe error", e)
        }

        awaitClose {
            realtimeJob.cancel()
            safeScope.launch {
                try {
                    channel.unsubscribe()
                } catch (e: Exception) {
                    Log.e("DashboardRepo", "Error unsubscribing", e)
                }
            }
        }
    }

    override suspend fun deleteChat(chatId: String): Result<Unit> {
        return try {
            supabase.postgrest["chats"].delete { filter { eq("id", chatId) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseTimestamp(isoString: String?): Long {
        if (isoString == null) return System.currentTimeMillis()
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(isoString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            try {
                val backupFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                backupFormat.timeZone = TimeZone.getTimeZone("UTC")
                backupFormat.parse(isoString)?.time ?: System.currentTimeMillis()
            } catch (ex: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}
