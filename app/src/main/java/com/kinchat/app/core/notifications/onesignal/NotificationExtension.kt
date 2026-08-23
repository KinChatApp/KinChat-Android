package com.kinchat.app.core.notifications.onesignal

import android.util.Log
import androidx.annotation.Keep
import com.kinchat.app.KinChatApplication
import com.kinchat.app.core.notifications.fcm.FcmMessageProcessor
import com.kinchat.app.core.notifications.fcm.model.FcmMessagePayload
import com.onesignal.notifications.INotificationReceivedEvent
import com.onesignal.notifications.INotificationServiceExtension
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Keep
class NotificationExtension : INotificationServiceExtension {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationExtensionEntryPoint {
        fun getMessageProcessor(): FcmMessageProcessor
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationReceived(event: INotificationReceivedEvent) {
        val additionalData = event.notification.additionalData

        if (additionalData == null || !additionalData.has("chat_id")) {
            Log.d("OneSignalExtension", "Not a chat notification. Keeping OneSignal default handling.")
            return
        }

        val type = additionalData.optString("type")
        if (type != "chat" && type != "chat_message") {
            Log.d("OneSignalExtension", "Invalid type: $type. Keeping OneSignal default handling.")
            return
        }

        val payloadMap = mutableMapOf<String, String>()
        val keys = additionalData.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            payloadMap[key] = additionalData.optString(key)
        }

        val payload = FcmMessagePayload.from(payloadMap)

        if (payload == null) {
            Log.w("OneSignalExtension", "Invalid KinChat payload. Keeping default notification.")
            return
        }

        // 🚀 ONLY suppress if we successfully parsed a valid KinChat payload
        event.preventDefault()
        Log.d("OneSignalExtension", "KinChat notification intercepted: ${payload.messageId}")

        val context = KinChatApplication.instance
        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            NotificationExtensionEntryPoint::class.java
        )

        val processor = entryPoint.getMessageProcessor()

        scope.launch {
            try {
                processor.processAndNotify(payload)
            } catch (e: Exception) {
                Log.e("OneSignalExtension", "Custom notification processing failed", e)
                processor.showFallbackNotification(payload)
            }
        }
    }
}
