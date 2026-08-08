package com.kinchat.app.features.auth.viewmodel

enum class AuthStep { PHONE, EMAIL, OTP }

data class LoginUiState(
    val step: AuthStep = AuthStep.PHONE,
    val countryCode: String = "+880",
    val phoneNumber: String = "",
    val email: String = "",
    val otp: String = "",
    val isNewUser: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)
