package com.kinchat.app.data.repository

import android.util.Log
import com.kinchat.app.data.remote.model.UserSettingsDto
import com.kinchat.app.domain.model.UserSettings
import com.kinchat.app.domain.repository.SettingsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : SettingsRepository {

    private val tableName = "user_settings"

    override suspend fun getUserSettings(): Result<UserSettings> = withContext(Dispatchers.IO) {
        try {
            val user = supabaseClient.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val dtoList = supabaseClient.postgrest[tableName]
                .select { filter { eq("user_id", user.id) } }
                .decodeList<UserSettingsDto>()

            val dto = dtoList.firstOrNull()

            if (dto != null) {
                Result.success(
                    UserSettings(
                        notificationsEnabled = dto.notificationsEnabled ?: true,
                        readReceiptsEnabled = dto.readReceiptsEnabled ?: true,
                        theme = dto.theme ?: "system"
                    )
                )
            } else {
                // Return default settings if none found in DB
                Result.success(UserSettings())
            }
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Error fetching settings", e)
            Result.failure(e)
        }
    }

    override suspend fun updateSetting(key: String, value: Any): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = supabaseClient.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val jsonPayload = buildJsonObject {
                put("user_id", user.id)
                put("updated_at", Instant.now().toString())
                when (value) {
                    is Boolean -> put(key, value)
                    is String -> put(key, value)
                    is Number -> put(key, value)
                    else -> throw IllegalArgumentException("Unsupported data type for settings")
                }
            }

            supabaseClient.postgrest[tableName].upsert(jsonPayload)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Error updating setting $key", e)
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Clears Supabase local session
            supabaseClient.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Error during logout", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        // Typically requires Admin API. For now, returning a generic feature-not-ready error.
        Result.failure(Exception("Account deletion requires admin privileges or edge function."))
    }
}
