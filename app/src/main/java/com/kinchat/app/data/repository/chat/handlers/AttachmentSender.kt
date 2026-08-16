package com.kinchat.app.data.repository.chat.handlers

import android.net.Uri
import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.local.db.AttachmentDao
import com.kinchat.app.data.local.db.AttachmentEntity
import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.local.db.ChatMessageEntity
import com.kinchat.app.data.local.db.MessageStatus
import com.kinchat.app.data.local.db.MessageType
import com.kinchat.app.data.local.db.OperationType
import com.kinchat.app.data.local.db.UploadState
import com.kinchat.app.data.repository.chat.ChatMessageDbHelper
import com.kinchat.app.data.repository.chat.CloudinaryUploader
import kotlinx.coroutines.TimeoutCancellationException
import java.util.UUID

class AttachmentSender(
    private val chatMessageDao: ChatMessageDao,
    private val attachmentDao: AttachmentDao,
    private val uploader: CloudinaryUploader, // Hilt DI না ভাঙার জন্য রাখা হলো
    private val dbHelper: ChatMessageDbHelper
) {
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
    ): Result<Unit> = runCatching {
        AppLogger.d("AttachmentSender", "Saving attachment message to local DB: $messageId")
        val timestamp = System.currentTimeMillis()

        val msgType = when {
            mimeType.startsWith("image/") -> MessageType.image
            mimeType.startsWith("video/") -> MessageType.video
            else -> MessageType.document
        }

        val entity = ChatMessageEntity(
            id = messageId,
            chatId = chatId,
            senderId = senderId,
            content = caption?.takeIf { it.isNotBlank() } ?: fileName,
            type = msgType,
            status = MessageStatus.PENDING,
            replyToId = replyToId,
            createdAt = timestamp
        )
        chatMessageDao.insertMessage(entity)
        
        val attachmentId = UUID.randomUUID().toString()
        val attachment = AttachmentEntity(
            id = attachmentId,
            messageId = messageId,
            fileUrl = null,
            mimeType = mimeType,
            fileName = fileName,
            fileSize = fileSize,
            createdAt = timestamp,
            localUri = localUri,
            uploadState = UploadState.PENDING // 🚀 FIXED: UPLOADING এর বদলে PENDING রাখা হলো
        )
        attachmentDao.insertAttachment(attachment)

        dbHelper.ensureChatExistsAndUpdateLastMessage(chatId, senderId, messageId, timestamp)

        // 🚀 FIXED (Phase 4): Queue UPLOAD_ATTACHMENT operation immediately offline!
        // chatId এবং attachmentId দুটোই payload এ পাঠানো হচ্ছে যেন Worker ফোল্ডার পাথ ঠিকমত পায়।
        val payload = "$chatId|$attachmentId"
        dbHelper.queuePendingOperation(OperationType.UPLOAD_ATTACHMENT, messageId, payload, timestamp)
        
        AppLogger.i("AttachmentSender", "✅ UPLOAD_ATTACHMENT queued offline")
        
    }.onSuccess {
        AppLogger.i("AttachmentSender", "✅ Attachment message processed successfully to outbox")
    }.onFailure {
        AppLogger.e("AttachmentSender", "❌ Failed to process attachment message", it)
    }
}
