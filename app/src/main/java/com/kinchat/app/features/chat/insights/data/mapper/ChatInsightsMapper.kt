package com.kinchat.app.features.chat.insights.data.mapper

import com.kinchat.app.data.local.db.ChatInsightsEntity
import com.kinchat.app.features.chat.insights.domain.model.ChatInsights
import com.kinchat.app.features.chat.insights.domain.model.ChatStats
import com.kinchat.app.features.chat.insights.domain.model.ExtendedStats
import com.kinchat.app.features.chat.insights.domain.model.MediaStats

fun ChatInsightsEntity.toDomain(): ChatInsights {
    return ChatInsights(
        totalMessages = this.totalMessages,
        daysConnected = this.daysConnected,
        firstMessageAt = this.firstMessageAt,
        lastMessageAt = this.lastMessageAt,
        firstMessageSender = this.firstMessageSender,
        longestMessage = maxOf(this.myLongest, this.friendLongest),
        friendName = this.friendName,
        mostActiveSender = when {
            this.myMessages > this.friendMessages -> "You"
            this.myMessages < this.friendMessages -> this.friendName
            else -> "Tie"
        },
        mostActiveDay = this.mostActiveDay,
        mostActiveHour = this.mostActiveHour,
        currentUserStats = ChatStats(
            totalMessages = this.myMessages,
            totalWords = this.myWords,
            totalChars = this.myChars,
            longestMessageLength = this.myLongest
        ),
        friendStats = ChatStats(
            totalMessages = this.friendMessages,
            totalWords = this.friendWords,
            totalChars = this.friendChars,
            longestMessageLength = this.friendLongest
        ),
        mediaStats = MediaStats(
            myImages = this.myImages,
            friendImages = this.friendImages,
            myVideos = this.myVideos,
            friendVideos = this.friendVideos,
            myAudio = this.myAudioMedia,
            friendAudio = this.friendAudioMedia,
            myDocuments = this.myDocuments,
            friendDocuments = this.friendDocuments
        ),
        extendedStats = ExtendedStats(
            myAudioCalls = this.myAudioCalls,
            friendAudioCalls = this.friendAudioCalls,
            myVideoCalls = this.myVideoCalls,
            friendVideoCalls = this.friendVideoCalls,
            myDuration = this.myCallDuration,
            friendDuration = this.friendCallDuration,
            myReactions = this.myReactions,
            friendReactions = this.friendReactions,
            myLinks = this.myLinks,
            friendLinks = this.friendLinks,
            myBytes = this.myDataShared,
            friendBytes = this.friendDataShared,
            topReaction = this.topReaction
        )
    )
}
