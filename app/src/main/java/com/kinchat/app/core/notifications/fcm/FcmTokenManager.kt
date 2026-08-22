package com.kinchat.app.core.notifications.fcm

import android.util.Log
import com.kinchat.app.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenManager @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend fun processNewToken(token: String) {
        Log.d("FCM", "New device token generated: $token")
        try {
            // 🚀 FIX (RC1): Send the rotated token to Supabase using AuthRepository
            authRepository.updateFcmToken(token)
            Log.d("FCM", "Token successfully synced with remote server.")
        } catch (e: Exception) {
            Log.e("FCM", "Failed to sync FCM token", e)
        }
    }
}
