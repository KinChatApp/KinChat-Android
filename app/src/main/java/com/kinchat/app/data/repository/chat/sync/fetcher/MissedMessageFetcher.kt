package com.kinchat.app.data.repository.chat.sync.fetcher

import android.util.Log
import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.repository.chat.sync.mapper.ChatSyncMapper
import com.kinchat.app.data.repository.chat.sync.utils.SyncRetryHelper.retryWithBackoff
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
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
                retryWithBackoff {
                    val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                    val lastSyncTimeEpoch = chatMessageDao.getLastMessageTimestamp(chatId)
                    val isInitialSync = lastSyncTimeEpoch == null
                    
                    var offset = 0L
                    val limit = 1000L
                    var hasMore = true

                    // 🚀 SENIOR FIX: Pagination added for handling large history on cold install
                    while (hasMore) {
                        val rawJsonArray = supabaseClient.postgrest[TABLE_MESSAGES]
                            .select {
                                filter {
                                    eq(COLUMN_CHAT_ID, chatId)
                                    if (!isInitialSync) {
                                        val lastSyncIso = Instant.ofEpochMilli(lastSyncTimeEpoch!!).toString()
                                        gt(COLUMN_CREATED_AT, lastSyncIso)
                                    }
                                }
                                range(offset, offset + limit - 1)
                            }
                            .decodeList<JsonObject>()

                        if (rawJsonArray.isNotEmpty()) {
                            val entities = rawJsonArray
                                .filterNot {
                                    // 🚀 SENIOR FIX: Only filter out own messages during delta sync, NOT on initial sync
                                    !isInitialSync &&
                                        currentUserId != null &&
                                        it[COLUMN_SENDER_ID]?.jsonPrimitive?.content == currentUserId
                                }
                                .mapNotNull {
                                    ChatSyncMapper.mapJsonToEntity(it, chatId)
                                }

                            if (entities.isNotEmpty()) {
                                chatMessageDao.insertMessages(entities)
                            }

                            if (rawJsonArray.size < limit.toInt()) {
                                hasMore = false // Fetched the last chunk
                            } else {
                                offset += limit // Prepare for the next chunk
                            }
                        } else {
                            hasMore = false // No more messages
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Delta Sync Error for chat $chatId after retries", e)
            }
        }
    }

    companion object {
        private const val TAG = "MissedMessageFetcher"
        private const val TABLE_MESSAGES = "messages"
        private const val COLUMN_CHAT_ID = "chat_id"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_SENDER_ID = "sender_id"
    }
}
