package com.tuktak.app.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OtpRequestDto(
    @Json(name = "phone") val phone: String,
    @Json(name = "email") val email: String? = null
)

@JsonClass(generateAdapter = true)
data class OtpResponseDto(
    @Json(name = "status") val status: String,
    @Json(name = "isNewUser") val isNewUser: Boolean? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class VerifyOtpRequestDto(
    @Json(name = "phone") val phone: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "otp") val otp: String,
    @Json(name = "isNewUser") val isNewUser: Boolean
)

@JsonClass(generateAdapter = true)
data class VerifyOtpResponseDto(
    @Json(name = "session") val session: SessionDto?,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class SessionDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String
)
