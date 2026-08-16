package com.kinchat.app.data.repository

import android.util.Log
import com.kinchat.app.data.local.db.UserSettingsDao
import com.kinchat.app.data.local.db.UserSettingsEntity
import com.kinchat.app.domain.model.UserSettings
import com.kinchat.app.domain.repository.SettingsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val userSettingsDao: UserSettingsDao
) : SettingsRepository {

    override suspend fun getUserSettings(): Result<UserSettings> = withContext(Dispatchers.IO) {
        try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val entity = userSettingsDao.getUserSettings(userId)
            
            if (entity != null) {
                Result.success(
                    UserSettings(
                        notificationsEnabled = entity.notificationsEnabled,
                        readReceiptsEnabled = entity.readReceiptsEnabled,
                        theme = entity.theme
                    )
                )
            } else {
                Result.success(UserSettings())
            }
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Error fetching settings from Room", e)
            Result.failure(e)
        }
    }

    override suspend fun updateSetting(key: String, value: Any): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val currentSettings = userSettingsDao.getUserSettings(userId) ?: UserSettingsEntity(userId = userId)
            
            val updatedSettings = when (key) {
                "notificationsEnabled" -> currentSettings.copy(notificationsEnabled = value as Boolean)
                "readReceiptsEnabled" -> currentSettings.copy(readReceiptsEnabled = value as Boolean)
                "theme" -> currentSettings.copy(theme = value as String)
                else -> currentSettings
            }
            
            userSettingsDao.insertOrUpdate(updatedSettings)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Error updating setting $key", e)
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabaseClient.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        Result.failure(Exception("Account deletion requires admin privileges or edge function."))
    }
}
