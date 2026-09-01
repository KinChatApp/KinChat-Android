package com.kinchat.app.data.repository.chat.sync.fetcher

import android.util.Log
import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.local.db.MessageStatus
import com.kinchat.app.data.repository.chat.sync.mapper.ChatSyncMapper
import com.kinchat.app.data.repository.chat.sync.utils.SyncRetryHelper.retryWithBackoff
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import javax.inject.Inject

class MissedMessageFetcher @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val chatMessageDao: ChatMessageDao
) {
    suspend fun fetchMissedMessages(chatId: String) {
        withContext(Dispatchers.IO) {
            try {
                supabaseClient.auth.awaitInitialization()
                val currentUser = supabaseClient.auth.currentSessionOrNull()?.user

                if (currentUser == null) {
                    AppLogger.d(TAG, "⏭️ Skipping missed-message fetch: no authenticated session")
                    return@withContext
                }
                
                retryWithBackoff {
                    val lastSyncEpoch = chatMessageDao.getLastMessageTimestamp(chatId) ?: 0L
                    val lastEditEpoch = chatMessageDao.getLastUpdatedTimestamp(chatId) ?: 0L
                    val targetEpoch = maxOf(lastSyncEpoch, lastEditEpoch)
                    val isInitialSync = targetEpoch == 0L

                    var offset = 0L
                    val limit = 1000L
                    var hasMore = true

                    while (hasMore) {
                        val rawJsonArray = supabaseClient.postgrest[TABLE_MESSAGES]
                            .select {
                                filter {
                                    eq(COLUMN_CHAT_ID, chatId)
                                    if (!isInitialSync) {
                                        val lastSyncIso = Instant.ofEpochMilli(targetEpoch).toString()
                                        gt("created_at", lastSyncIso)
                                    }
                                }
                                order("created_at", Order.DESCENDING)
                                range(offset, offset + limit - 1)
                            }
                            .decodeList<JsonObject>()

                        if (rawJsonArray.isNotEmpty()) {
                            val entities = rawJsonArray.mapNotNull {
                                ChatSyncMapper.mapJsonToEntity(it, chatId)
                            }

                            if (entities.isNotEmpty()) {
                                chatMessageDao.upsertMessagesMerged(entities)
                            }

                            if (rawJsonArray.size < limit.toInt()) {
                                hasMore = false
                            } else {
                                offset += limit
                            }
                        } else {
                            hasMore = false
                        }
                    }

                    // 🚀 FIX: Targeted Receipt Recovery (No cursor guesswork)
                    val pendingIds = chatMessageDao.getPendingMessageIds(chatId, currentUser.id)
                    if (pendingIds.isNotEmpty()) {
                        pendingIds.chunked(100).forEach { chunk ->
                            val receiptsArray = supabaseClient.postgrest[TABLE_RECEIPTS]
                                .select {
                                    filter { isIn("message_id", chunk) }
                                }.decodeList<JsonObject>()

                            receiptsArray.forEach { receiptObj ->
                                val msgId = receiptObj["message_id"]?.jsonPrimitive?.content ?: return@forEach
                                val statusStr = receiptObj["status"]?.jsonPrimitive?.content?.uppercase()

                                val mappedStatus = when (statusStr) {
                                    "READ" -> MessageStatus.READ
                                    "DELIVERED" -> MessageStatus.DELIVERED
                                    else -> null
                                }

                                if (mappedStatus != null) {
                                    chatMessageDao.updateMessageStatus(msgId, mappedStatus)
                                }
                            }
                        }
                        AppLogger.d(TAG, "✅ Recovered receipts for ${pendingIds.size} pending messages in chat $chatId")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e(TAG, "Delta Sync Error for chat $chatId", e)
                throw e
            }
        }
    }

    companion object {
        private const val TAG = "MissedMessageFetcher"
        private const val TABLE_MESSAGES = "messages"
        private const val TABLE_RECEIPTS = "message_receipts"
        private const val COLUMN_CHAT_ID = "chat_id"
    }
}
