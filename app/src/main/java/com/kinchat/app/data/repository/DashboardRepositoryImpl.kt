package com.kinchat.app.data.repository

import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.local.datastore.AuthPreferencesManager
import com.kinchat.app.data.local.db.*
import com.kinchat.app.data.remote.model.UserProfileDto
import com.kinchat.app.data.repository.dashboard.sync.DashboardSyncManager
import com.kinchat.app.data.repository.dashboard.utils.DashboardConstants
import com.kinchat.app.domain.model.Chat
import com.kinchat.app.domain.model.UserProfile
import com.kinchat.app.domain.repository.DashboardRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val userDao: UserDao,
    private val chatDao: ChatDao,
    private val chatParticipantDao: ChatParticipantDao,
    private val chatMessageDao: ChatMessageDao,
    private val authPreferencesManager: AuthPreferencesManager
) : DashboardRepository {
    private val safeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val syncManager = DashboardSyncManager(
        supabase = supabase,
        chatDao = chatDao,
        chatParticipantDao = chatParticipantDao,
        chatMessageDao = chatMessageDao
    )

    override suspend fun getCurrentUserId(): String? =
        authPreferencesManager.meId.firstOrNull()

    override fun observeUserProfile(userId: String): Flow<UserProfile?> {
        safeScope.launch {
            try {
                val dto = supabase.postgrest[DashboardConstants.DB_TABLE_USERS]
                    .select { filter { eq(DashboardConstants.DB_FIELD_ID, userId) } }
                    .decodeSingleOrNull<UserProfileDto>()
                if (dto != null) {
                    val existingUser = userDao.getUsersByIds(listOf(userId)).firstOrNull()
                    val userEntity = existingUser?.copy(
                        avatarUrl = dto.avatarUrl,
                        updatedAt = System.currentTimeMillis()
                    ) ?: UserEntity(
                        id = dto.id,
                        displayName = "User",
                        username = null,
                        phone = null,
                        bio = null,
                        avatarUrl = dto.avatarUrl,
                        updatedAt = System.currentTimeMillis()
                    )
                    userDao.insertUser(userEntity)
                }
            } catch (e: Exception) {
                // অফলাইনে সিঙ্ক ফেইল হলে ইগনোর করবে
            }
        }
        return userDao.observeUser(userId).map { entity ->
            entity?.let { UserProfile(id = it.id, avatarUrl = it.avatarUrl) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val cachedChatsFlow: StateFlow<List<Chat>> = authPreferencesManager.meId
        .filterNotNull()
        .filter { it.isNotBlank() }
        .distinctUntilChanged()
        .onEach { AppLogger.d("DataFlowLog", "DashboardRepository: meId received -> \$it") }
        .flatMapLatest { currentUserId ->
            safeScope.launch {
                try {
                    AppLogger.d("DataFlowLog", "DashboardRepository: Starting syncDashboardChats")
                    syncManager.syncDashboardChats(currentUserId)
                    AppLogger.d("DataFlowLog", "DashboardRepository: Finished syncDashboardChats")
                } catch (e: Exception) {}
            }

            AppLogger.d("DataFlowLog", "DashboardRepository: Calling observeAllChatsFlow")
            chatDao.observeAllChatsFlow(currentUserId)
                .onEach { AppLogger.d("DataFlowLog", "DashboardRepository: chatDao emitted \${it.size} items") }
                .map { previews ->
                    previews.map { it.toDomainModel(currentUserId) }
                }
                .distinctUntilChanged()
        }
        .stateIn(
            scope = safeScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    override fun getRecentChats(): StateFlow<List<Chat>> = cachedChatsFlow
    override suspend fun deleteChat(chatId: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatPinStatus(chatId: String, isPinned: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatFavoriteStatus(chatId: String, isFavorite: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatArchiveStatus(chatId: String, isArchived: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatMuteStatus(chatId: String, isMuted: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatBlockStatus(chatId: String, isBlocked: Boolean): Result<Unit> = Result.success(Unit)
}
