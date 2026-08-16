package com.kinchat.app.data.local.db

import androidx.room.Entity

@Entity(tableName = "user_blocks", primaryKeys = ["blockerId", "blockedId"])
data class UserBlockEntity(
    val blockerId: String,
    val blockedId: String
)
