package com.tuktak.app.data.remote.api

import com.tuktak.app.data.remote.model.OtpRequestDto
import com.tuktak.app.data.remote.model.OtpResponseDto
import com.tuktak.app.data.remote.model.VerifyOtpRequestDto
import com.tuktak.app.data.remote.model.VerifyOtpResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    // Supabase Edge Function-এর ডিফল্ট পাথ: /functions/v1/{function-name}
    @POST("functions/v1/request-otp")
    suspend fun requestOtp(@Body request: OtpRequestDto): Response<OtpResponseDto>

    @POST("functions/v1/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequestDto): Response<VerifyOtpResponseDto>
}
