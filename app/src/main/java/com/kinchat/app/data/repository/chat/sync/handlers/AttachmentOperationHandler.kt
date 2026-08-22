package com.kinchat.app.data.repository.chat.sync.handlers

import android.content.Context
import android.net.Uri
import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.local.db.AttachmentDao
import com.kinchat.app.data.local.db.OperationType
import com.kinchat.app.data.local.db.PendingOperationEntity
import com.kinchat.app.data.local.db.UploadState
import com.kinchat.app.data.repository.chat.ChatMessageDbHelper
import com.kinchat.app.data.repository.chat.CloudinaryUploader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import javax.inject.Inject

class AttachmentOperationHandler @Inject constructor(
    private val attachmentDao: AttachmentDao,
    private val uploader: CloudinaryUploader,
    private val dbHelper: ChatMessageDbHelper,
    @ApplicationContext private val context: Context // 🚀 Added Context to check file
) {
    suspend fun handle(op: PendingOperationEntity) {
        val messageId = op.referenceId

        val payloadParts = op.payloadJson?.split("|")
        val chatId = payloadParts?.getOrNull(0) ?: return
        val attachmentId = payloadParts?.getOrNull(1) ?: return

        val attachment = attachmentDao.getAttachmentById(attachmentId)
            ?: throw IllegalArgumentException("Attachment $attachmentId not found for message $messageId")

        if (attachment.uploadState == UploadState.SUCCESS && attachment.fileUrl != null) {
            dbHelper.queuePendingOperation(OperationType.SEND_MESSAGE, messageId, null, System.currentTimeMillis())
            return
        }

        val localUriStr = attachment.localUri ?: return
        val uploadFolder = "kinchat_attachments/$chatId"

        // 🚀 PRO-FIX: Verify if the file still exists locally before attempting upload
        val fileUri = Uri.parse(localUriStr)
        val isFileReadable = try {
            context.contentResolver.openFileDescriptor(fileUri, "r")?.use { true } ?: false
        } catch (e: Exception) {
            false
        }

        if (!isFileReadable) {
            AppLogger.e("AttachmentHandler", "❌ File not found or unreadable: $localUriStr")
            attachmentDao.insertAttachment(attachment.copy(uploadState = enumValueOf<UploadState>("FAILED")))
            // Throw IllegalArgumentException so the PendingWorker treats it as unrecoverable and marks it DEAD
            throw IllegalArgumentException("Unrecoverable file missing error for URI: $localUriStr")
        }

        try {
            attachmentDao.insertAttachment(attachment.copy(uploadState = UploadState.UPLOADING))

            val uploadResponse = uploader.uploadFile(fileUri, uploadFolder)
            val secureUrl = uploadResponse["secure_url"].toString()
            val publicId = uploadResponse["public_id"].toString()

            AppLogger.i("AttachmentHandler", "🎉 Upload SUCCESS! URL: $secureUrl")

            val updatedAttachment = attachment.copy(
                fileUrl = secureUrl,
                imageKitFileId = publicId,
                uploadState = enumValueOf<UploadState>("SUCCESS")
            )
            attachmentDao.insertAttachment(updatedAttachment)

            dbHelper.queuePendingOperation(OperationType.SEND_MESSAGE, messageId, null, System.currentTimeMillis())
            AppLogger.i("AttachmentHandler", "✅ SEND_MESSAGE queued after upload")

        } catch (e: Exception) {
            val errorMessage = if (e is TimeoutCancellationException) "⏱️ Upload timed out" else "❌ Upload failed: ${e.message}"
            AppLogger.e("AttachmentHandler", errorMessage, e)
            attachmentDao.insertAttachment(attachment.copy(uploadState = enumValueOf<UploadState>("FAILED")))
            throw e 
        }
    }
}
