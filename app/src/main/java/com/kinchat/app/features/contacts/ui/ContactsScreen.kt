package com.kinchat.app.features.contacts.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.kinchat.app.R
import com.kinchat.app.features.contacts.ui.components.ContactsListContent
import com.kinchat.app.features.contacts.ui.components.ContactsSkeleton
import com.kinchat.app.features.contacts.ui.components.ContactsTopAppBar
import com.kinchat.app.features.contacts.ui.components.EmptyContactsView
import com.kinchat.app.features.contacts.ui.utils.ContactIntentUtils
import com.kinchat.app.features.contacts.viewmodel.ContactsViewModel

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
        viewModel.updatePermissionState(isGranted)
        if (!isGranted) {
            Toast.makeText(context, context.getString(R.string.toast_contact_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        viewModel.updatePermissionState(isGranted)
    }

    LaunchedEffect(resolvedChatId) {
        resolvedChatId?.let { chatId ->
            onNavigateToChat(chatId)
            viewModel.onChatNavigated()
        }
    }

    Scaffold(
        topBar = {
            ContactsTopAppBar(
                onSearchClick = { /* TODO: open search */ }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO */ },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.action_add)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!uiState.hasContactsPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Find your contacts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Allow Contacts access to see saved names and find friends on KinChat.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Allow Access")
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
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
}
