package com.kinchat.app.features.chat.viewmodel

import com.kinchat.app.domain.repository.ChatRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject

@Serializable
private data class ParticipantDto(val chat_id: String? = null, val user_id: String)

@Serializable
private data class ChatDto(val id: String)

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
        var partnerName = quickName ?: chatRepository.getPartnerName(actualChatId, currentUserId)
        var partnerId = ""

        if (passedId == AI_BOT_ID || partnerName == null) {
            try {
                if (passedId == AI_BOT_ID) {
                    partnerId = AI_BOT_ID
                    partnerName = "TukTak AI"
                }
                
                val myChats = supabaseClient.postgrest["chat_participants"]
                    .select { filter { eq("user_id", currentUserId) } }
                    .decodeList<ParticipantDto>().mapNotNull { it.chat_id }

                val partnerChats = supabaseClient.postgrest["chat_participants"]
                    .select { filter { eq("user_id", partnerId) } }
                    .decodeList<ParticipantDto>().mapNotNull { it.chat_id }

                val sharedChatId = myChats.intersect(partnerChats.toSet()).firstOrNull()

                if (sharedChatId != null) {
                    actualChatId = sharedChatId
                    partnerName = partnerName ?: chatRepository.getPartnerName(actualChatId, currentUserId)
                } else {
                    val newChatId = UUID.randomUUID().toString()
                    supabaseClient.postgrest["chats"].insert(ChatDto(id = newChatId))
                    supabaseClient.postgrest["chat_participants"].insert(listOf(
                        ParticipantDto(chat_id = newChatId, user_id = currentUserId),
                        ParticipantDto(chat_id = newChatId, user_id = partnerId)
                    ))
                    actualChatId = newChatId
                }
            } catch (e: Exception) {
                if (passedId == AI_BOT_ID) partnerName = "TukTak AI"
            }
        }

        return ChatSetupResult(actualChatId, partnerId, partnerName, currentUserId)
    }
}
