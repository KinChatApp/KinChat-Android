package com.kinchat.app.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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

    // 🚀 In-memory StateFlow Cache
    private val _meId = MutableStateFlow<String?>(null)
    val meId: StateFlow<String?> = _meId.asStateFlow()

    init {
        // Singleton স্কোপ হওয়ায় এটি অ্যাপের লাইফসাইকেল পর্যন্ত বেঁচে থাকবে
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            dataStore.data
                .map { preferences -> preferences[meIdKey] }
                .collect { id ->
                    _meId.value = id
                }
        }
    }

    suspend fun setMeId(id: String) {
        _meId.value = id
        dataStore.edit { preferences ->
            preferences[meIdKey] = id
        }
    }

    suspend fun removeMeId() {
        _meId.value = null
        dataStore.edit { preferences ->
            preferences.remove(meIdKey)
        }
    }
}
