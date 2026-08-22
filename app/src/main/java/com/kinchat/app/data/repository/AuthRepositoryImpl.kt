package com.kinchat.app.data.repository

import com.kinchat.app.data.local.datastore.AuthPreferencesManager
import com.kinchat.app.data.remote.api.AuthApi
import com.kinchat.app.data.remote.model.OtpRequestDto
import com.kinchat.app.data.remote.model.VerifyOtpRequestDto
import com.kinchat.app.data.source.auth.DeviceTokenDataSource
import com.kinchat.app.data.source.auth.SupabaseAuthDataSource
import com.kinchat.app.domain.repository.AuthRepository
import com.kinchat.app.domain.repository.RequestOtpResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val supabaseAuthDataSource: SupabaseAuthDataSource,
    private val deviceTokenDataSource: DeviceTokenDataSource,
    private val authPreferencesManager: AuthPreferencesManager
) : AuthRepository {

    override suspend fun requestOtp(phone: String, email: String?): RequestOtpResult {
        return withContext(Dispatchers.IO) {
            try {
                val response = authApi.requestOtp(OtpRequestDto(phone, email))
                val body = response.body()

                if (response.isSuccessful && body != null) {
                    when (body.status) {
                        "email_required" -> RequestOtpResult.EmailRequired
                        "otp_sent" -> RequestOtpResult.OtpSent(body.isNewUser ?: false)
                        else -> RequestOtpResult.Error(body.error ?: "Unknown error")
                    }
                } else {
                    RequestOtpResult.Error("Server error: ${response.code()}")
                }
            } catch (e: Exception) {
                RequestOtpResult.Error(e.message ?: "Failed to connect to server")
            }
        }
    }

    override suspend fun verifyOtp(
        phone: String,
        email: String?,
        otp: String,
        isNewUser: Boolean
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = authApi.verifyOtp(VerifyOtpRequestDto(phone, email, otp, isNewUser))
                val body = response.body()

                if (response.isSuccessful && body?.session != null) {
                    supabaseAuthDataSource.importAuthToken(
                        accessToken = body.session.accessToken,
                        refreshToken = body.session.refreshToken
                    )

                    // 🚀 FIX: Persist meId after successful auth
                    val userId = supabaseAuthDataSource.getCurrentUserId()
                    if (userId != null) {
                        authPreferencesManager.setMeId(userId)
                    }

                    Result.success(Unit)
                } else {
                    Result.failure(Exception(body?.error ?: "Invalid OTP"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun logout(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 🚀 FIX (RC2): Removed deviceTokenDataSource.clearDeviceTokens(userId)
                // Wiping out tokens for all devices is now prevented.
                
                supabaseAuthDataSource.signOut()

                // 🚀 FIX: Clear meId on logout to disable notifications for logged out user
                authPreferencesManager.setMeId("")

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun isUserLoggedIn(): Boolean {
        return supabaseAuthDataSource.isUserLoggedIn()
    }

    override suspend fun updateFcmToken(token: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                supabaseAuthDataSource.getCurrentUserId()?.let { userId ->
                    // 🚀 FIX (RC2): Removed clearDeviceTokens(userId) to prevent deleting other devices' tokens.
                    // Now it only saves/upserts the current token.
                    deviceTokenDataSource.saveDeviceToken(userId, token)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
