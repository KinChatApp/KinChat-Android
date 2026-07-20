package com.tuktak.app.domain.usecase

import com.tuktak.app.domain.repository.AuthRepository
import com.tuktak.app.domain.repository.RequestOtpResult
import javax.inject.Inject

class RequestOtpUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(phone: String, email: String? = null): RequestOtpResult {
        return repository.requestOtp(phone, email)
    }
}

class VerifyOtpUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(phone: String, email: String?, otp: String, isNewUser: Boolean): Result<Unit> {
        return repository.verifyOtp(phone, email, otp, isNewUser)
    }
}

class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.logout()
    }
}

data class AuthUseCases @Inject constructor(
    val requestOtp: RequestOtpUseCase,
    val verifyOtp: VerifyOtpUseCase,
    val logout: LogoutUseCase
)
