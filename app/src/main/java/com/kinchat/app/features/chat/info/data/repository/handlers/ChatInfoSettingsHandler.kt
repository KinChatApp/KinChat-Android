package com.kinchat.app.features.chat.info.data.repository.handlers

import com.kinchat.app.data.local.db.UserBlockDao
import com.kinchat.app.data.local.db.ChatParticipantDao
import com.kinchat.app.features.chat.info.domain.model.ChatInfoSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatInfoSettingsHandler @Inject constructor(
    private val userBlockDao: UserBlockDao,
    private val participantDao: ChatParticipantDao
) {
    suspend fun getChatSettings(partnerId: String): Result<ChatInfoSettings> {
        // TODO: Room থেকে ডাটা ফেচ করার লজিক (Offline-first)
        // আপতত বিল্ড ঠিক করার জন্য লোকাল ডিফল্ট ভ্যালু রিটার্ন করা হচ্ছে
        return Result.success(
            ChatInfoSettings(
                chatId = null,
                isMuted = false,
                isBlocked = false,
                mediaCount = 0
            )
        )
    }

    suspend fun toggleMute(chatId: String, isMuted: Boolean): Result<Unit> {
        // TODO: Route through ChatRepository.updateChatMuteStatus (Outbox)
        return Result.success(Unit)
    }

    suspend fun clearChat(chatId: String): Result<Unit> {
        // TODO: Offline pending operation for clearing chat
        return Result.success(Unit)
    }
}
