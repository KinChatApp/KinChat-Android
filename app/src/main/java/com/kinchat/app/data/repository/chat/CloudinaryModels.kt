package com.kinchat.app.data.repository.chat

import kotlinx.serialization.Serializable

@Serializable
data class CloudinaryAuthPayload(
    val folder: String
)

@Serializable
data class CloudinaryAuthResponse(
    val signature: String,
    val timestamp: Long,
    val apiKey: String
)
