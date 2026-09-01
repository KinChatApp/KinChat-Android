package com.kinchat.app.data.repository.dashboard.sync

import android.util.Log
import com.kinchat.app.data.local.db.ChatDao
import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.local.db.ChatMessageEntity
import com.kinchat.app.data.local.db.ChatParticipantDao
import com.kinchat.app.data.remote.model.ChatPreviewDto
import com.kinchat.app.data.repository.dashboard.mapper.DashboardMapper
import com.kinchat.app.data.repository.dashboard.utils.DashboardConstants
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc

internal class DashboardSyncManager(
    private val supabase: SupabaseClient,
    private val chatDao: ChatDao,
    private val chatParticipantDao: ChatParticipantDao,
    private val chatMessageDao: ChatMessageDao
) {
    suspend fun syncDashboardChats(currentUserId: String) {
        try {
            val dtos = supabase.postgrest.rpc(
                function = DashboardConstants.RPC_GET_USER_CHAT_PREVIEWS,
                parameters = mapOf(DashboardConstants.PARAM_CURRENT_USER_ID to currentUserId)
            ).decodeList<ChatPreviewDto>()

            if (dtos.isEmpty()) return

            // 🚀 NEW: Fetch real messages from local DB to resolve status properly
            val realMessagesMap = mutableMapOf<String, ChatMessageEntity>()
            dtos.forEach { dto ->
                val realMsg = chatMessageDao.getLatestRealMessage(dto.chat_id)
                if (realMsg != null) {
                    realMessagesMap[dto.chat_id] = realMsg
                }
            }

            val syncResult = DashboardMapper.mapPreviewsToEntities(dtos, currentUserId, realMessagesMap)

            chatDao.insertChats(syncResult.chats)
            chatParticipantDao.insertParticipants(syncResult.participants)
            chatMessageDao.insertMessages(syncResult.messages)

        } catch (e: Exception) {
            Log.e("DashboardSyncManager", "Sync error", e)
        }
    }
}
