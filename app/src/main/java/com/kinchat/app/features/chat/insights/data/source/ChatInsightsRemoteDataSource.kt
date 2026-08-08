package com.kinchat.app.features.chat.insights.data.source

import com.kinchat.app.features.chat.insights.data.model.ChatPreviewDto
import com.kinchat.app.features.chat.insights.data.model.ChatUserStatisticsDto
import com.kinchat.app.features.chat.insights.data.model.ExtendedStatsDto
import com.kinchat.app.features.chat.insights.data.model.MessageDto
import com.kinchat.app.features.chat.insights.data.model.ChatInsightsRawData
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class ChatInsightsRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient
) {

    fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }

    suspend fun fetchChatInsightsData(meId: String, friendId: String): ChatInsightsRawData? = coroutineScope {
        val previews = supabase.postgrest.rpc("get_user_chat_previews", mapOf("current_user_id" to meId))
            .decodeList<ChatPreviewDto>()
            
        val chatPreview = previews.find { it.otherUserId == friendId } ?: return@coroutineScope null
        val chatId = chatPreview.chatId
        val friendName = chatPreview.otherUserName ?: "Friend"

        val firstMsgDef = async {
            supabase.postgrest["messages"].select(columns = Columns.list("created_at, sender_id")) {
                filter { eq("chat_id", chatId) }
                order("created_at", Order.ASCENDING)
                limit(1)
            }.decodeSingleOrNull<MessageDto>()
        }

        val lastMsgDef = async {
            supabase.postgrest["messages"].select(columns = Columns.list("created_at")) {
                filter { eq("chat_id", chatId) }
                order("created_at", Order.DESCENDING)
                limit(1)
            }.decodeSingleOrNull<MessageDto>()
        }

        val statsDef = async {
            supabase.postgrest["chat_user_statistics"].select {
                filter { eq("chat_id", chatId) }
            }.decodeList<ChatUserStatisticsDto>()
        }

        val recentMsgsDef = async {
            supabase.postgrest["messages"].select(columns = Columns.list("created_at")) {
                filter { eq("chat_id", chatId) }
                order("created_at", Order.DESCENDING)
                limit(1000)
            }.decodeList<MessageDto>()
        }

        val extendedStatsDef = async {
            try {
                supabase.postgrest.rpc(
                    "get_chat_extended_stats",
                    mapOf("p_chat_id" to chatId, "p_current_user_id" to meId)
                ).decodeSingleOrNull<ExtendedStatsDto>()
            } catch (e: Exception) {
                null
            }
        }

        ChatInsightsRawData(
            chatId = chatId,
            friendName = friendName,
            firstMessage = firstMsgDef.await(),
            lastMessage = lastMsgDef.await(),
            stats = statsDef.await(),
            recentMessages = recentMsgsDef.await(),
            extendedStats = extendedStatsDef.await() ?: ExtendedStatsDto()
        )
    }
}
