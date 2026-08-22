package com.kinchat.app.core.notifications.onesignal

import android.util.Log
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
import kotlinx.coroutines.launch

class NotificationExtension : INotificationServiceExtension {

    // Hilt এর মাধ্যমে FcmMessageProcessor ইনজেক্ট করার জন্য EntryPoint
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationExtensionEntryPoint {
        fun getMessageProcessor(): FcmMessageProcessor
    }

    override fun onNotificationReceived(event: INotificationReceivedEvent) {
        // ১. OneSignal এর ডিফল্ট সাধারণ নোটিফিকেশন দেখানো বন্ধ করুন
        event.preventDefault()

        val additionalData = event.notification.additionalData

        if (additionalData != null && additionalData.has("chat_id")) {
            Log.d("OneSignalExtension", "Intercepted OneSignal push, passing to custom processor")
            
            val context = KinChatApplication.instance

            // ২. Hilt থেকে আপনার কাস্টম FcmMessageProcessor নিয়ে আসা
            val entryPoint = EntryPointAccessors.fromApplication(
                context,
                NotificationExtensionEntryPoint::class.java
            )
            val processor = entryPoint.getMessageProcessor()

            // ৩. JSON ডাটাকে আপনার FcmMessagePayload-এ কনভার্ট করা
            val payloadMap = mutableMapOf<String, String>()
            val keys = additionalData.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                payloadMap[key] = additionalData.optString(key)
            }
            val payload = FcmMessagePayload.from(payloadMap)

            // ৪. আপনার কাস্টম UI (MessagingStyle) ট্রিগার করা
            if (payload != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    processor.processAndNotify(payload)
                }
            }
        }
    }
}
