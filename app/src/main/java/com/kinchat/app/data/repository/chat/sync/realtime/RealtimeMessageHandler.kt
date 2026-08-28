package com.kinchat.app.data.repository.chat.sync.realtime

import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.local.db.MessageStatus
import com.kinchat.app.data.repository.chat.sync.mapper.ChatSyncMapper
import io.github.jan.supabase.SupabaseClient
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

    // 🚀 FIX: Receipts ইভেন্ট হ্যান্ডেল করার নতুন ফাংশন
    suspend fun handleReceiptAction(action: PostgresAction) {
        when (action) {
            is PostgresAction.Insert -> processReceipt(action.record)
            is PostgresAction.Update -> processReceipt(action.record)
            else -> {}
        }
    }

    private suspend fun processReceipt(record: JsonObject) {
        val messageId = record["message_id"]?.jsonPrimitive?.content ?: return
        val statusStr = record["status"]?.jsonPrimitive?.content?.uppercase() ?: return

        // Supabase-এর "delivered" বা "read" স্টেট অনুযায়ী লোকাল ডাটাবেস আপডেট করা
        val newStatus = when (statusStr) {
            "READ" -> MessageStatus.READ
            "DELIVERED" -> MessageStatus.DELIVERED
            else -> null
        }

        if (newStatus != null) {
            chatMessageDao.updateMessageStatus(messageId, newStatus)
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
            chatMessageDao.softDeleteMessage(deletedId, System.currentTimeMillis())
        }
    }

    companion object {
        private const val COLUMN_ID = "id"
    }
}
