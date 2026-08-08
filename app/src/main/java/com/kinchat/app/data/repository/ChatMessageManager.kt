package com.kinchat.app.data.repository

import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.local.db.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Serializable
data class CloudinaryAuthPayload(
    val folder: String
)

@Serializable
data class CloudinaryAuthResponse(
    val signature: String,
    val timestamp: Long,
    val apiKey: String
)

class ChatMessageManager(
    private val supabaseClient: SupabaseClient,
    private val chatDao: ChatDao,
    private val chatMessageDao: ChatMessageDao,
    private val attachmentDao: AttachmentDao,
    private val pendingOperationDao: PendingOperationDao,
    private val messageReactionDao: MessageReactionDao,
    private val chatInsightsDao: ChatInsightsDao
) {
    private val savedMessagesCache = ConcurrentHashMap<String, Boolean>()

    // ============================================================
    // TEXT MESSAGE
    // ============================================================

    suspend fun sendMessage(
        messageId: String,
        chatId: String,
        senderId: String,
        content: String,
        replyToId: String?
    ): Result<Unit> = runCatching {

        AppLogger.d("MsgManager", "Saving new message to local DB: $messageId")

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

        val dummyChat = ChatEntity(
            id = chatId,
            title = "New Chat",
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
        chatDao.updateLastMessageInfo(chatId, messageId, timestamp)

        val wordCount = content.split("\\s+".toRegex()).count { it.isNotEmpty() }
        val charCount = content.length
        val isoTimestamp = Instant.ofEpochMilli(timestamp).toString()

        chatInsightsDao.incrementMyMessageCount(
            chatId,
            wordCount,
            charCount,
            isoTimestamp
        )

        val pendingOp = PendingOperationEntity(
            id = UUID.randomUUID().toString(),
            type = OperationType.SEND_MESSAGE,
            referenceId = messageId,
            payloadJson = null,
            createdAt = timestamp
        )

        pendingOperationDao.insertOperation(pendingOp)

        Unit
    }.onSuccess {
        AppLogger.i("MsgManager", "✅ Message $messageId saved locally & added to PendingSync")
    }.onFailure {
        AppLogger.e("MsgManager", "❌ Failed to save message locally: $messageId", it)
    }

    // ============================================================
    // CLOUDINARY SIGNED UPLOAD HELPER
    // ============================================================

    private suspend fun uploadToCloudinary(
        fileBytes: ByteArray,
        uploadFolder: String,
        signature: String,
        timestamp: Long,
        apiKey: String
    ): Map<*, *> = suspendCoroutine { continuation ->

        AppLogger.d("MsgManager", "🚀 Cloudinary Signed Upload called")

        try {
            MediaManager.get().upload(fileBytes)
                .option("folder", uploadFolder)
                .option("resource_type", "auto")
                .option("signature", signature)
                .option("timestamp", timestamp)
                .option("api_key", apiKey)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        AppLogger.d("MsgManager", "🚀 Upload started: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        AppLogger.i("MsgManager", "🎉 Cloudinary onSuccess fired!")
                        continuation.resume(resultData)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        val msg = error.description ?: "Unknown Error"
                        AppLogger.e("MsgManager", "❌ Cloudinary onError: $msg")
                        continuation.resumeWithException(Exception(msg))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        continuation.resumeWithException(Exception("Upload rescheduled"))
                    }
                })
                .dispatch()

        } catch (e: Exception) {
            AppLogger.e("MsgManager", "💥 Cloudinary upload threw exception", e)
            continuation.resumeWithException(e)
        }
    }

    // ============================================================
    // ATTACHMENT MESSAGE
    // ============================================================

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

        AppLogger.d("MsgManager", "Saving attachment message to local DB: $messageId")
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

        val dummyChat = ChatEntity(
            id = chatId,
            title = "New Chat",
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
        chatDao.updateLastMessageInfo(chatId, messageId, timestamp)

        val uploadFolder = "kinchat_attachments/$chatId"

        try {
            // ====================================================
            // STEP 1: GET SIGNATURE FROM SUPABASE EDGE FUNCTION
            // ====================================================
            AppLogger.d("MsgManager", "🔍 [1/3] Requesting Cloudinary Signature...")

            val authPayload = CloudinaryAuthPayload(folder = uploadFolder)
            val authResponse = withTimeout(15_000L) {
                supabaseClient.functions.invoke(
                    function = "cloudinary-auth", // ⚠️ নতুন এজ ফাংশনের নাম
                    body = authPayload
                )
            }

            val authData = authResponse.body<CloudinaryAuthResponse>()

            if (authData.signature.isBlank()) {
                throw IllegalStateException("Cloudinary signature is empty")
            }
            AppLogger.d("MsgManager", "✅ [1/3] Signature received")

            // ====================================================
            // STEP 2: CLOUDINARY UPLOAD (SIGNED)
            // ====================================================
            AppLogger.d("MsgManager", "🔍 [2/3] Starting Signed Upload...")

            val uploadResponse = withTimeout(120_000L) {
                withContext(Dispatchers.IO) {
                    uploadToCloudinary(
                        fileBytes = fileBytes,
                        uploadFolder = uploadFolder,
                        signature = authData.signature,
                        timestamp = authData.timestamp,
                        apiKey = authData.apiKey
                    )
                }
            }

            // ====================================================
            // STEP 3: UPDATE DB & QUEUE SYNC
            // ====================================================
            val secureUrl = uploadResponse["secure_url"].toString()
            val publicId = uploadResponse["public_id"].toString()

            AppLogger.i("MsgManager", "🎉 [3/3] Upload SUCCESS! URL: $secureUrl")

            val updatedAttachment = attachment.copy(
                fileUrl = secureUrl,
                imageKitFileId = publicId, // ডাটাবেসের আগের কলাম নামই রাখলাম
                uploadState = enumValueOf<UploadState>("SUCCESS")
            )
            attachmentDao.insertAttachment(updatedAttachment)

            val pendingOp = PendingOperationEntity(
                id = UUID.randomUUID().toString(),
                type = OperationType.SEND_MESSAGE,
                referenceId = messageId,
                payloadJson = null,
                createdAt = System.currentTimeMillis()
            )
            pendingOperationDao.insertOperation(pendingOp)
            AppLogger.i("MsgManager", "✅ SEND_MESSAGE queued")

        } catch (e: TimeoutCancellationException) {
            AppLogger.e("MsgManager", "⏱️ Upload timed out", e)
            attachmentDao.insertAttachment(attachment.copy(uploadState = enumValueOf<UploadState>("FAILED")))
            throw e
        } catch (e: Exception) {
            AppLogger.e("MsgManager", "❌ Upload failed: ${e.message}", e)
            attachmentDao.insertAttachment(attachment.copy(uploadState = enumValueOf<UploadState>("FAILED")))
            throw e
        }
        Unit
    }.onSuccess {
        AppLogger.i("MsgManager", "✅ Attachment message processed successfully")
    }.onFailure {
        AppLogger.e("MsgManager", "❌ Failed to process attachment message", it)
    }

    // ============================================================
    // REACTION
    // ============================================================

    suspend fun addReaction(messageId: String, userId: String, reactionType: String): Result<Unit> = runCatching {
        AppLogger.d("MsgManager", "Adding reaction $reactionType to msg: $messageId")
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
        Unit
    }.onFailure {
        AppLogger.e("MsgManager", "Failed to add reaction", it)
    }

    // ============================================================
    // DELETE MESSAGE
    // ============================================================

    suspend fun deleteMessage(messageId: String, userId: String, deleteType: String): Result<Unit> = runCatching {
        AppLogger.d("MsgManager", "Soft deleting message: $messageId")
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
        Unit
    }.onFailure {
        AppLogger.e("MsgManager", "Failed to delete message", it)
    }

    suspend fun toggleSaveMessage(messageId: String, userId: String): Result<Boolean> = Result.success(true)
    suspend fun checkIsSaved(messageId: String, userId: String): Boolean = false
    suspend fun reportMessage(messageId: String, reporterId: String, reportedUserId: String, reason: String): Result<Unit> = Result.success(Unit)
}
