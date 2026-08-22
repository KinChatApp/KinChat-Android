package com.kinchat.app.domain.repository

import com.kinchat.app.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    suspend fun getUserSettings(): Result<UserSettings>
    
    // 🚀 লাইভ ডাটা স্ট্রিমের জন্য নতুন ফাংশন
    fun getUserSettingsFlow(): Flow<UserSettings>
    
    suspend fun updateSetting(key: String, value: Any): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
}
