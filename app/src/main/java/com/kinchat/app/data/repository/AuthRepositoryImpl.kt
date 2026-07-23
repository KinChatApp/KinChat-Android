// app/src/main/java/com/kinchat/app/data/repository/AuthRepositoryImpl.kt

package com.kinchat.app.data.repository

import com.kinchat.app.data.remote.api.AuthApi
import com.kinchat.app.data.remote.model.OtpRequestDto
import com.kinchat.app.data.remote.model.VerifyOtpRequestDto
import com.kinchat.app.domain.repository.AuthRepository
import com.kinchat.app.domain.repository.RequestOtpResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val supabase: SupabaseClient
) : AuthRepository {

    override suspend fun requestOtp(phone: String, email: String?): RequestOtpResult {
        // 🚀 API কল ব্যাকগ্রাউন্ড IO থ্রেডে পাঠানো হলো
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
                e.printStackTrace()
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
        // 🚀 API কল ব্যাকগ্রাউন্ড IO থ্রেডে পাঠানো হলো
        return withContext(Dispatchers.IO) {
            try {
                val response = authApi.verifyOtp(VerifyOtpRequestDto(phone, email, otp, isNewUser))
                val body = response.body()

                if (response.isSuccessful && body?.session != null) {
                    supabase.auth.importAuthToken(
                        accessToken = body.session.accessToken,
                        refreshToken = body.session.refreshToken
                    )
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(body?.error ?: "Invalid OTP"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    override suspend fun logout(): Result<Unit> {
        // 🚀 API কল ব্যাকগ্রাউন্ড IO থ্রেডে পাঠানো হলো
        return withContext(Dispatchers.IO) {
            try {
                supabase.auth.signOut()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun isUserLoggedIn(): Boolean {
        // এটি সিঙ্ক্রোনাস কাজ, তাই Dispatchers.IO প্রয়োজন নেই
        return supabase.auth.currentSessionOrNull() != null
    }
}
