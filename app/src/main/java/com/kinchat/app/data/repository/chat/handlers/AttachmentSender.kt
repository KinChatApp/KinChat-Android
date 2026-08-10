package com.kinchat.app.data.repository.chat.handlers

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
    private val uploader: CloudinaryUploader,
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
        fileBytes: ByteArray,
        replyToId: String? = null
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
            content = fileName,
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
            uploadState = UploadState.UPLOADING
        )
        attachmentDao.insertAttachment(attachment)

        dbHelper.ensureChatExistsAndUpdateLastMessage(chatId, senderId, messageId, timestamp)

        val uploadFolder = "kinchat_attachments/$chatId"

        try {
            val uploadResponse = uploader.uploadFile(fileBytes, uploadFolder)
            val secureUrl = uploadResponse["secure_url"].toString()
            val publicId = uploadResponse["public_id"].toString()

            AppLogger.i("AttachmentSender", "🎉 [3/3] Upload SUCCESS! URL: $secureUrl")

            val updatedAttachment = attachment.copy(
                fileUrl = secureUrl,
                imageKitFileId = publicId,
                uploadState = enumValueOf<UploadState>("SUCCESS")
            )
            attachmentDao.insertAttachment(updatedAttachment)
            
            dbHelper.queuePendingOperation(OperationType.SEND_MESSAGE, messageId, null, System.currentTimeMillis())
            AppLogger.i("AttachmentSender", "✅ SEND_MESSAGE queued")
        } catch (e: Exception) {
            val errorMessage = if (e is TimeoutCancellationException) "⏱️ Upload timed out" else "❌ Upload failed: ${e.message}"
            AppLogger.e("AttachmentSender", errorMessage, e)
            attachmentDao.insertAttachment(attachment.copy(uploadState = enumValueOf<UploadState>("FAILED")))
            throw e
        }
    }.onSuccess {
        AppLogger.i("AttachmentSender", "✅ Attachment message processed successfully")
    }.onFailure {
        AppLogger.e("AttachmentSender", "❌ Failed to process attachment message", it)
    }
}
