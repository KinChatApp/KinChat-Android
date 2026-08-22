package com.kinchat.app.data.repository

import com.kinchat.app.data.local.db.*
import com.kinchat.app.data.remote.model.UserProfileDto
import com.kinchat.app.data.repository.dashboard.sync.DashboardSyncManager
import com.kinchat.app.data.repository.dashboard.utils.DashboardConstants
import com.kinchat.app.domain.model.Chat
import com.kinchat.app.domain.model.UserProfile
import com.kinchat.app.domain.repository.DashboardRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
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
    private val syncManager = DashboardSyncManager(
        supabase = supabase,
        chatDao = chatDao,
        chatParticipantDao = chatParticipantDao,
        chatMessageDao = chatMessageDao
    )

    override suspend fun getCurrentUserId(): String? =
        supabase.auth.currentUserOrNull()?.id

    override suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            val dto = supabase.postgrest[DashboardConstants.DB_TABLE_USERS]
                .select { filter { eq(DashboardConstants.DB_FIELD_ID, userId) } }
                .decodeSingleOrNull<UserProfileDto>()

            dto?.let { UserProfile(id = it.id, avatarUrl = it.avatarUrl) }
        } catch (e: Exception) {
            null
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getRecentChats(): Flow<List<Chat>> {
        // অফলাইনের জন্য লোকাল ক্যাশ থেকে সরাসরি ইউজার আইডি নেওয়ার চেষ্টা
        val cachedUserId = supabase.auth.currentUserOrNull()?.id

        val userIdFlow = if (cachedUserId != null) {
            flowOf(cachedUserId)
        } else {
            supabase.auth.sessionStatus
                .filterIsInstance<SessionStatus.Authenticated>()
                .mapNotNull { it.session.user?.id }
        }

        return userIdFlow.flatMapLatest { currentUserId ->
            // ব্যাকগ্রাউন্ডে সিঙ্ক হবে, ফেইল হলেও ক্র্যাশ করবে না বা ফ্লো ব্লক করবে না
            safeScope.launch {
                try {
                    syncManager.syncDashboardChats(currentUserId)
                } catch (e: Exception) {
                    // অফলাইনে সিঙ্ক ফেইল হলে ইগনোর করবে
                }
            }
            
            // সাথে সাথে Room DB থেকে ডাটা রিটার্ন করবে
            chatDao.observeAllChatsFlow(currentUserId).map { previews ->
                previews.map { it.toDomainModel(currentUserId) }
            }
        }
    }

    override suspend fun deleteChat(chatId: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatPinStatus(chatId: String, isPinned: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatFavoriteStatus(chatId: String, isFavorite: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatArchiveStatus(chatId: String, isArchived: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatMuteStatus(chatId: String, isMuted: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatBlockStatus(chatId: String, isBlocked: Boolean): Result<Unit> = Result.success(Unit)
}
