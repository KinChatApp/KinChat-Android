package com.kinchat.app.core.notifications.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kinchat.app.core.notifications.fcm.model.FcmMessagePayload
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "Message received from FCM: ${remoteMessage.data}")

        // 🚀 FIX (P13): Replace fragile "zego" substring filter with explicit type check
        if (remoteMessage.data["type"] == "call") {
            Log.d(TAG, "Call payload detected. Ignoring in custom FCM processor.")
            return
        }

        val payload = FcmMessagePayload.from(remoteMessage.data) ?: return

        val wakeLock = wakeLockManager.acquireWakeLock(
            tag = WAKE_LOCK_TAG,
            timeoutMs = WORK_TIMEOUT_MS + WAKE_LOCK_SAFETY_MARGIN_MS
        )

        scope.launch {
            try {
                // 🚀 FIX (RC6): Use bounded withTimeout to strictly enforce the 8s budget
                withTimeout(WORK_TIMEOUT_MS) {
                    messageProcessor.processAndNotify(payload)
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Message processing timed out, falling back to plain notification", e)
                // 🚀 FIX: Protect fallback from onDestroy/job.cancel() using NonCancellable
                withContext(NonCancellable) {
                    messageProcessor.showFallbackNotification(payload)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process message for chatId=${payload.chatId}", e)
                withContext(NonCancellable) {
                    messageProcessor.showFallbackNotification(payload)
                }
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
