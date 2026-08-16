package com.kinchat.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val userId: String,
    val notificationsEnabled: Boolean = true,
    val readReceiptsEnabled: Boolean = true,
    val theme: String = "system"
)
