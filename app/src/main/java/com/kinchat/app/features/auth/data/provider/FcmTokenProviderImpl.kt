package com.kinchat.app.features.auth.data.provider

import android.util.Log
import com.kinchat.app.features.auth.domain.provider.FcmTokenProvider
import com.onesignal.OneSignal
import javax.inject.Inject

class FcmTokenProviderImpl @Inject constructor() : FcmTokenProvider {

    override suspend fun getToken(): String? {
        val subscriptionId = OneSignal.User.pushSubscription.id

        return if (!subscriptionId.isNullOrEmpty() && !subscriptionId.startsWith("local-")) {
            Log.d("TokenProvider", "✅ Found OneSignal Subscription ID: $subscriptionId")
            subscriptionId
        } else {
            Log.w("TokenProvider", "⚠️ OneSignal Subscription ID is not ready yet.")
            null
        }
    }
}
