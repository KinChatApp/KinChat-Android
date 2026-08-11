package com.kinchat.app.data.repository.chat

import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.kinchat.app.core.logging.AppLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CloudinaryUploader(
    private val supabaseClient: SupabaseClient
) {
    suspend fun uploadFile(
        uri: Uri,
        uploadFolder: String
    ): Map<*, *> {
        AppLogger.d("CloudinaryUploader", "🔍 [1/3] Requesting Cloudinary Signature...")
        
        val authData = getSignature(uploadFolder)
        AppLogger.d("CloudinaryUploader", "✅ [1/3] Signature received")
        
        AppLogger.d("CloudinaryUploader", "🔍 [2/3] Starting Signed Upload...")
        return withTimeout(120_000L) {
            withContext(Dispatchers.IO) {
                executeUpload(
                    uri = uri,
                    uploadFolder = uploadFolder,
                    signature = authData.signature,
                    timestamp = authData.timestamp,
                    apiKey = authData.apiKey
                )
            }
        }
    }

    private suspend fun getSignature(uploadFolder: String): CloudinaryAuthResponse {
        val authPayload = CloudinaryAuthPayload(folder = uploadFolder)
        val authResponse = withTimeout(15_000L) {
            supabaseClient.functions.invoke(
                function = "cloudinary-auth",
                body = authPayload
            )
        }
        val authData = authResponse.body<CloudinaryAuthResponse>()

        if (authData.signature.isBlank()) {
            throw IllegalStateException("Cloudinary signature is empty")
        }
        return authData
    }

    private suspend fun executeUpload(
        uri: Uri,
        uploadFolder: String,
        signature: String,
        timestamp: Long,
        apiKey: String
    ): Map<*, *> = suspendCoroutine { continuation ->
        AppLogger.d("CloudinaryUploader", "🚀 Cloudinary Signed Upload called")

        try {
            MediaManager.get().upload(uri)
                .option("folder", uploadFolder)
                .option("resource_type", "auto")
                .option("signature", signature)
                .option("timestamp", timestamp)
                .option("api_key", apiKey)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        AppLogger.d("CloudinaryUploader", "🚀 Upload started: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        AppLogger.i("CloudinaryUploader", "🎉 Cloudinary onSuccess fired!")
                        continuation.resume(resultData)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        val msg = error.description ?: "Unknown Error"
                        AppLogger.e("CloudinaryUploader", "❌ Cloudinary onError: $msg")
                        continuation.resumeWithException(Exception(msg))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        continuation.resumeWithException(Exception("Upload rescheduled"))
                    }
                })
                .dispatch()

        } catch (e: Exception) {
            AppLogger.e("CloudinaryUploader", "💥 Cloudinary upload threw exception", e)
            continuation.resumeWithException(e)
        }
    }
}
