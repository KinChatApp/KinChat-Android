package com.kinchat.app.data.source.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceTokenDto(
    @SerialName("user_id") val userId: String,
    @SerialName("device_token") val deviceToken: String,
    @SerialName("device_type") val deviceType: String,
    @SerialName("is_active") val isActive: Boolean
)
