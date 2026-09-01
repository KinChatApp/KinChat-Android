package com.kinchat.app.data.repository.chat.sync.mapper

import android.util.Log
import com.kinchat.app.data.local.db.ChatMessageEntity
import com.kinchat.app.data.local.db.MessageStatus
import com.kinchat.app.data.local.db.MessageType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

object ChatSyncMapper {
    private const val TAG = "ChatSyncMapper"

    private fun JsonObject.getStringOrNull(key: String): String? {
        val element = this[key]
        if (element == null || element is JsonNull) return null
        return element.jsonPrimitive.content
    }

    fun mapJsonToEntity(jsonObj: JsonObject, fallbackChatId: String): ChatMessageEntity? {
        return try {
            val createdAtStr = jsonObj.getStringOrNull("created_at") ?: return null
            val createdAtEpoch = Instant.parse(createdAtStr).toEpochMilli()

            val updatedAtStr = jsonObj.getStringOrNull("updated_at")
            val updatedAtEpoch = updatedAtStr?.let { Instant.parse(it).toEpochMilli() }

            val editedAtStr = jsonObj.getStringOrNull("edited_at")
            val editedAtEpoch = editedAtStr?.let { Instant.parse(it).toEpochMilli() }

            val deletedAtStr = jsonObj.getStringOrNull("deleted_at")
            val deletedAtEpoch = deletedAtStr?.let { Instant.parse(it).toEpochMilli() }

            // 🚀 FIX: Map actual server status instead of hardcoding DELIVERED
            val statusStr = jsonObj.getStringOrNull("status")?.uppercase()
            val mappedStatus = when (statusStr) {
                "READ" -> MessageStatus.READ
                "DELIVERED" -> MessageStatus.DELIVERED
                "SENT" -> MessageStatus.SENT
                else -> MessageStatus.DELIVERED
            }

            ChatMessageEntity(
                id = jsonObj.getStringOrNull("id") ?: return null,
                chatId = jsonObj.getStringOrNull("chat_id") ?: fallbackChatId,
                senderId = jsonObj.getStringOrNull("sender_id") ?: return null,
                content = jsonObj.getStringOrNull("content"),
                type = MessageType.valueOf(jsonObj.getStringOrNull("type") ?: "text"),
                status = mappedStatus,
                replyToId = jsonObj.getStringOrNull("reply_to_id"),
                createdAt = createdAtEpoch,
                updatedAt = updatedAtEpoch,
                editedAt = editedAtEpoch,
                deletedAt = deletedAtEpoch,
                isForwarded = jsonObj.getStringOrNull("is_forwarded")?.toBoolean() ?: false,
                metadataJson = jsonObj.getStringOrNull("metadata"),
                isDeletedForMe = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message JSON: ${e.message}")
            null
        }
    }
}
