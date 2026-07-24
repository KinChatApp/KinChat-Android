package com.kinchat.app.data.repository

import com.kinchat.app.data.local.db.*
import io.github.jan.supabase.SupabaseClient
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ChatMessageManager(
    private val supabaseClient: SupabaseClient,
    private val chatDao: ChatDao, // 🚀 FIXED: ChatDao যোগ করা হলো
    private val chatMessageDao: ChatMessageDao,
    private val pendingOperationDao: PendingOperationDao,
    private val messageReactionDao: MessageReactionDao
) {
    private val savedMessagesCache = ConcurrentHashMap<String, Boolean>()

    suspend fun sendMessage(chatId: String, senderId: String, content: String, replyToId: String?): Result<Unit> = runCatching {
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val entity = ChatMessageEntity(
            id = messageId,
            chatId = chatId,
            senderId = senderId,
            content = content,
            type = MessageType.text,
            status = MessageStatus.PENDING,
            replyToId = replyToId,
            createdAt = timestamp
        )
        chatMessageDao.insertMessage(entity)

        // 🚀 NEW FIX: ড্যাশবোর্ডে সাথে সাথে দেখানোর জন্য লোকাল ডাটাবেসে ডামি চ্যাট তৈরি করা হচ্ছে (যদি না থাকে)
        val dummyChat = ChatEntity(
            id = chatId,
            title = "New Chat", // ব্যাকগ্রাউন্ড সিঙ্ক হওয়ার পর আসল নাম অটোমেটিক বসে যাবে
            isGroup = false,
            avatarUrl = null,
            lastMessageId = messageId,
            lastMessageTime = timestamp,
            createdBy = senderId,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        chatDao.insertChatIfNotExists(dummyChat)
        chatDao.insertLocalParticipant(chatId, senderId, timestamp)

        // 🚀 FIXED: এখন এই আপডেটটি সফলভাবে কাজ করবে কারণ উপরের কোড নিশ্চিত করেছে যে চ্যাট ডাটাবেসে আছে
        chatDao.updateLastMessageInfo(chatId, messageId, timestamp)

        val pendingOp = PendingOperationEntity(
            id = UUID.randomUUID().toString(),
            type = OperationType.SEND_MESSAGE,
            referenceId = messageId,
            payloadJson = null,
            createdAt = timestamp
        )
        pendingOperationDao.insertOperation(pendingOp)
    }

    suspend fun addReaction(messageId: String, userId: String, reactionType: String): Result<Unit> = runCatching {
        val timestamp = System.currentTimeMillis()
        val reactionEnum = enumValueOf<ReactionType>(reactionType)

        val reactionEntity = MessageReactionEntity(
            messageId = messageId,
            userId = userId,
            reaction = reactionEnum,
            createdAt = timestamp,
            isSynced = false
        )
        messageReactionDao.insertReaction(reactionEntity)

        val pendingOp = PendingOperationEntity(
            id = UUID.randomUUID().toString(),
            type = OperationType.ADD_REACTION,
            referenceId = "${messageId}_${userId}",
            payloadJson = reactionType,
            createdAt = timestamp
        )
        pendingOperationDao.insertOperation(pendingOp)
    }

    suspend fun deleteMessage(messageId: String, userId: String, deleteType: String): Result<Unit> = runCatching {
        val timestamp = System.currentTimeMillis()
        chatMessageDao.softDeleteMessage(messageId, timestamp)

        if (deleteType == "for_everyone") {
            val pendingOp = PendingOperationEntity(
                id = UUID.randomUUID().toString(),
                type = OperationType.DELETE_MESSAGE,
                referenceId = messageId,
                payloadJson = null,
                createdAt = timestamp
            )
            pendingOperationDao.insertOperation(pendingOp)
        }
    }

    suspend fun toggleSaveMessage(messageId: String, userId: String): Result<Boolean> = runCatching { true }
    suspend fun checkIsSaved(messageId: String, userId: String): Boolean = false
    suspend fun reportMessage(messageId: String, reporterId: String, reportedUserId: String, reason: String): Result<Unit> = Result.success(Unit)
}
