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
            } catch (e: Exception) {
                AppLogger.e("FCM_SYNC", "Error clearing tokens: ${e.message}", e)
            }
        }
    }

    override suspend fun saveDeviceToken(userId: String, token: String) {
        withContext(Dispatchers.IO) {
            try {
                // সরাসরি ভ্যালুগুলো দিয়ে দেওয়া হলো, যাতে JSON-এ মিসিং না হয়
                val deviceData = DeviceTokenDto(
                    userId = userId,
                    deviceToken = token,
                    deviceType = "android", 
                    isActive = true
                )
                AppLogger.d("FCM_SYNC", "Attempting to save token to Supabase for user: $userId")
                supabase.from("user_devices").upsert(deviceData)
                AppLogger.d("FCM_SYNC", "✅ Successfully saved token to Supabase!")
            } catch (e: Exception) {
                AppLogger.e("FCM_SYNC", "❌ Supabase Insert/Upsert Failed. Reason: ${e.message}", e)
                throw e
            }
        }
    }
}
