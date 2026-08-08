package com.kinchat.app.data.repository.settings

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class ChatRemoteSettingsHandler(
    private val supabaseClient: SupabaseClient
) {
    suspend fun updateFavoriteStatus(chatId: String, isFavorite: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = requireCurrentUserId()
            
            supabaseClient.postgrest[ChatSettingsConstants.TABLE_CHAT_PARTICIPANTS]
                .update(mapOf(ChatSettingsConstants.COLUMN_IS_FAVORITE to isFavorite)) { 
                    filter { 
                        eq(ChatSettingsConstants.COLUMN_CHAT_ID, chatId)
                        eq(ChatSettingsConstants.COLUMN_USER_ID, userId) 
                    } 
                }
            Unit
        }
    }

    suspend fun updateBlockStatus(chatId: String, isBlocked: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = requireCurrentUserId()
            
            val participants = supabaseClient.postgrest[ChatSettingsConstants.TABLE_CHAT_PARTICIPANTS]
                .select { 
                    filter { 
                        eq(ChatSettingsConstants.COLUMN_CHAT_ID, chatId)
                        neq(ChatSettingsConstants.COLUMN_USER_ID, userId) 
                    } 
                }
                .decodeList<JsonObject>()

            val partnerId = participants.firstOrNull()?.get(ChatSettingsConstants.COLUMN_USER_ID)?.jsonPrimitive?.content
                ?: throw IllegalStateException("Partner not found for chat: $chatId")

            if (isBlocked) {
                supabaseClient.postgrest[ChatSettingsConstants.TABLE_USER_BLOCKS].insert(
                    mapOf(
                        ChatSettingsConstants.COLUMN_BLOCKER_ID to userId, 
                        ChatSettingsConstants.COLUMN_BLOCKED_ID to partnerId
                    )
                )
            } else {
                supabaseClient.postgrest[ChatSettingsConstants.TABLE_USER_BLOCKS].delete { 
                    filter { 
                        eq(ChatSettingsConstants.COLUMN_BLOCKER_ID, userId)
                        eq(ChatSettingsConstants.COLUMN_BLOCKED_ID, partnerId) 
                    } 
                }
            }
            Unit
        }
    }

    private fun requireCurrentUserId(): String {
        return supabaseClient.auth.currentUserOrNull()?.id 
            ?: throw IllegalStateException("User is not authenticated")
    }
}
