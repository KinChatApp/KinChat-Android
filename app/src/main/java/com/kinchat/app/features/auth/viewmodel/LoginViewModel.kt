package com.kinchat.app.features.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinchat.app.domain.repository.AuthRepository
import com.kinchat.app.domain.repository.RequestOtpResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateCountryCode(code: String) {
        _uiState.update { it.copy(countryCode = code, error = null) }
    }

    fun updatePhoneNumber(phone: String) {
        val cleanPhone = phone.replace("\\s+".toRegex(), "")
        _uiState.update { it.copy(phoneNumber = cleanPhone, error = null) }
        
        // 🚀 Auto-submit logic: 10 digits (without 0) or 11 digits (with 0)
        val expectedLength = if (cleanPhone.startsWith("0")) 11 else 10
        if (cleanPhone.length == expectedLength && !_uiState.value.isLoading) {
            requestOtp()
        }
    }

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun updateOtp(otp: String) {
        val cleanOtp = otp.take(6)
        _uiState.update { it.copy(otp = cleanOtp, error = null) }
        
        if (cleanOtp.length == 6 && !_uiState.value.isLoading) {
            verifyOtp()
        }
    }

    // 🛠️ Helper: ০ (Zero) প্রবলেম ফিক্স করার জন্য
    private fun getFullFormattedPhone(): String {
        val state = _uiState.value
        // যদি নাম্বারের শুরুতে 0 থাকে, তবে সেটি বাদ দিয়ে বাকিটুকু নেবে
        val phoneWithoutLeadingZero = if (state.phoneNumber.startsWith("0")) {
            state.phoneNumber.substring(1)
        } else {
            state.phoneNumber
        }
        return "${state.countryCode}${phoneWithoutLeadingZero}"
    }

    fun requestOtp() {
        val state = _uiState.value
        if (state.isLoading || state.phoneNumber.isEmpty()) return

        val fullPhone = getFullFormattedPhone()
        val emailToSend = if (state.step == AuthStep.EMAIL) state.email else null

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
        if (state.isLoading || state.otp.length < 6) return

        val fullPhone = getFullFormattedPhone()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = repository.verifyOtp(
                phone = fullPhone,
                email = if (state.isNewUser) state.email else null,
                otp = state.otp,
                isNewUser = state.isNewUser
            )

            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } else {
                _uiState.update { 
                    it.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Verification failed") 
                }
            }
        }
    }
}
