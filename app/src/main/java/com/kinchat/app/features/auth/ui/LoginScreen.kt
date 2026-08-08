package com.kinchat.app.features.auth.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kinchat.app.features.auth.ui.components.AuthErrorBanner
import com.kinchat.app.features.auth.ui.components.CountrySelectionBottomSheet
import com.kinchat.app.features.auth.ui.components.EmailInputStep
import com.kinchat.app.features.auth.ui.components.OtpInputStep
import com.kinchat.app.features.auth.ui.components.PhoneInputStep
import com.kinchat.app.features.auth.viewmodel.AuthStep
import com.kinchat.app.features.auth.viewmodel.LoginViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCountrySheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "KinChat Secure Login",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                AuthErrorBanner(error = uiState.error.orEmpty())

                AnimatedContent(targetState = uiState.step, label = "Auth Steps") { step ->
                    when (step) {
                        AuthStep.PHONE -> {
                            PhoneInputStep(
                                countryCode = uiState.countryCode,
                                phoneNumber = uiState.phoneNumber,
                                isLoading = uiState.isLoading,
                                onPhoneNumberChange = viewModel::updatePhoneNumber,
                                onRequestOtp = viewModel::requestOtp,
                                onCountrySelectorClick = { showCountrySheet = true }
                            )
                        }
                        AuthStep.EMAIL -> {
                            EmailInputStep(
                                email = uiState.email,
                                isLoading = uiState.isLoading,
                                onEmailChange = viewModel::updateEmail,
                                onRequestOtp = viewModel::requestOtp
                            )
                        }
                        AuthStep.OTP -> {
                            OtpInputStep(
                                otp = uiState.otp,
                                isLoading = uiState.isLoading,
                                onOtpChange = viewModel::updateOtp,
                                onVerifyOtp = viewModel::verifyOtp
                            )
                        }
                    }
                }
            }
        }

        if (showCountrySheet) {
            CountrySelectionBottomSheet(
                onDismissRequest = { showCountrySheet = false },
                onCountrySelected = { code ->
                    viewModel.updateCountryCode(code)
                    showCountrySheet = false
                }
            )
        }
    }
}
