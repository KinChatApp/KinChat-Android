package com.kinchat.app.features.chat.insights.domain.repository

import com.kinchat.app.features.chat.insights.domain.model.ChatInsights
import kotlinx.coroutines.flow.Flow

interface ChatInsightsRepository {
    fun getChatInsightsFlow(friendId: String): Flow<ChatInsights?>
    suspend fun refreshChatInsights(friendId: String): Result<Unit>
}
