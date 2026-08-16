package com.kinchat.app.data.repository.chat.sync.handlers

import android.net.Uri
import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.local.db.AttachmentDao
import com.kinchat.app.data.local.db.OperationType
import com.kinchat.app.data.local.db.PendingOperationEntity
import com.kinchat.app.data.local.db.UploadState
import com.kinchat.app.data.repository.chat.ChatMessageDbHelper
import com.kinchat.app.data.repository.chat.CloudinaryUploader
import kotlinx.coroutines.TimeoutCancellationException
import javax.inject.Inject

class AttachmentOperationHandler @Inject constructor(
    private val attachmentDao: AttachmentDao,
    private val uploader: CloudinaryUploader,
    private val dbHelper: ChatMessageDbHelper
) {
    suspend fun handle(op: PendingOperationEntity) {
        val messageId = op.referenceId

        // 🚀 FIX: payload এর বদলে payloadJson ব্যবহার করা হয়েছে
        val payloadParts = op.payloadJson?.split("|")
        val chatId = payloadParts?.getOrNull(0) ?: return
        val attachmentId = payloadParts?.getOrNull(1) ?: return

        val attachment = attachmentDao.getAttachmentById(attachmentId)
            ?: throw IllegalArgumentException("Attachment $attachmentId not found for message $messageId")

        // 🚀 Idempotency Check: আগে থেকে আপলোড হয়ে থাকলে শুধু SEND_MESSAGE কিউ করবে
        if (attachment.uploadState == UploadState.SUCCESS && attachment.fileUrl != null) {
            dbHelper.queuePendingOperation(OperationType.SEND_MESSAGE, messageId, null, System.currentTimeMillis())
            return
        }

        val localUri = attachment.localUri ?: return
        val uploadFolder = "kinchat_attachments/$chatId"

        try {
            attachmentDao.insertAttachment(attachment.copy(uploadState = UploadState.UPLOADING))

            val uploadResponse = uploader.uploadFile(Uri.parse(localUri), uploadFolder)
            val secureUrl = uploadResponse["secure_url"].toString()
            val publicId = uploadResponse["public_id"].toString()

            AppLogger.i("AttachmentHandler", "🎉 Upload SUCCESS! URL: $secureUrl")

            val updatedAttachment = attachment.copy(
                fileUrl = secureUrl,
                imageKitFileId = publicId,
                uploadState = enumValueOf<UploadState>("SUCCESS")
            )
            attachmentDao.insertAttachment(updatedAttachment)

            // আপলোড সফল হলে এরপর মেসেজ সেন্ড করার অপ কিউতে পাঠানো হচ্ছে
            dbHelper.queuePendingOperation(OperationType.SEND_MESSAGE, messageId, null, System.currentTimeMillis())
            AppLogger.i("AttachmentHandler", "✅ SEND_MESSAGE queued after upload")

        } catch (e: Exception) {
            val errorMessage = if (e is TimeoutCancellationException) "⏱️ Upload timed out" else "❌ Upload failed: ${e.message}"
            AppLogger.e("AttachmentHandler", errorMessage, e)
            attachmentDao.insertAttachment(attachment.copy(uploadState = enumValueOf<UploadState>("FAILED")))
            throw e // Exception থ্রো করা হচ্ছে যাতে Worker এটি রিস্টার্ট বা রিট্রাই করতে পারে
        }
    }
}
