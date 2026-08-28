package com.kinchat.app.features.contacts.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
        if (isGranted) {
            viewModel.syncContacts()
        } else {
            Toast.makeText(context, context.getString(R.string.toast_contact_permission_denied), Toast.LENGTH_SHORT).show()
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
