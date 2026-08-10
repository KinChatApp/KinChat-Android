package com.kinchat.app.core.notifications.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kinchat.app.core.notifications.fcm.model.FcmMessagePayload
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@AndroidEntryPoint
class KinChatMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var tokenManager: FcmTokenManager

    @Inject
    lateinit var messageProcessor: FcmMessageProcessor

    @Inject
    lateinit var wakeLockManager: WakeLockManager

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            try {
                tokenManager.processNewToken(token)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process new token", e)
            }
        }
    }

    // 🚀 FIX (Preserved): WakeLock is held manually until notify() ends or times out.
    // This prevents CPU Doze mode or aggressive OS freezing (like Vivo) from 
    // terminating the process before the notification is properly shown.
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "Message received from FCM: ${remoteMessage.data}")

        val payload = FcmMessagePayload.from(remoteMessage.data) ?: return
        
        val wakeLock = wakeLockManager.acquireWakeLock(
            tag = WAKE_LOCK_TAG, 
            timeoutMs = WORK_TIMEOUT_MS + WAKE_LOCK_SAFETY_MARGIN_MS
        )

        scope.launch {
            try {
                val handled = withTimeoutOrNull(WORK_TIMEOUT_MS) {
                    messageProcessor.processAndNotify(payload)
                    true
                }

                // If DB/Network response times out, show a fallback notification
                if (handled == null) {
                    messageProcessor.showFallbackNotification(payload)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process message for chatId=${payload.chatId}", e)
                messageProcessor.showFallbackNotification(payload)
            } finally {
                wakeLockManager.releaseWakeLock(wakeLock)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    companion object {
        private const val TAG = "KinChatFCM"
        private const val WAKE_LOCK_TAG = "KinChat:FcmMessageWakeLock"
        private const val WORK_TIMEOUT_MS = 8000L
        private const val WAKE_LOCK_SAFETY_MARGIN_MS = 3000L
    }
}
