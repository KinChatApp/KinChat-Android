package com.kinchat.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_insights")
data class ChatInsightsEntity(
    @PrimaryKey
    val friendId: String,
    val chatId: String,
    val friendName: String,
    
    val totalMessages: Int,
    val daysConnected: Int,
    val firstMessageAt: String?,
    val lastMessageAt: String?,
    val firstMessageSender: String,
    
    val mostActiveDay: String,
    val mostActiveHour: String,
    
    val myMessages: Int, val myWords: Int, val myChars: Int, val myLongest: Int,
    val friendMessages: Int, val friendWords: Int, val friendChars: Int, val friendLongest: Int,
    
    val myImages: Int, val friendImages: Int,
    val myVideos: Int, val friendVideos: Int,
    val myAudioMedia: Int, val friendAudioMedia: Int,
    val myDocuments: Int, val friendDocuments: Int,

    val myAudioCalls: Int, val friendAudioCalls: Int,
    val myVideoCalls: Int, val friendVideoCalls: Int,
    val myCallDuration: Long, val friendCallDuration: Long,
    val myReactions: Int, val friendReactions: Int,
    val myLinks: Int, val friendLinks: Int,
    val myDataShared: Long, val friendDataShared: Long,
    val topReaction: String?,
    
    val lastCalculatedAt: Long,
    val isSynced: Boolean
)
