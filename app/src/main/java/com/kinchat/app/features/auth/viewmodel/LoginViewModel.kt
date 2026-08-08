package com.kinchat.app.features.auth.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinchat.app.data.local.datastore.UserPreferencesManager
import com.kinchat.app.domain.repository.AuthRepository
import com.kinchat.app.domain.repository.RequestOtpResult
import com.kinchat.app.features.auth.domain.provider.FcmTokenProvider
import com.kinchat.app.features.auth.utils.PhoneFormatter
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
                is RequestOtpResult.EmailRequired -> {
                    _uiState.update { it.copy(step = AuthStep.EMAIL, isLoading = false) }
                }
                is RequestOtpResult.OtpSent -> {
                    _uiState.update { it.copy(step = AuthStep.OTP, isNewUser = result.isNewUser, isLoading = false) }
                }
                is RequestOtpResult.Error -> {
                    _uiState.update { it.copy(error = result.message, isLoading = false) }
                }
            }
        }
    }

    fun verifyOtp() {
        val state = _uiState.value
        if (state.isLoading || !PhoneFormatter.isOtpValid(state.otp)) return

        val fullPhone = PhoneFormatter.getFullFormattedPhone(state.countryCode, state.phoneNumber)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.verifyOtp(
                phone = fullPhone,
                email = if (state.isNewUser) state.email else null,
                otp = state.otp,
                isNewUser = state.isNewUser
            )

            if (result.isSuccess) {
                syncFcmToken()
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Verification failed"
                _uiState.update { it.copy(isLoading = false, error = errorMsg) }
            }
        }
    }

    private fun syncFcmToken() {
        viewModelScope.launch {
            try {
                val fcmToken = fcmTokenProvider.getToken()
                if (fcmToken != null) {
                    repository.updateFcmToken(fcmToken)
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Exception while saving FCM token", e)
            }
        }
    }
}
