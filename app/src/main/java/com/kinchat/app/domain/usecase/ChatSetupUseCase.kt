package com.kinchat.app.domain.usecase

import com.kinchat.app.data.local.db.ChatDao
import com.kinchat.app.domain.repository.AuthRepository
import com.kinchat.app.domain.repository.ChatRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

data class ChatSetupResult(
    val actualChatId: String,
    val partnerId: String,
    val partnerName: String?,
    val currentUserId: String
)

class ChatSetupUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val chatDao: ChatDao
) {
    private val AI_BOT_ID = "de438bb4-d954-4c31-9ad1-9dd34b85d981"

    suspend fun getInstantPartnerInfo(passedId: String): Pair<String, String?> {
        if (passedId == AI_BOT_ID) return Pair(AI_BOT_ID, "TukTak AI")

        val currentUserId = authRepository.getCurrentUserId() ?: ""
        val localPreview = chatDao.observeChatPreview(passedId).firstOrNull()

        if (localPreview != null) {
            val partnerId = localPreview.participants.firstOrNull { it.userId != currentUserId }?.userId
            val partnerName = chatDao.getChatTitle(passedId)
            if (partnerId != null) return Pair(partnerId, partnerName)
        }
        return Pair(passedId, chatDao.getChatTitle(passedId))
    }

    suspend fun execute(passedId: String, quickName: String?): ChatSetupResult? {
        val currentUserId = authRepository.getCurrentUserId() ?: return null
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
                }

                if (partnerName.isNullOrEmpty()) {
                    partnerName = localTitle ?: chatRepository.getPartnerName(actualChatId, currentUserId)
                }
            } else {
                partnerId = passedId

                val result = chatRepository.createChatIfNotExists(partnerId)
                if (result.isSuccess) {
                    actualChatId = result.getOrNull() ?: passedId
                    if (partnerName.isNullOrEmpty()) {
                        partnerName = chatRepository.getPartnerName(actualChatId, currentUserId)
                    }
                } else {
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
