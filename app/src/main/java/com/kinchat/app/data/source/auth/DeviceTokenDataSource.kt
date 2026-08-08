package com.kinchat.app.data.source.auth

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
            supabase.from("user_devices").delete {
                filter { eq("user_id", userId) }
            }
        }
    }

    override suspend fun saveDeviceToken(userId: String, token: String) {
        withContext(Dispatchers.IO) {
            val deviceData = DeviceTokenDto(
                userId = userId,
                deviceToken = token
            )
            supabase.from("user_devices").insert(deviceData)
        }
    }
}
