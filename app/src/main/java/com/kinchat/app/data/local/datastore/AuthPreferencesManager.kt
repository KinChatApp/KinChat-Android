package com.kinchat.app.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android equivalent of authStorage.ts
 */
@Singleton
class AuthPreferencesManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val meIdKey = stringPreferencesKey("kinchat_me_id")

    val meId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[meIdKey]
    }

    suspend fun setMeId(id: String) {
        dataStore.edit { preferences ->
            preferences[meIdKey] = id
        }
    }

    suspend fun removeMeId() {
        dataStore.edit { preferences ->
            preferences.remove(meIdKey)
        }
    }
}
