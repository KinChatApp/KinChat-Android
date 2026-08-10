package com.kinchat.app.data.remote.api

import android.content.Context
import android.widget.Toast
import com.kinchat.app.core.logging.AppLogger
// 🚀 Fixed import path for SendMessageRequest
import com.kinchat.app.data.repository.chat.SendMessageRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface ChatNotificationService {
    suspend fun sendNotification(chatId: String, messageId: String, senderId: String, content: String, replyToId: String?)
}

@Singleton
class ChatNotificationServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseClient: SupabaseClient
) : ChatNotificationService {

    companion object {
        private const val EDGE_FUNCTION_NAME = "send-notification"
    }

    override suspend fun sendNotification(
        chatId: String,
        messageId: String,
        senderId: String,
        content: String,
        replyToId: String?
    ) {
        try {
            AppLogger.d("ChatNotification", "Calling Edge Function '$EDGE_FUNCTION_NAME' for msg: $messageId")
            val requestPayload = SendMessageRequest(
                chatId = chatId,
                messageId = messageId,
                senderId = senderId,
                content = content,
                replyToId = replyToId
            )

            supabaseClient.functions.invoke(EDGE_FUNCTION_NAME) {
                contentType(ContentType.Application.Json)
                setBody(requestPayload)
            }
            AppLogger.i("ChatNotification", "✅ Edge Function called successfully for $messageId")

        } catch (e: ResponseException) {
            val status = e.response.status
            val errorBody = try { e.response.bodyAsText() } catch (ex: Exception) { "No error body" }
            AppLogger.e("ChatNotification", "❌ Edge Function HTTP Error: ${status.value} - $errorBody", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Server Error ${status.value}: $errorBody", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            AppLogger.e("ChatNotification", "🚨 Edge Function Error: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error: ${e.message ?: e.javaClass.simpleName}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
