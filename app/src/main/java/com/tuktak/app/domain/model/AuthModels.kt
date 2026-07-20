package com.tuktak.app.domain.model

sealed class AuthStatus {
    object EmailRequired : AuthStatus()
    data class OtpSent(val isNewUser: Boolean) : AuthStatus()
}

enum class AuthStep {
    PHONE, EMAIL, OTP
}

data class CountryCode(
    val code: String,
    val iso: String,
    val flag: String
)

val SupportedCountries = listOf(
    CountryCode("+880", "BD", "🇧🇩"),
    CountryCode("+1", "US", "🇺🇸"),
    CountryCode("+44", "UK", "🇬🇧"),
    CountryCode("+91", "IN", "🇮🇳")
)
