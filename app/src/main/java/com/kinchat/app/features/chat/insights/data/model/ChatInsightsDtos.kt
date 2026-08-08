package com.kinchat.app.features.chat.insights.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ChatPreviewDto(
    @SerialName("chat_id") val chatId: String,
    @SerialName("other_user_id") val otherUserId: String,
    @SerialName("other_user_name") val otherUserName: String? = null
)

@Serializable
internal data class MessageDto(
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("sender_id") val senderId: String? = null
)

@Serializable
internal data class ChatUserStatisticsDto(
    @SerialName("chat_id") val chatId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("total_messages") val totalMessages: Int = 0,
    @SerialName("total_words") val totalWords: Int = 0,
    @SerialName("total_chars") val totalChars: Int = 0,
    @SerialName("longest_message_length") val longestMessageLength: Int = 0
)

@Serializable
internal data class ExtendedStatsDto(
    @SerialName("my_audio_calls") val myAudioCalls: Int = 0,
    @SerialName("friend_audio_calls") val friendAudioCalls: Int = 0,
    @SerialName("my_video_calls") val myVideoCalls: Int = 0,
    @SerialName("friend_video_calls") val friendVideoCalls: Int = 0,
    @SerialName("my_duration") val myDuration: Long = 0,
    @SerialName("friend_duration") val friendDuration: Long = 0,
    @SerialName("my_reactions") val myReactions: Int = 0,
    @SerialName("friend_reactions") val friendReactions: Int = 0,
    @SerialName("my_links") val myLinks: Int = 0,
    @SerialName("friend_links") val friendLinks: Int = 0,
    @SerialName("my_bytes") val myBytes: Long = 0,
    @SerialName("friend_bytes") val friendBytes: Long = 0,
    @SerialName("my_images") val myImages: Int = 0,
    @SerialName("friend_images") val friendImages: Int = 0,
    @SerialName("my_videos") val myVideos: Int = 0,
    @SerialName("friend_videos") val friendVideos: Int = 0,
    @SerialName("my_audio") val myAudio: Int = 0,
    @SerialName("friend_audio") val friendAudio: Int = 0,
    @SerialName("my_docs") val myDocs: Int = 0,
    @SerialName("friend_docs") val friendDocs: Int = 0,
    @SerialName("top_reaction") val topReaction: String? = null
)
