package com.kinchat.app.domain.model

data class UserSettings(
    val notificationsEnabled: Boolean = true,
    val readReceiptsEnabled: Boolean = true,
    val theme: String = "system"
)
