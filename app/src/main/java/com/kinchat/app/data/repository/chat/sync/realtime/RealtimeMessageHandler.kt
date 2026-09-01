package com.kinchat.app.data.repository.chat.sync.realtime

import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.local.db.ChatMessageEntity
import com.kinchat.app.data.local.db.MessageStatus
import com.kinchat.app.data.repository.chat.sync.mapper.ChatSyncMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeMessageHandler @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val chatMessageDao: ChatMessageDao
) {
    // 🚀 NEW: Memory queue for early arriving receipts
    private val pendingReceipts = ConcurrentHashMap<String, MessageStatus>()

    suspend fun handleAction(action: PostgresAction, chatId: String) {
        when (action) {
            is PostgresAction.Insert -> handleInsert(action.record, chatId)
            is PostgresAction.Update -> handleUpdate(action.record, chatId)
            is PostgresAction.Delete -> handleDelete(action.oldRecord)
            else -> {}
        }
    }

    suspend fun handleReceiptAction(action: PostgresAction) {
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

        val newStatus = when (statusStr) {
            "READ" -> MessageStatus.READ
            "DELIVERED" -> MessageStatus.DELIVERED
            else -> null
        } ?: return

        val currentMessage = chatMessageDao.getMessageById(messageId)

        if (currentMessage != null) {
            val shouldUpdate = when (newStatus) {
                MessageStatus.READ -> true
                MessageStatus.DELIVERED -> currentMessage.status != MessageStatus.READ
                else -> false
            }

            if (shouldUpdate && currentMessage.status != newStatus) {
                val affectedRows = chatMessageDao.updateMessageStatus(messageId, newStatus)
                AppLogger.d("RealtimeMessageHandler", "✅ Status upgraded to $newStatus | messageId=$messageId | rows=$affectedRows")
            }
        } else {
            // 🚀 FIX: Store receipt in memory if it arrives before the actual message
            val existing = pendingReceipts[messageId]
            if (existing != MessageStatus.READ) {
                pendingReceipts[messageId] = newStatus
                AppLogger.d("RealtimeMessageHandler", "⚠️ Buffered early receipt in memory: $messageId -> $newStatus")
            }
        }
    }

    // 🚀 FIX: Use .copy() to create a new instance since entity fields are immutable 'val'
    private fun applyBufferedReceipt(entity: ChatMessageEntity): ChatMessageEntity {
        val bufferedStatus = pendingReceipts.remove(entity.id)
        if (bufferedStatus != null) {
            val shouldApply = when (bufferedStatus) {
                MessageStatus.READ -> true
                MessageStatus.DELIVERED -> entity.status != MessageStatus.READ
                else -> false
            }
            if (shouldApply) {
                AppLogger.d("RealtimeMessageHandler", "✅ Applied buffered receipt to message: ${entity.id} -> $bufferedStatus")
                return entity.copy(status = bufferedStatus)
            }
        }
        return entity
    }

    private suspend fun handleInsert(record: JsonObject, chatId: String) {
        ChatSyncMapper.mapJsonToEntity(record, chatId)?.let { entity ->
            val finalEntity = applyBufferedReceipt(entity)
            chatMessageDao.upsertMessageMerged(finalEntity)
        }
    }

    private suspend fun handleUpdate(record: JsonObject, chatId: String) {
        ChatSyncMapper.mapJsonToEntity(record, chatId)?.let { entity ->
            val finalEntity = applyBufferedReceipt(entity)
            chatMessageDao.upsertMessageMerged(finalEntity)
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
