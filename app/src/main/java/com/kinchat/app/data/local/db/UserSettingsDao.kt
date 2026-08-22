package com.kinchat.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    suspend fun getUserSettings(userId: String): UserSettingsEntity?

    // 🚀 লাইভ আপডেটের জন্য Flow ফাংশন যোগ করা হলো
    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    fun getUserSettingsFlow(userId: String): Flow<UserSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: UserSettingsEntity)
}
