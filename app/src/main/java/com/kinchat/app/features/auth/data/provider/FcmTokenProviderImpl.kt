package com.kinchat.app.features.auth.data.provider

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.kinchat.app.features.auth.domain.provider.FcmTokenProvider
import com.onesignal.OneSignal
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class FcmTokenProviderImpl @Inject constructor() : FcmTokenProvider {

    override suspend fun getToken(): String? = suspendCancellableCoroutine { continuation ->
        // 🚀 ১. প্রথমে OneSignal-এর টোকেন নেওয়ার চেষ্টা করবে
        val osToken = OneSignal.User.pushSubscription.id
        
        if (!osToken.isNullOrEmpty() && !osToken.startsWith("local-")) {
            Log.d("TokenProvider", "✅ Found OneSignal Token: $osToken")
            continuation.resume(osToken)
            return@suspendCancellableCoroutine
        }

        // 🚀 ২. OneSignal টোকেন রেডি না থাকলে FCM টোকেন নেবে (Fallback)
        Log.d("TokenProvider", "⚠️ OneSignal token not ready, falling back to FCM.")
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("TokenProvider", "✅ Found FCM Token: ${task.result}")
                continuation.resume(task.result)
            } else {
                Log.e("TokenProvider", "❌ Failed to get FCM Token")
                continuation.resume(null)
            }
        }
    }
}
