package com.kinchat.app.features.chat.insights.data.factory

import com.kinchat.app.data.local.db.ChatInsightsEntity
import com.kinchat.app.features.chat.insights.data.model.ChatInsightsRawData
import com.kinchat.app.features.chat.insights.data.utils.ChatInsightsAnalyticsHelper
import javax.inject.Inject

class ChatInsightsEntityFactory @Inject constructor() {

    internal fun createEntity(
        meId: String,
        friendId: String,
        rawData: ChatInsightsRawData
    ): ChatInsightsEntity {
        val myStatsRaw = rawData.stats.find { it.userId == meId }
        val friendStatsRaw = rawData.stats.find { it.userId == friendId }
        val extStats = rawData.extendedStats

        val daysConnected = ChatInsightsAnalyticsHelper.calculateDaysConnected(rawData.firstMessage?.createdAt)
        val mostActiveDay = ChatInsightsAnalyticsHelper.calculateMostActiveDay(rawData.recentMessages)
        val mostActiveHour = ChatInsightsAnalyticsHelper.calculateMostActiveHour(rawData.recentMessages)

        val firstMessageSender = if (rawData.firstMessage?.senderId == meId) {
            "You"
        } else {
            rawData.friendName
        }

        val totalMsgs = (myStatsRaw?.totalMessages ?: 0) + (friendStatsRaw?.totalMessages ?: 0)

        return ChatInsightsEntity(
            friendId = friendId,
            chatId = rawData.chatId,
            friendName = rawData.friendName,
            totalMessages = totalMsgs,
            daysConnected = daysConnected,
            firstMessageAt = rawData.firstMessage?.createdAt,
            lastMessageAt = rawData.lastMessage?.createdAt,
            firstMessageSender = firstMessageSender,
            mostActiveDay = mostActiveDay,
            mostActiveHour = mostActiveHour,
            myMessages = myStatsRaw?.totalMessages ?: 0,
            myWords = myStatsRaw?.totalWords ?: 0,
            myChars = myStatsRaw?.totalChars ?: 0,
            myLongest = myStatsRaw?.longestMessageLength ?: 0,
            friendMessages = friendStatsRaw?.totalMessages ?: 0,
            friendWords = friendStatsRaw?.totalWords ?: 0,
            friendChars = friendStatsRaw?.totalChars ?: 0,
            friendLongest = friendStatsRaw?.longestMessageLength ?: 0,
            myImages = extStats.myImages,
            friendImages = extStats.friendImages,
            myVideos = extStats.myVideos,
            friendVideos = extStats.friendVideos,
            myAudioMedia = extStats.myAudio,
            friendAudioMedia = extStats.friendAudio,
            myDocuments = extStats.myDocs,
            friendDocuments = extStats.friendDocs,
            myAudioCalls = extStats.myAudioCalls,
            friendAudioCalls = extStats.friendAudioCalls,
            myVideoCalls = extStats.myVideoCalls,
            friendVideoCalls = extStats.friendVideoCalls,
            myCallDuration = extStats.myDuration,
            friendCallDuration = extStats.friendDuration,
            myReactions = extStats.myReactions,
            friendReactions = extStats.friendReactions,
            myLinks = extStats.myLinks,
            friendLinks = extStats.friendLinks,
            myDataShared = extStats.myBytes,
            friendDataShared = extStats.friendBytes,
            topReaction = extStats.topReaction,
            lastCalculatedAt = System.currentTimeMillis(),
            isSynced = true
        )
    }
}
