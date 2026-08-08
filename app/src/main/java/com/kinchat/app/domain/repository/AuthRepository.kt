package com.kinchat.app.domain.repository

sealed class RequestOtpResult {
    object EmailRequired : RequestOtpResult()
    data class OtpSent(val isNewUser: Boolean) : RequestOtpResult()
    data class Error(val message: String) : RequestOtpResult()
}

interface AuthRepository {
    suspend fun requestOtp(phone: String, email: String? = null): RequestOtpResult
    suspend fun verifyOtp(phone: String, email: String?, otp: String, isNewUser: Boolean): Result<Unit>
    suspend fun logout(): Result<Unit>
    
    // 🚀 Updated: Made it a suspend function for offline support
    suspend fun isUserLoggedIn(): Boolean

    // 🚀 FCM Token আপডেট করার মেথড
    suspend fun updateFcmToken(token: String): Result<Unit>
}
