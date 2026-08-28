package com.kinchat.app.data.repository.chat.sync.realtime

import com.kinchat.app.core.logging.AppLogger
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
        AppLogger.d("RealtimeMessageHandler", "📨 handleReceiptAction() | action=${action::class.simpleName}")
        try {
            when (action) {
                is PostgresAction.Insert -> processReceipt(action.record)
                is PostgresAction.Update -> processReceipt(action.record)
                else -> {}
            }
        } catch (e: Exception) {
            AppLogger.e("RealtimeMessageHandler", "❌ Failed processing receipt action", e)
        }
    }

    private suspend fun processReceipt(record: JsonObject) {
        val messageId = record["message_id"]?.jsonPrimitive?.content ?: return
        val statusStr = record["status"]?.jsonPrimitive?.content?.uppercase() ?: return

        AppLogger.d("RealtimeMessageHandler", "🔄 processReceipt() | messageId=$messageId | status=$statusStr")

        val newStatus = when (statusStr) {
            "READ" -> {
                AppLogger.d("RealtimeMessageHandler", "👁️ READ RECEIPT PARSED | messageId=$messageId")
                MessageStatus.READ
            }
            "DELIVERED" -> MessageStatus.DELIVERED
            else -> null
        }

        if (newStatus != null) {
            AppLogger.d("RealtimeMessageHandler", "💾 Updating Room status | messageId=$messageId | status=$newStatus")
            val affectedRows = chatMessageDao.updateMessageStatus(messageId, newStatus)
            AppLogger.d("RealtimeMessageHandler", "✅ Room UPDATE RESULT | messageId=$messageId | status=$newStatus | affectedRows=$affectedRows")
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
