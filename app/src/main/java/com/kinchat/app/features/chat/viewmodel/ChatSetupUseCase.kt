package com.kinchat.app.features.chat.viewmodel

import com.kinchat.app.data.local.db.ChatDao
import com.kinchat.app.domain.repository.ChatRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class ParticipantDto(val chat_id: String? = null, val user_id: String)

data class ChatSetupResult(
    val actualChatId: String,
    val partnerId: String,
    val partnerName: String?,
    val currentUserId: String
)

class ChatSetupUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val supabaseClient: SupabaseClient,
    private val chatDao: ChatDao
) {
    private val AI_BOT_ID = "de438bb4-d954-4c31-9ad1-9dd34b85d981"

    // 🚀 NEW FIX: UI-কে সাথে সাথে নাম দেওয়ার জন্য ইনস্ট্যান্ট ডাটাবেজ লুকআপ (No Loading Delay)
    suspend fun getInstantPartnerInfo(passedId: String): Pair<String, String?> {
        if (passedId == AI_BOT_ID) return Pair(AI_BOT_ID, "TukTak AI")
        
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: ""
        val localPreview = chatDao.observeChatPreview(passedId).firstOrNull()
        
        if (localPreview != null) {
            // চ্যাট আইডি দিয়ে লোকাল ডাটাবেজ থেকে সাথে সাথে আসল ইউজার আইডি বের করে আনা হচ্ছে
            val partnerId = localPreview.participants.firstOrNull { it.userId != currentUserId }?.userId
            val partnerName = chatDao.getChatTitle(passedId)
            if (partnerId != null) return Pair(partnerId, partnerName)
        }
        return Pair(passedId, chatDao.getChatTitle(passedId))
    }

    suspend fun execute(passedId: String, quickName: String?): ChatSetupResult? {
        var user = supabaseClient.auth.currentUserOrNull()
        var retryCount = 0

        while (user == null && retryCount < 5) {
            delay(300)
            user = supabaseClient.auth.currentUserOrNull()
            retryCount++
        }

        val currentUserId = user?.id ?: return null
        if (passedId.isEmpty()) return null

        if (passedId == AI_BOT_ID) {
            return ChatSetupResult(AI_BOT_ID, AI_BOT_ID, "TukTak AI", currentUserId)
        }

        var actualChatId = passedId
        var partnerId = passedId
        var partnerName = quickName

        try {
            val localTitle = chatDao.getChatTitle(passedId)
            val localPreview = chatDao.observeChatPreview(passedId).firstOrNull()
            val isLocalChat = localTitle != null || localPreview != null

            if (isLocalChat) {
                actualChatId = passedId
                val localPartnerId = localPreview?.participants?.firstOrNull { it.userId != currentUserId }?.userId
                
                if (localPartnerId != null) {
                    partnerId = localPartnerId
                } else {
                    try {
                        val participants = supabaseClient.postgrest["chat_participants"]
                            .select { filter { eq("chat_id", passedId) } }
                            .decodeList<ParticipantDto>()
                        val pId = participants.firstOrNull { it.user_id != currentUserId }?.user_id
                        if (pId != null) partnerId = pId
                    } catch (e: Exception) {
                        // Offline fallback
                    }
                }

                if (partnerName.isNullOrEmpty()) {
                    partnerName = localTitle ?: chatRepository.getPartnerName(actualChatId, currentUserId)
                }
            } else {
                partnerId = passedId

                try {
                    val response = supabaseClient.postgrest.rpc(
                        "create_chat_if_not_exists",
                        mapOf("user1_id" to currentUserId, "user2_id" to partnerId)
                    ).decodeAs<String>()

                    actualChatId = response.replace("\"", "")
                    if (partnerName.isNullOrEmpty()) {
                        partnerName = chatRepository.getPartnerName(actualChatId, currentUserId)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    actualChatId = passedId
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ChatSetupResult(
            actualChatId = actualChatId,
            partnerId = partnerId,
            partnerName = partnerName ?: "Unknown",
            currentUserId = currentUserId
        )
    }
}
