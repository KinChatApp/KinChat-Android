package com.kinchat.app.features.chat.insights.data.model

data class ChatInsightsRawData(
    val chatId: String,
    val friendName: String,
    val firstMessage: MessageDto?,
    val lastMessage: MessageDto?,
    val stats: List<ChatUserStatisticsDto>,
    val recentMessages: List<MessageDto>,
    val extendedStats: ExtendedStatsDto
)
