package com.kinchat.app.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index("username", unique = true),
        Index("phone", unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val username: String?,
    val phone: String?,
    val bio: String?,
    val avatarUrl: String?,
    val isOnline: Boolean = false,
    val isVerified: Boolean = false,
    val isDeleted: Boolean = false,
    val lastSeen: Long? = null,
    val updatedAt: Long
)
