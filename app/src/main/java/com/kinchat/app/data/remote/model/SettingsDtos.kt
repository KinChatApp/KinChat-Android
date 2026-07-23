package com.kinchat.app.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserSettingsDto(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean? = true,
    @SerialName("read_receipts_enabled") val readReceiptsEnabled: Boolean? = true,
    val theme: String? = "system",
    @SerialName("updated_at") val updatedAt: String? = null
)
