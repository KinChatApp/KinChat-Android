package com.kinchat.app.data.remote.api

import com.kinchat.app.core.logging.AppLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import javax.inject.Inject
import javax.inject.Singleton

interface ChatRpcService {
    suspend fun getPartnerName(chatId: String, currentUserId: String): String?
}

@Singleton
class ChatRpcServiceImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ChatRpcService {

    companion object {
        private const val RPC_GET_PARTNER_NAME = "get_chat_partner_name"
    }

    override suspend fun getPartnerName(chatId: String, currentUserId: String): String? {
        return try {
            val name = supabaseClient.postgrest.rpc(
                function = RPC_GET_PARTNER_NAME,
                parameters = mapOf(
                    "p_chat_id" to chatId, 
                    "p_current_user_id" to currentUserId
                )
            ).decodeAs<String>()
            
            AppLogger.d("ChatRpcService", "Fetched partner name from RPC successfully")
            name
        } catch (e: Exception) {
            AppLogger.e("ChatRpcService", "RPC Error getting partner name", e)
            null
        }
    }
}
