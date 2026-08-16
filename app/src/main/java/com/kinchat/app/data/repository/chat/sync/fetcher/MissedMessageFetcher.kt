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
                    // Fetch based on the last updated timestamp to catch edits/deletes as well
                    val lastSyncTimeEpoch = chatMessageDao.getLastUpdatedTimestamp(chatId) 
                        ?: chatMessageDao.getLastMessageTimestamp(chatId)
                    val isInitialSync = lastSyncTimeEpoch == null

                    var offset = 0L
                    val limit = 1000L
                    var hasMore = true

                    while (hasMore) {
                        val rawJsonArray = supabaseClient.postgrest[TABLE_MESSAGES]
                            .select {
                                filter {
                                    eq(COLUMN_CHAT_ID, chatId)
                                    if (!isInitialSync) {
                                        // 🚀 FIX: Tracking edits and tombstones by updated_at (or created_at fallback)
                                        val lastSyncIso = Instant.ofEpochMilli(lastSyncTimeEpoch!!).toString()
                                        gt(COLUMN_UPDATED_AT, lastSyncIso)
                                    }
                                }
                                range(offset, offset + limit - 1)
                            }
                            .decodeList<JsonObject>()

                        if (rawJsonArray.isNotEmpty()) {
                            val entities = rawJsonArray.mapNotNull {
                                ChatSyncMapper.mapJsonToEntity(it, chatId)
                            }

                            if (entities.isNotEmpty()) {
                                // 🚀 FIX: Delegate to Dao to merge, preventing REPLACE clobbering
                                entities.forEach { entity ->
                                    chatMessageDao.upsertMessageMerged(entity)
                                }
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
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Delta Sync Error for chat $chatId", e)
                throw e // 🚀 FIX: Propagate failure to the caller
            }
        }
    }

    companion object {
        private const val TAG = "MissedMessageFetcher"
        private const val TABLE_MESSAGES = "messages"
        private const val COLUMN_CHAT_ID = "chat_id"
        private const val COLUMN_UPDATED_AT = "updated_at"
    }
}
