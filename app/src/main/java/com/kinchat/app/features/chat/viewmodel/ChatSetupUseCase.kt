package com.kinchat.app.features.chat.viewmodel

import com.kinchat.app.domain.repository.ChatRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.delay
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
    private val supabaseClient: SupabaseClient
) {
    private val AI_BOT_ID = "de438bb4-d954-4c31-9ad1-9dd34b85d981"

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

        var actualChatId = passedId
        var partnerId = ""
        var partnerName = quickName

        if (passedId == AI_BOT_ID) {
            partnerId = AI_BOT_ID
            partnerName = "TukTak AI"
        } else {
            try {
                // Check if passedId is a Chat ID
                val participants = supabaseClient.postgrest["chat_participants"]
                    .select { filter { eq("chat_id", passedId) } }
                    .decodeList<ParticipantDto>()

                if (participants.isNotEmpty()) {
                    actualChatId = passedId
                    partnerId = participants.firstOrNull { it.user_id != currentUserId }?.user_id ?: ""
                } else {
                    // Treat passedId as User ID
                    partnerId = passedId

                    val response = supabaseClient.postgrest.rpc(
                        "create_chat_if_not_exists",
                        mapOf("user1_id" to currentUserId, "user2_id" to partnerId)
                    ).decodeAs<String>()

                    actualChatId = response.replace("\"", "")
                }

                if (partnerName.isNullOrEmpty()) {
                    partnerName = chatRepository.getPartnerName(actualChatId, currentUserId)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                // 🚀 FIXED (Phase 3): Return offline fallback data instead of null.
                // This ensures offline chat reads are not blocked by a failed network request.
                return ChatSetupResult(
                    actualChatId = passedId,
                    partnerId = passedId,
                    partnerName = partnerName ?: "Unknown",
                    currentUserId = currentUserId
                )
            }
        }

        return ChatSetupResult(actualChatId, partnerId, partnerName, currentUserId)
    }
}
