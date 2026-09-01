package com.kinchat.app.data.source.auth

import com.kinchat.app.core.logging.AppLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface DeviceTokenDataSource {
    suspend fun clearDeviceTokens(userId: String)
    suspend fun saveDeviceToken(userId: String, token: String)
}

class DeviceTokenDataSourceImpl @Inject constructor(
    private val supabase: SupabaseClient
) : DeviceTokenDataSource {

    override suspend fun clearDeviceTokens(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                supabase.from("user_devices").delete {
                    filter { eq("user_id", userId) }
                }
                AppLogger.d("FCM_SYNC", "✅ Cleared device tokens for user: $userId")
            } catch (e: Exception) {
                AppLogger.e("FCM_SYNC", "Error clearing tokens: ${e.message}", e)
            }
        }
    }

    override suspend fun saveDeviceToken(userId: String, token: String) {
        withContext(Dispatchers.IO) {
            try {
                // 🚀 FIX: Delete ALL existing tokens for this user to strictly prevent duplicate notifications.
                // (Since we don't have a unique device_id in the schema yet)
                supabase.from("user_devices").delete {
                    filter { eq("user_id", userId) }
                }

                val deviceData = DeviceTokenDto(
                    userId = userId,
                    deviceToken = token,
                    deviceType = "onesignal_android",
                    isActive = true
                )
                supabase.from("user_devices").upsert(deviceData)
                AppLogger.d("FCM_SYNC", "✅ Saved new token and cleared old ones to prevent duplicate pushes.")
            } catch (e: Exception) {
                AppLogger.e("FCM_SYNC", "❌ Supabase Insert/Upsert Failed.", e)
                throw e
            }
        }
    }
}
