package com.kinchat.app.features.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kinchat.app.features.settings.ui.components.*
import com.kinchat.app.features.settings.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBlocked: () -> Unit,
    onNavigateToDevices: () -> Unit,
    onNavigateToFeedback: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dropdown state for theme selection
    var showThemeDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onNavigateToLogin()
        }
    }

    LaunchedEffect(uiState.errorMsg) {
        uiState.errorMsg?.let { error ->
            snackbarHostState.showSnackbar(message = error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading && uiState.settings.theme == "system") {
            // Assume initial load
            Box(modifier = Modifier.padding(paddingValues)) {
                SettingsSkeleton()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                // Appearance & Behavior Section
                item {
                    SettingsSectionTitle("Appearance & Behavior")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        
                        // Theme Selector
                        Box {
                            SettingsNavigationItem(
                                icon = Icons.Default.Palette,
                                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                title = "Theme",
                                trailingText = uiState.settings.theme.replaceFirstChar { it.uppercase() },
                                onClick = { showThemeDropdown = true }
                            )
                            
                            DropdownMenu(
                                expanded = showThemeDropdown,
                                onDismissRequest = { showThemeDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Light") },
                                    onClick = { 
                                        viewModel.updateTheme("light")
                                        showThemeDropdown = false 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Dark") },
                                    onClick = { 
                                        viewModel.updateTheme("dark")
                                        showThemeDropdown = false 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("System Default") },
                                    onClick = { 
                                        viewModel.updateTheme("system")
                                        showThemeDropdown = false 
                                    }
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)

                        // Push Notifications
                        SettingsSwitchItem(
                            icon = Icons.Default.Notifications,
                            iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            title = "Push Notifications",
                            isChecked = uiState.settings.notificationsEnabled,
                            onCheckedChange = { viewModel.toggleNotifications(it) }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)

                        // Read Receipts
                        SettingsSwitchItem(
                            icon = Icons.Default.DoneAll,
                            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            title = "Read Receipts",
                            subtitle = "Let others know you've read their messages",
                            isChecked = uiState.settings.readReceiptsEnabled,
                            onCheckedChange = { viewModel.toggleReadReceipts(it) }
                        )
                    }
                }

                // Privacy & Security Section
                item {
                    SettingsSectionTitle("Privacy & Security")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        SettingsNavigationItem(
                            icon = Icons.Default.Block,
                            iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                            iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
                            title = "Blocked Users",
                            onClick = onNavigateToBlocked
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                        SettingsNavigationItem(
                            icon = Icons.Default.Devices,
                            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            title = "Connected Devices",
                            onClick = onNavigateToDevices
                        )
                    }
                }

                // Support & Info Section
                item {
                    SettingsSectionTitle("Support & Info")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        SettingsNavigationItem(
                            icon = Icons.Default.Feedback,
                            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            title = "Submit Feedback",
                            onClick = onNavigateToFeedback
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                        SettingsNavigationItem(
                            icon = Icons.Default.Policy,
                            iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            title = "Privacy Policy",
                            onClick = onNavigateToPrivacy
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                        SettingsNavigationItem(
                            icon = Icons.Default.Info,
                            iconContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            iconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            title = "About KinChat",
                            trailingText = "v1.0.0",
                            onClick = onNavigateToAbout
                        )
                    }
                }

                // Account Actions Section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        SettingsActionItem(
                            icon = Icons.Default.Logout,
                            title = "Log Out",
                            contentColor = MaterialTheme.colorScheme.error,
                            onClick = { viewModel.logout() }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                        SettingsActionItem(
                            icon = Icons.Default.Delete,
                            title = "Delete Account",
                            contentColor = MaterialTheme.colorScheme.outline,
                            onClick = { viewModel.deleteAccount() }
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
