package com.kinchat.app.features.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinchat.app.BuildConfig
import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.local.datastore.UserPreferencesManager
import com.kinchat.app.domain.repository.AuthRepository
import com.kinchat.app.domain.repository.RequestOtpResult
import com.kinchat.app.features.auth.domain.provider.FcmTokenProvider
import com.kinchat.app.features.auth.utils.PhoneFormatter
import com.onesignal.OneSignal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val fcmTokenProvider: FcmTokenProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateCountryCode(code: String) {
        _uiState.update { it.copy(countryCode = code, error = null) }
    }

    fun updatePhoneNumber(phone: String) {
        val cleanPhone = PhoneFormatter.cleanPhoneNumber(phone)
        _uiState.update { it.copy(phoneNumber = cleanPhone, error = null) }
        if (PhoneFormatter.isReadyForSubmission(cleanPhone) && !_uiState.value.isLoading) {
            requestOtp()
        }
    }

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun updateOtp(otp: String) {
        val cleanOtp = PhoneFormatter.cleanOtp(otp)
        if (_uiState.value.otp == cleanOtp) return
        _uiState.update { it.copy(otp = cleanOtp, error = null) }

        if (PhoneFormatter.isOtpValid(cleanOtp) && !_uiState.value.isLoading) {
            verifyOtp()
        }
    }

    fun requestOtp() {
        val state = _uiState.value
        if (state.isLoading || state.phoneNumber.isEmpty()) return

        val fullPhone = PhoneFormatter.getFullFormattedPhone(state.countryCode, state.phoneNumber)
        val emailToSend = if (state.step == AuthStep.EMAIL || state.isNewUser) state.email else null

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.requestOtp(fullPhone, emailToSend)) {
                is RequestOtpResult.EmailRequired -> _uiState.update { it.copy(step = AuthStep.EMAIL, isLoading = false) }
                is RequestOtpResult.OtpSent -> _uiState.update { it.copy(step = AuthStep.OTP, isNewUser = result.isNewUser, isLoading = false) }
                is RequestOtpResult.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
            }
        }
    }

    fun verifyOtp() {
        val state = _uiState.value
        if (state.isLoading || !PhoneFormatter.isOtpValid(state.otp)) return

        val fullPhone = PhoneFormatter.getFullFormattedPhone(state.countryCode, state.phoneNumber)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.verifyOtp(fullPhone, if (state.isNewUser) state.email else null, state.otp, state.isNewUser)
            if (result.isSuccess) {
                // 🚀 Link User to OneSignal on successful login
                repository.getCurrentUserId()?.let { userId ->
                    OneSignal.login(userId)
                    AppLogger.d("OneSignal", "✅ User logged into OneSignal Identity with ID: $userId")
                }
                
                syncFcmToken()
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Verification failed") }
            }
        }
    }

    fun quickDevLogin(userNum: Int) {
        val phone = if (userNum == 1) BuildConfig.DEV_USER1_PHONE else BuildConfig.DEV_USER2_PHONE
        val email = if (userNum == 1) BuildConfig.DEV_USER1_EMAIL else BuildConfig.DEV_USER2_EMAIL
        val otp = BuildConfig.DEV_TEST_OTP
        val uiPhone = if (phone.startsWith("0")) phone.substring(1) else phone

        _uiState.update { it.copy(countryCode = "880", phoneNumber = uiPhone, email = email, otp = otp, isLoading = true, error = null) }
        val fullPhone = "+880$uiPhone"

        viewModelScope.launch {
            when (val result = repository.requestOtp(fullPhone, email)) {
                is RequestOtpResult.OtpSent -> {
                    _uiState.update { it.copy(step = AuthStep.OTP, isNewUser = result.isNewUser) }
                    verifyOtpBypass(fullPhone, email, otp, result.isNewUser)
                }
                is RequestOtpResult.EmailRequired -> {
                    when (val emailResult = repository.requestOtp(fullPhone, email)) {
                        is RequestOtpResult.OtpSent -> {
                            _uiState.update { it.copy(step = AuthStep.OTP, isNewUser = emailResult.isNewUser) }
                            verifyOtpBypass(fullPhone, email, otp, emailResult.isNewUser)
                        }
                        is RequestOtpResult.Error -> _uiState.update { it.copy(isLoading = false, error = emailResult.message) }
                        else -> _uiState.update { it.copy(isLoading = false, error = "Unexpected error") }
                    }
                }
                is RequestOtpResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    private fun verifyOtpBypass(fullPhone: String, email: String, otp: String, isNewUser: Boolean) {
        viewModelScope.launch {
            val result = repository.verifyOtp(fullPhone, if (isNewUser) email else null, otp, isNewUser)
            if (result.isSuccess) {
                // 🚀 Link User to OneSignal on successful Quick login
                repository.getCurrentUserId()?.let { userId ->
                    OneSignal.login(userId)
                    AppLogger.d("OneSignal", "✅ User logged into OneSignal Identity with ID: $userId")
                }
                
                syncFcmToken()
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Quick verification failed") }
            }
        }
    }

    private suspend fun syncFcmToken() {
        AppLogger.d("FCM_SYNC", "--- Starting Token Sync Process ---")
        try {
            val fcmToken = fcmTokenProvider.getToken()
            if (fcmToken != null) {
                AppLogger.d("FCM_SYNC", "Fetched Token: $fcmToken")
                val result = repository.updateFcmToken(fcmToken)

                if (result.isSuccess) {
                    AppLogger.d("FCM_SYNC", "✅ Token successfully saved via Repository")
                } else {
                    AppLogger.e("FCM_SYNC", "❌ Repository failed to save token: ${result.exceptionOrNull()?.message}")
                }
            } else {
                AppLogger.e("FCM_SYNC", "❌ Token is NULL! OneSignal failed to generate subscription ID.")
            }
        } catch (e: Exception) {
            AppLogger.e("FCM_SYNC", "❌ Exception inside syncFcmToken: ${e.message}", e)
        }
    }
}
