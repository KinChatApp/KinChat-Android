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

                    val rawJsonArray = supabaseClient.postgrest[TABLE_MESSAGES]
                        .select {
                            filter {
                                eq(COLUMN_CHAT_ID, chatId)
                                lastSyncTimeEpoch?.let {
                                    val lastSyncIso = Instant.ofEpochMilli(it).toString()
                                    gt(COLUMN_CREATED_AT, lastSyncIso)
                                }
                            }
                        }
                        .decodeList<JsonObject>()

                    if (rawJsonArray.isNotEmpty()) {
                        val entities = rawJsonArray
                            .filterNot {
                                currentUserId != null &&
                                    it[COLUMN_SENDER_ID]?.jsonPrimitive?.content == currentUserId
                            }
                            .mapNotNull {
                                ChatSyncMapper.mapJsonToEntity(it, chatId)
                            }

                        if (entities.isNotEmpty()) {
                            chatMessageDao.insertMessages(entities)
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
