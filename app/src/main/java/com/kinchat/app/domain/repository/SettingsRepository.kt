package com.kinchat.app.domain.repository

import com.kinchat.app.domain.model.UserSettings

interface SettingsRepository {
    suspend fun getUserSettings(): Result<UserSettings>
    suspend fun updateSetting(key: String, value: Any): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
}
