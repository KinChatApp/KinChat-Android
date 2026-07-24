package com.kinchat.app.data.repository

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kinchat.app.data.local.db.*
import com.kinchat.app.domain.model.ChatMessage
import com.kinchat.app.domain.repository.ChatRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseClient: SupabaseClient,
    private val chatDao: ChatDao,
    private val chatMessageDao: ChatMessageDao,
    private val pendingOperationDao: PendingOperationDao,
    private val messageReactionDao: MessageReactionDao,
    private val syncManager: ChatSyncManager
) : ChatRepository {

    private val scope = CoroutineScope(Dispatchers.IO)
    
    // 🚀 FIXED: ৫টি প্যারামিটারই সঠিকভাবে পাস করা হয়েছে (chatDao যোগ করা হয়েছে)
    private val messageManager = ChatMessageManager(
        supabaseClient, 
        chatDao, 
        chatMessageDao, 
        pendingOperationDao, 
        messageReactionDao
    )

    override fun observeMessages(chatId: String): Flow<List<ChatMessage>> {
        scope.launch {
            syncManager.fetchMissedMessages(chatId)
            syncManager.startRealtimeListener(chatId)
        }
        return chatMessageDao.observeMessagesWithDetails(chatId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun sendMessage(chatId: String, senderId: String, content: String, replyToId: String?): Result<Unit> {
        val result = messageManager.sendMessage(chatId, senderId, content, replyToId)
        triggerPendingWorker()
        return result
    }

    override suspend fun editMessage(messageId: String, newContent: String): Result<Unit> {
        val timestamp = System.currentTimeMillis()
        chatMessageDao.updateMessageContent(messageId, newContent, timestamp)

        val pendingOp = PendingOperationEntity(
            id = UUID.randomUUID().toString(), type = OperationType.EDIT_MESSAGE,
            referenceId = messageId, payloadJson = newContent, createdAt = timestamp
        )
        pendingOperationDao.insertOperation(pendingOp)

        triggerPendingWorker()
        return Result.success(Unit)
    }

    override suspend fun getPartnerName(chatId: String, currentUserId: String): String? {
        try {
            val localTitle = chatDao.getChatTitle(chatId)
            if (!localTitle.isNullOrBlank()) {
                return localTitle
            }
        } catch (e: Exception) {
            Log.e("ChatRepo", "Local DB Error: ${e.message}")
        }

        return try {
            supabaseClient.postgrest.rpc(
                function = "get_chat_partner_name",
                parameters = mapOf("p_chat_id" to chatId, "p_current_user_id" to currentUserId)
            ).decodeAs<String>()
        } catch (e: Exception) {
            Log.e("ChatRepo", "RPC Error: ${e.message}")
            null
        }
    }

    override suspend fun deleteMessage(messageId: String, userId: String, deleteType: String): Result<Unit> = Result.success(Unit)
    override suspend fun addReaction(messageId: String, userId: String, reactionType: String): Result<Unit> = Result.success(Unit)
    override suspend fun createChatIfNotExists(partnerUserId: String): Result<String> = Result.success("")
    override suspend fun checkIsSaved(messageId: String, userId: String): Boolean = false
    override suspend fun toggleSaveMessage(messageId: String, userId: String): Result<Boolean> = Result.success(true)
    override suspend fun reportMessage(messageId: String, reporterId: String, reportedUserId: String, reason: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateLastRead(chatId: String, userId: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatPinStatus(chatId: String, isPinned: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatFavoriteStatus(chatId: String, isFavorite: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatArchiveStatus(chatId: String, isArchived: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatMuteStatus(chatId: String, isMuted: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun updateChatBlockStatus(chatId: String, isBlocked: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun deleteChatParticipant(chatId: String): Result<Unit> = Result.success(Unit)

    // 🚀 FIXED: Network Constraints যুক্ত করা হয়েছে
    private fun triggerPendingWorker() {
        // ১. শর্ত দেওয়া হলো যে ইন্টারনেট কানেকশন থাকতেই হবে
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<PendingOperationWorker>()
            .setConstraints(constraints)
            .build()

        // ২. enqueueUniqueWork ব্যবহার করা হয়েছে যেন বারবার একই ওয়ার্কার কল না হয়
        WorkManager.getInstance(context).enqueueUniqueWork(
            "SyncPendingOperations",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
