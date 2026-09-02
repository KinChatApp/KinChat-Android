package com.kinchat.app.data.repository

import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.local.datastore.AuthPreferencesManager
import com.kinchat.app.data.remote.api.AuthApi
import com.kinchat.app.data.remote.model.OtpRequestDto
import com.kinchat.app.data.remote.model.VerifyOtpRequestDto
import com.kinchat.app.data.source.auth.DeviceTokenDataSource
import com.kinchat.app.data.source.auth.SupabaseAuthDataSource
import com.kinchat.app.domain.repository.AppAuthState
import com.kinchat.app.domain.repository.AuthRepository
import com.kinchat.app.domain.repository.RequestOtpResult
import com.onesignal.OneSignal
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    override suspend fun verifyOtp(phone: String, email: String?, otp: String, isNewUser: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = authApi.verifyOtp(VerifyOtpRequestDto(phone, email, otp, isNewUser))
                val body = response.body()

                if (response.isSuccessful && body?.session != null) {
                    supabaseAuthDataSource.importAuthToken(
                        accessToken = body.session.accessToken,
                        refreshToken = body.session.refreshToken
                    )

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
                OneSignal.logout()
                supabaseAuthDataSource.signOut()
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

    override suspend fun getCurrentUserId(): String? {
        return try {
            if (!supabaseAuthDataSource.isUserLoggedIn()) {
                null
            } else {
                supabaseAuthDataSource.getCurrentUserId()
            }
        } catch (e: Exception) {
            AppLogger.e(
                "AuthRepository",
                "Failed to initialize/read current user",
                e
            )
            null
        }
    }

    override suspend fun updateFcmToken(token: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId()
                    ?: return@withContext Result.failure(
                        IllegalStateException("Authenticated user unavailable")
                    )

                deviceTokenDataSource.saveDeviceToken(userId, token)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun observeAuthState(): Flow<AppAuthState> {
        return supabaseAuthDataSource.observeSessionStatus().map { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    val user = status.session.user
                    val currentUserId = user?.id?.replace("-", "")?.trim() ?: ""
                    val currentUserName = user?.phone ?: user?.email ?: "KinChat User"

                    // 🚀 FIX 1: Ensure meId is written to DataStore on cold starts
                    if (currentUserId.isNotBlank()) {
                        authPreferencesManager.setMeId(currentUserId)
                    }

                    AppAuthState.Authenticated(currentUserId, currentUserName)
                }
                is SessionStatus.NotAuthenticated -> AppAuthState.Unauthenticated
                else -> AppAuthState.Unknown
            }
        }
    }
}
