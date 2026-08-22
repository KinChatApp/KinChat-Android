package com.kinchat.app.data.repository.chat.sync.realtime

import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.repository.chat.sync.mapper.ChatSyncMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

class RealtimeMessageHandler @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val chatMessageDao: ChatMessageDao
) {
    suspend fun handleAction(action: PostgresAction, chatId: String) {
        when (action) {
            is PostgresAction.Insert -> handleInsert(action.record, chatId)
            is PostgresAction.Update -> handleUpdate(action.record, chatId)
            is PostgresAction.Delete -> handleDelete(action.oldRecord)
            else -> {}
        }
    }

    private suspend fun handleInsert(record: JsonObject, chatId: String) {
        ChatSyncMapper.mapJsonToEntity(record, chatId)?.let { entity ->
            chatMessageDao.upsertMessageMerged(entity)
        }
    }

    private suspend fun handleUpdate(record: JsonObject, chatId: String) {
        ChatSyncMapper.mapJsonToEntity(record, chatId)?.let { entity ->
            chatMessageDao.upsertMessageMerged(entity)
        }
    }

    private suspend fun handleDelete(oldRecord: JsonObject) {
        val deletedId = oldRecord[COLUMN_ID]?.jsonPrimitive?.content
        if (deletedId != null) {
            // 🚀 PRO-FIX: Trust the server. If a realtime delete event arrives with an ID we know,
            // we must soft-delete it locally regardless of its local SENDING status, 
            // as this implies the server successfully received it but it was subsequently deleted.
            chatMessageDao.softDeleteMessage(deletedId, System.currentTimeMillis())
        }
    }

    companion object {
        private const val COLUMN_ID = "id"
    }
}
