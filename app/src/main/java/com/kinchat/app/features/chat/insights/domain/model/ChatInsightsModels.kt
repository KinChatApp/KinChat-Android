package com.kinchat.app.features.chat.insights.domain.model

data class ChatStats(
    val messages: Int,
    val words: Int,
    val chars: Int,
    val longest: Int
)

data class MediaStats(
    val myImages: Int, val friendImages: Int,
    val myVideos: Int, val friendVideos: Int,
    val myAudio: Int, val friendAudio: Int,
    val myDocuments: Int, val friendDocuments: Int
)

data class ExtendedStats(
    val myAudioCalls: Int, val friendAudioCalls: Int,
    val myVideoCalls: Int, val friendVideoCalls: Int,
    val myCallDuration: Long, val friendCallDuration: Long,
    val myReactions: Int, val friendReactions: Int,
    val myLinks: Int, val friendLinks: Int,
    val myDataShared: Long, val friendDataShared: Long,
    val topReaction: String?
)

data class ChatInsights(
    val totalMessages: Int,
    val daysConnected: Int,
    val firstMessageAt: String?,
    val lastMessageAt: String?,
    val firstMessageSender: String,
    val longestMessage: Int,
    val friendName: String,
    val mostActiveSender: String,
    val mostActiveDay: String,
    val mostActiveHour: String,
    val currentUserStats: ChatStats,
    val friendStats: ChatStats,
    val mediaStats: MediaStats,
    val extendedStats: ExtendedStats
)
