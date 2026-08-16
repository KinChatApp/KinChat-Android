package com.kinchat.app.data.repository.chat.sync.mapper

import android.util.Log
import com.kinchat.app.data.local.db.ChatMessageEntity
import com.kinchat.app.data.local.db.MessageStatus
import com.kinchat.app.data.local.db.MessageType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

object ChatSyncMapper {
    private const val TAG = "ChatSyncMapper"

    fun mapJsonToEntity(jsonObj: JsonObject, fallbackChatId: String): ChatMessageEntity? {
        return try {
            val createdAtStr = jsonObj["created_at"]?.jsonPrimitive?.content ?: return null
            val createdAtEpoch = Instant.parse(createdAtStr).toEpochMilli()
            
            // 🚀 FIX: Parse updated_at and deleted_at for proper sync
            val updatedAtStr = jsonObj["updated_at"]?.jsonPrimitive?.content
            val updatedAtEpoch = updatedAtStr?.let { Instant.parse(it).toEpochMilli() }
            
            val deletedAtStr = jsonObj["deleted_at"]?.jsonPrimitive?.content
            val deletedAtEpoch = deletedAtStr?.let { Instant.parse(it).toEpochMilli() }

            ChatMessageEntity(
                id = jsonObj["id"]?.jsonPrimitive?.content ?: return null,
                chatId = jsonObj["chat_id"]?.jsonPrimitive?.content ?: fallbackChatId,
                senderId = jsonObj["sender_id"]?.jsonPrimitive?.content ?: return null,
                content = jsonObj["content"]?.jsonPrimitive?.content,
                type = MessageType.valueOf(jsonObj["type"]?.jsonPrimitive?.content ?: "text"),
                status = MessageStatus.DELIVERED, // Will be handled by upsertMessageMerged logic if local is PENDING
                replyToId = jsonObj["reply_to_id"]?.jsonPrimitive?.content,
                createdAt = createdAtEpoch,
                editedAt = updatedAtEpoch,
                deletedAt = deletedAtEpoch,
                isForwarded = jsonObj["is_forwarded"]?.jsonPrimitive?.content?.toBoolean() ?: false,
                metadataJson = jsonObj["metadata"]?.toString(),
                isDeletedForMe = deletedAtEpoch != null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message JSON: ${e.message}")
            null
        }
    }
}
