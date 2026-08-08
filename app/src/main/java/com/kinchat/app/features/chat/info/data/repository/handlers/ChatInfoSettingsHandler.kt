package com.kinchat.app.features.chat.info.data.repository.handlers

import com.kinchat.app.features.chat.info.domain.model.ChatInfoSettings
import com.kinchat.app.features.chat.info.domain.model.ChatParticipantDto
import com.kinchat.app.features.chat.info.domain.model.UserBlockDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatInfoSettingsHandler @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val currentUserId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    suspend fun getChatSettings(partnerId: String): Result<ChatInfoSettings> = runCatching {
        val myId = currentUserId ?: throw Exception("User not authenticated")

        val blockData = supabase.postgrest["user_blocks"]
            .select {
                filter {
                    eq("blocker_id", myId)
                    eq("blocked_id", partnerId)
                }
            }.decodeSingleOrNull<UserBlockDto>()
        
        val isBlocked = blockData != null
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

    suspend fun toggleMute(chatId: String, isMuted: Boolean): Result<Unit> = runCatching {
        val myId = currentUserId ?: throw Exception("User not authenticated")
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

    suspend fun clearChat(chatId: String): Result<Unit> = runCatching {
        val myId = currentUserId ?: throw Exception("User not authenticated")
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
}
