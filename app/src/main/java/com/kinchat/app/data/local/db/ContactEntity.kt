package com.kinchat.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val contactName: String,
    val contactPhone: String,
    val contactPhoneNormalized: String,
    val registeredUserId: String? = null,
    val profileName: String? = null,
    val username: String? = null,
    val avatarUrl: String? = null
)
