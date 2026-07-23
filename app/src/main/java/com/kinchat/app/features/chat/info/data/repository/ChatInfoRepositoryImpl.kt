package com.kinchat.app.features.chat.info.data.repository

import com.kinchat.app.features.chat.info.domain.model.ChatInfoSettings
import com.kinchat.app.features.chat.info.domain.model.ChatParticipantDto
import com.kinchat.app.features.chat.info.domain.model.ReportDto
import com.kinchat.app.features.chat.info.domain.model.UserBlockDto
import com.kinchat.app.features.chat.info.domain.model.UserProfile
import com.kinchat.app.features.chat.info.domain.repository.ChatInfoRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatInfoRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : ChatInfoRepository {

    private val currentUserId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    override fun getUserProfile(userId: String): Flow<UserProfile?> = flow {
        // Initial Fetch
        val initialData = try {
            supabase.postgrest["users"]
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<UserProfile>()
        } catch (e: Exception) {
            null
        }
        emit(initialData)

        // Realtime Updates
        try {
            val channel = supabase.channel("public:users:$userId")
            val changeFlow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table = "users"
                filter = "id=eq.$userId"
            }
            channel.subscribe()

            changeFlow.collect { action ->
                // 🚀 FIX: Properly using kotlinx.serialization Json object
                val updatedProfile = action.record?.let { jsonElement ->
                    Json { ignoreUnknownKeys = true }.decodeFromJsonElement<UserProfile>(jsonElement)
                }
                emit(updatedProfile)
            }
        } catch (e: Exception) {
            // Handle realtime failure gracefully
            e.printStackTrace()
        }
    }

    override suspend fun getChatSettings(partnerId: String): Result<ChatInfoSettings> = runCatching {
        val myId = currentUserId ?: throw Exception("User not authenticated")

        // Check Block Status
        val blockData = supabase.postgrest["user_blocks"]
            .select {
                filter {
                    eq("blocker_id", myId)
                    eq("blocked_id", partnerId)
                }
            }.decodeSingleOrNull<UserBlockDto>()
        val isBlocked = blockData != null

        // Find common chat ID
        val chatIds = supabase.postgrest.rpc("get_my_chat_ids").decodeList<String>()
        var activeChatId: String? = null
        var isMuted = false
        var clearedAt: String? = null

        if (chatIds.isNotEmpty()) {
            val partnerParticipant = supabase.postgrest["chat_participants"]
                .select {
                    filter {
                        eq("user_id", partnerId)
                        isIn("chat_id", chatIds)
                    }
                }.decodeSingleOrNull<ChatParticipantDto>()

            if (partnerParticipant != null) {
                activeChatId = partnerParticipant.chat_id

                val myParticipant = supabase.postgrest["chat_participants"]
                    .select {
                        filter {
                            eq("chat_id", activeChatId)
                            eq("user_id", myId)
                        }
                    }.decodeSingleOrNull<ChatParticipantDto>()

                if (myParticipant != null) {
                    isMuted = myParticipant.is_muted
                    clearedAt = myParticipant.cleared_at
                }
            }
        }

        // Fetch Media Count
        var mediaCount = 0
        if (activeChatId != null) {
            val messagesQuery = supabase.postgrest["messages"].select {
                filter {
                    eq("chat_id", activeChatId)
                    isIn("type", listOf("image", "video", "audio", "document"))
                    if (clearedAt != null) {
                        gt("created_at", clearedAt)
                    }
                }
            }
            mediaCount = messagesQuery.data.length
        }

        ChatInfoSettings(
            chatId = activeChatId,
            isMuted = isMuted,
            isBlocked = isBlocked,
            mediaCount = mediaCount
        )
    }

    override suspend fun toggleMute(chatId: String, isMuted: Boolean): Result<Unit> = runCatching {
        val myId = currentUserId ?: return@runCatching
        supabase.postgrest["chat_participants"].update(
            {
                set("is_muted", isMuted)
            }
        ) {
            filter {
                eq("chat_id", chatId)
                eq("user_id", myId)
            }
        }
    }

    override suspend fun toggleBlock(partnerId: String, isBlocked: Boolean): Result<Unit> = runCatching {
        val myId = currentUserId ?: return@runCatching
        if (isBlocked) {
            supabase.postgrest["user_blocks"].insert(
                UserBlockDto(blocker_id = myId, blocked_id = partnerId)
            )
        } else {
            supabase.postgrest["user_blocks"].delete {
                filter {
                    eq("blocker_id", myId)
                    eq("blocked_id", partnerId)
                }
            }
        }
    }

    override suspend fun clearChat(chatId: String): Result<Unit> = runCatching {
        val myId = currentUserId ?: return@runCatching
        val now = Clock.System.now().toString()
        supabase.postgrest["chat_participants"].update(
            {
                set("cleared_at", now)
            }
        ) {
            filter {
                eq("chat_id", chatId)
                eq("user_id", myId)
            }
        }
    }

    override suspend fun reportUser(partnerId: String): Result<Unit> = runCatching {
        val myId = currentUserId ?: return@runCatching
        supabase.postgrest["reports"].insert(
            ReportDto(
                reporter_id = myId,
                reported_user_id = partnerId,
                reason = "User reported via contact info",
                target_type = "user",
                status = "pending"
            )
        )
    }
}
