package com.kinchat.app.core.services

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenManager @Inject constructor() {

    suspend fun processNewToken(token: String) {
        Log.d("FCM", "New device token generated: $token")
        
        // Future implementation: Send token to Supabase or specific backend API
        // Device type "android" should be attached during the sync.
        Log.d("FCM", "Token generated and ready to be synced with remote server.")
    }
}
