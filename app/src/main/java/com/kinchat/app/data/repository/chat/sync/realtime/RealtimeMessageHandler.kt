package com.kinchat.app.data.repository.chat.sync.realtime

import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.local.db.MessageStatus
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
    private val currentUserId: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    suspend fun handleAction(action: PostgresAction, chatId: String) {
        when (action) {
            is PostgresAction.Insert -> handleInsert(action.record, chatId)
            is PostgresAction.Update -> handleUpdate(action.record, chatId)
            is PostgresAction.Delete -> handleDelete(action.oldRecord)
            else -> {}
        }
    }

    private suspend fun handleInsert(record: JsonObject, chatId: String) {
        // Echo fix: Still process own messages through merge to confirm server delivery
        ChatSyncMapper.mapJsonToEntity(record, chatId)?.let { entity ->
            chatMessageDao.upsertMessageMerged(entity) // 🚀 FIX: Use merge instead of overwrite
        }
    }

    private suspend fun handleUpdate(record: JsonObject, chatId: String) {
        ChatSyncMapper.mapJsonToEntity(record, chatId)?.let { entity ->
            chatMessageDao.upsertMessageMerged(entity) // 🚀 FIX: Use merge instead of overwrite
        }
    }

    private suspend fun handleDelete(oldRecord: JsonObject) {
        val deletedId = oldRecord[COLUMN_ID]?.jsonPrimitive?.content
        if (deletedId != null) {
            val localMsg = chatMessageDao.getMessageById(deletedId)
            // 🚀 FIX: Don't soft-delete if it's currently pending/sent locally
            if (localMsg != null && localMsg.status != MessageStatus.PENDING && localMsg.status != MessageStatus.SENT) {
                chatMessageDao.softDeleteMessage(deletedId, System.currentTimeMillis())
            }
        }
    }

    companion object {
        private const val COLUMN_ID = "id"
    }
}
