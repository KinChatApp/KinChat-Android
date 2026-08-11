package com.kinchat.app.data.repository.chat

import com.kinchat.app.data.local.db.AttachmentDao
import com.kinchat.app.data.local.db.ChatDao
import com.kinchat.app.data.local.db.ChatInsightsDao
import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.local.db.MessageReactionDao
import com.kinchat.app.data.local.db.PendingOperationDao
import com.kinchat.app.data.repository.chat.handlers.AttachmentSender
import com.kinchat.app.data.repository.chat.handlers.MessageActionManager
import com.kinchat.app.data.repository.chat.handlers.MessageSender
import com.kinchat.app.data.repository.chat.handlers.ReactionManager
import io.github.jan.supabase.SupabaseClient

class ChatMessageManager(
    private val supabaseClient: SupabaseClient,
    private val chatDao: ChatDao,
    private val chatMessageDao: ChatMessageDao,
    private val attachmentDao: AttachmentDao,
    private val pendingOperationDao: PendingOperationDao,
    private val messageReactionDao: MessageReactionDao,
    private val chatInsightsDao: ChatInsightsDao
) {
    private val dbHelper = ChatMessageDbHelper(chatDao, pendingOperationDao)
    private val uploader = CloudinaryUploader(supabaseClient)

    private val messageSender = MessageSender(chatMessageDao, chatInsightsDao, dbHelper)
    
    private val attachmentSender = AttachmentSender(chatMessageDao, attachmentDao, uploader, dbHelper)
    
    private val reactionManager = ReactionManager(messageReactionDao, dbHelper)
    
    private val actionManager = MessageActionManager(chatMessageDao, dbHelper)

    suspend fun sendMessage(
        messageId: String,
        chatId: String,
        senderId: String,
        content: String,
        replyToId: String?
    ): Result<Unit> = messageSender.sendMessage(messageId, chatId, senderId, content, replyToId)

    suspend fun sendAttachmentMessage(
        messageId: String,
        chatId: String,
        senderId: String,
        localUri: String,
        mimeType: String,
        fileName: String,
        fileSize: Long,
        replyToId: String? = null,
        caption: String? = null
    ): Result<Unit> = attachmentSender.sendAttachmentMessage(
        messageId, chatId, senderId, localUri, mimeType, fileName, fileSize, replyToId, caption
    )

    suspend fun addReaction(messageId: String, userId: String, reactionType: String): Result<Unit> = 
        reactionManager.addReaction(messageId, userId, reactionType)

    suspend fun deleteMessage(messageId: String, userId: String, deleteType: String): Result<Unit> = 
        actionManager.deleteMessage(messageId, userId, deleteType)

    suspend fun toggleSaveMessage(messageId: String, userId: String): Result<Boolean> = 
        actionManager.toggleSaveMessage(messageId, userId)
    
    suspend fun checkIsSaved(messageId: String, userId: String): Boolean = 
        actionManager.checkIsSaved(messageId, userId)
    
    suspend fun reportMessage(messageId: String, reporterId: String, reportedUserId: String, reason: String): Result<Unit> = 
        actionManager.reportMessage(messageId, reporterId, reportedUserId, reason)
}
