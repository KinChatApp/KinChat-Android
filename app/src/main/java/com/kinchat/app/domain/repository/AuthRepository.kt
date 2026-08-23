package com.kinchat.app.domain.repository

import kotlinx.coroutines.flow.Flow

sealed class RequestOtpResult {
    object EmailRequired : RequestOtpResult()
    data class OtpSent(val isNewUser: Boolean) : RequestOtpResult()
    data class Error(val message: String) : RequestOtpResult()
}

sealed class AppAuthState {
    data class Authenticated(val userId: String, val userName: String) : AppAuthState()
    object Unauthenticated : AppAuthState()
    object Unknown : AppAuthState()
}

interface AuthRepository {
    suspend fun requestOtp(phone: String, email: String? = null): RequestOtpResult
    suspend fun verifyOtp(phone: String, email: String?, otp: String, isNewUser: Boolean): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun isUserLoggedIn(): Boolean
    suspend fun getCurrentUserId(): String?
    suspend fun updateFcmToken(token: String): Result<Unit>
    
    // 🚀 NEW: UI-কে Supabase থেকে আলাদা করার জন্য
    fun observeAuthState(): Flow<AppAuthState>
}
