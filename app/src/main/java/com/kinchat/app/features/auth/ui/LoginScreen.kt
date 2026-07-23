package com.kinchat.app.features.auth.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kinchat.app.core.utils.COUNTRIES
import com.kinchat.app.features.auth.viewmodel.AuthStep
import com.kinchat.app.features.auth.viewmodel.LoginViewModel

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
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

                if (!uiState.error.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFEBEE), RoundedCornerShape(4.dp))
                            .padding(12.dp)
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = uiState.error!!,
                            color = Color(0xFFC62828),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                AnimatedContent(targetState = uiState.step, label = "Auth Steps") { step ->
                    when (step) {
                        AuthStep.PHONE -> {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column {
                                    Text(
                                        text = "Phone Number",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // 🚀 Beautiful Bottom Sheet Trigger
                                        Box(modifier = Modifier.weight(0.35f)) {
                                            OutlinedTextField(
                                                value = uiState.countryCode, // শুধু কোড দেখাবে (আপনার কথামতো)
                                                onValueChange = {},
                                                readOnly = true,
                                                singleLine = true,
                                                trailingIcon = { 
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select") 
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                )
                                            )
                                            // Invisible layer to catch clicks
                                            Spacer(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .background(Color.Transparent)
                                                    .clickable { showCountrySheet = true }
                                            )
                                        }

                                        OutlinedTextField(
                                            value = uiState.phoneNumber,
                                            onValueChange = { viewModel.updatePhoneNumber(it) },
                                            placeholder = { Text("Phone number") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                            modifier = Modifier.weight(0.65f),
                                            singleLine = true
                                        )
                                    }
                                }
                                Button(
                                    onClick = { viewModel.requestOtp() },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    enabled = !uiState.isLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                ) {
                                    if (uiState.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    else Text("Continue")
                                }
                            }
                        }
                        AuthStep.EMAIL -> {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    text = "New number detected. Please link an email to secure your account.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column {
                                    Text(
                                        text = "Email Address",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    OutlinedTextField(
                                        value = uiState.email,
                                        onValueChange = { viewModel.updateEmail(it) },
                                        placeholder = { Text("you@example.com") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                                Button(
                                    onClick = { viewModel.requestOtp() },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    enabled = !uiState.isLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                ) {
                                    if (uiState.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    else Text("Send OTP via Email")
                                }
                            }
                        }
                        AuthStep.OTP -> {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    text = "Enter the 6-digit code sent to your email.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column {
                                    Text(
                                        text = "Verification Code",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    OutlinedTextField(
                                        value = uiState.otp,
                                        onValueChange = { viewModel.updateOtp(it) },
                                        placeholder = { Text("••••••", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, letterSpacing = 8.sp)
                                    )
                                }
                                Button(
                                    onClick = { viewModel.verifyOtp() },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    enabled = !uiState.isLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                                ) {
                                    if (uiState.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    else Text("Verify & Login")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 🚀 Smooth Bottom Sheet for Countries
        if (showCountrySheet) {
            ModalBottomSheet(
                onDismissRequest = { showCountrySheet = false },
                windowInsets = WindowInsets.navigationBars
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    items(COUNTRIES) { country ->
                        ListItem(
                            headlineContent = { 
                                Text(
                                    text = "${country.flag}  ${country.name} (${country.code})",
                                    style = MaterialTheme.typography.bodyLarge
                                ) 
                            },
                            modifier = Modifier.clickable {
                                viewModel.updateCountryCode(country.code)
                                showCountrySheet = false
                            }
                        )
                    }
                }
            }
        }
    }
}
