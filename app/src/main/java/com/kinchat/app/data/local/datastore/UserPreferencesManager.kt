package com.kinchat.app.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android equivalent of userStorage.ts
 * Uses DataStore instead of synchronous localStorage for better performance and thread safety.
 */
@Singleton
class UserPreferencesManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val contactCacheKey = stringPreferencesKey("kinchat_contact_cache")
    private val avatarCacheKey = stringPreferencesKey("kinchat_my_avatar")

    // Get a single contact name by ID as a Flow
    fun getContactName(userId: String): Flow<String?> {
        return dataStore.data.map { preferences ->
            val jsonString = preferences[contactCacheKey] ?: "{}"
            val cache = try {
                Json.decodeFromString<Map<String, String>>(jsonString)
            } catch (e: Exception) {
                emptyMap()
            }
            cache[userId]
        }
    }

    // Set a single contact name by ID
    suspend fun setContactName(userId: String, name: String) {
        dataStore.edit { preferences ->
            val jsonString = preferences[contactCacheKey] ?: "{}"
            val cache = try {
                Json.decodeFromString<MutableMap<String, String>>(jsonString)
            } catch (e: Exception) {
                mutableMapOf()
            }
            cache[userId] = name
            preferences[contactCacheKey] = Json.encodeToString(cache)
        }
    }

    // Get the entire contact cache object
    fun getAllContacts(): Flow<Map<String, String>> {
        return dataStore.data.map { preferences ->
            val jsonString = preferences[contactCacheKey] ?: "{}"
            try {
                Json.decodeFromString<Map<String, String>>(jsonString)
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    // Set the entire contact cache object
    suspend fun setAllContacts(contacts: Map<String, String>) {
        dataStore.edit { preferences ->
            preferences[contactCacheKey] = Json.encodeToString(contacts)
        }
    }

    // Get current user's avatar URL
    fun getMyAvatar(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[avatarCacheKey]
        }
    }

    // Set current user's avatar URL
    suspend fun setMyAvatar(avatarUrl: String) {
        dataStore.edit { preferences ->
            preferences[avatarCacheKey] = avatarUrl
        }
    }
}
