package com.kinchat.app.data.repository

import android.util.Log
import com.kinchat.app.data.local.db.*
import com.kinchat.app.data.remote.model.ChatPreviewDto
import com.kinchat.app.data.remote.model.UserProfileDto
import com.kinchat.app.domain.model.Chat
import com.kinchat.app.domain.model.UserProfile
import com.kinchat.app.domain.repository.DashboardRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val chatDao: ChatDao,
    private val chatParticipantDao: ChatParticipantDao,
    private val chatMessageDao: ChatMessageDao
) : DashboardRepository {

    private val safeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override suspend fun getCurrentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    override suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            val dto = supabase.postgrest["users"].select { filter { eq("id", userId) } }.decodeSingleOrNull<UserProfileDto>()
            dto?.let { UserProfile(id = it.id, avatarUrl = it.avatarUrl) }
        } catch (e: Exception) { null }
    }

    // 🚀 Offline-First Dashboard Flow - Fixed Race Condition
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getRecentChats(): Flow<List<Chat>> {
        // সেশন রেডি হওয়া পর্যন্ত অপেক্ষা করবে এবং ইউজার আইডি পেলে ফ্লো চালু করবে
        return supabase.auth.sessionStatus
            .filterIsInstance<SessionStatus.Authenticated>()
            .mapNotNull { it.session.user?.id }
            .flatMapLatest { currentUserId ->
                // ১. ব্যাকগ্রাউন্ডে নেটওয়ার্ক থেকে নতুন চ্যাট ডেটা সিঙ্ক করা
                safeScope.launch { syncDashboardChats(currentUserId) }

                // ২. UI-কে সরাসরি Room Database-এর ফ্লো ধরিয়ে দেওয়া
                chatDao.observeAllChatsFlow(currentUserId).map { previews ->
                    previews.map { it.toDomainModel(currentUserId) }
                }
            }
    }

    private suspend fun syncDashboardChats(currentUserId: String) {
        try {
            val dtos = supabase.postgrest.rpc(
                function = "get_user_chat_previews",
                parameters = mapOf("current_user_id" to currentUserId)
            ).decodeList<ChatPreviewDto>()

            val chats = mutableListOf<ChatEntity>()
            val participants = mutableListOf<ChatParticipantEntity>()
            val messages = mutableListOf<ChatMessageEntity>()

            dtos.forEach { dto ->
                val timestamp = parseTimestamp(dto.last_message_time)
                val dummyMsgId = "msg_${dto.chat_id}_last"

                chats.add(ChatEntity(
                    id = dto.chat_id,
                    title = dto.other_user_name,
                    isGroup = false,
                    avatarUrl = dto.other_user_avatar,
                    lastMessageId = dummyMsgId,
                    lastMessageTime = timestamp,
                    createdBy = null, createdAt = null, updatedAt = System.currentTimeMillis()
                ))

                participants.add(ChatParticipantEntity(
                    chatId = dto.chat_id, userId = currentUserId, role = "member",
                    joinedAt = System.currentTimeMillis(), lastReadAt = null, clearedAt = null,
                    unreadCount = dto.unread_count ?: 0, isPinned = dto.is_pinned ?: false,
                    isMuted = dto.is_muted ?: false, isArchived = dto.is_archived ?: false,
                    isHidden = false, isLocked = false
                ))

                if (!dto.last_message_content.isNullOrBlank()) {
                    messages.add(ChatMessageEntity(
                        id = dummyMsgId, chatId = dto.chat_id, senderId = "unknown",
                        content = dto.last_message_content, type = MessageType.text,
                        status = MessageStatus.DELIVERED, createdAt = timestamp
                    ))
                }
            }

            chatDao.insertChats(chats)
            chatParticipantDao.insertParticipants(participants)
            chatMessageDao.insertMessages(messages)

        } catch (e: Exception) {
            Log.e("DashboardRepo", "Sync error", e)
        }
    }

    override suspend fun deleteChat(chatId: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatPinStatus(chatId: String, isPinned: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatFavoriteStatus(chatId: String, isFavorite: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatArchiveStatus(chatId: String, isArchived: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatMuteStatus(chatId: String, isMuted: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatBlockStatus(chatId: String, isBlocked: Boolean): Result<Unit> = Result.success(Unit)

    private fun parseTimestamp(isoString: String?): Long {
        if (isoString == null) return System.currentTimeMillis()
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
            format.parse(isoString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) { System.currentTimeMillis() }
    }
}
