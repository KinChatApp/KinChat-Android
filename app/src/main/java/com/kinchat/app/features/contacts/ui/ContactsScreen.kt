package com.kinchat.app.features.contacts.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kinchat.app.features.contacts.ui.components.*
import com.kinchat.app.features.contacts.ui.utils.ContactIntentUtils
import com.kinchat.app.features.contacts.viewmodel.ContactsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onNavigateToChat: (String) -> Unit,
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val resolvedChatId by viewModel.resolvedChatId.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.syncContacts()
        } else {
            Toast.makeText(context, "Contact permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    LaunchedEffect(resolvedChatId) {
        resolvedChatId?.let { chatId ->
            onNavigateToChat(chatId)
            viewModel.onChatNavigated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts", fontWeight = FontWeight.Bold) },
                actions = {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.registeredContacts.isEmpty() && uiState.unregisteredContacts.isEmpty() -> {
                    ContactsSkeleton()
                }
                !uiState.isLoading && uiState.registeredContacts.isEmpty() && uiState.unregisteredContacts.isEmpty() -> {
                    EmptyContactsView()
                }
                else -> {
                    ContactsListContent(
                        uiState = uiState,
                        onOpenChat = { userId -> viewModel.openChatWithUser(userId) },
                        onInvite = { phone -> ContactIntentUtils.sendSmsInvite(context, phone) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactsListContent(
    uiState: com.kinchat.app.features.contacts.viewmodel.ContactsUiState,
    onOpenChat: (String) -> Unit,
    onInvite: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        uiState.errorMsg?.let { error ->
            item {
                ErrorBanner(message = error)
            }
        }

        if (uiState.registeredContacts.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "REGISTERED ON TUKTAK (${uiState.registeredContacts.size})",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(
                items = uiState.registeredContacts,
                key = { it.id ?: it.contactPhoneNormalized.ifEmpty { it.hashCode().toString() } }
            ) { contact ->
                RegisteredContactItem(
                    contact = contact,
                    onClick = {
                        contact.registeredUserId?.let { onOpenChat(it) }
                    }
                )
            }
        }

        if (uiState.unregisteredContacts.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "INVITE TO TUKTAK (${uiState.unregisteredContacts.size})",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(
                items = uiState.unregisteredContacts,
                key = { it.id ?: it.contactPhone.ifEmpty { it.hashCode().toString() } }
            ) { contact ->
                UnregisteredContactItem(
                    contact = contact,
                    onInvite = { onInvite(contact.contactPhone) }
                )
            }
        }
    }
}
