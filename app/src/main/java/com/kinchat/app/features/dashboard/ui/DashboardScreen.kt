package com.kinchat.app.features.dashboard.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.kinchat.app.features.chat.ui.components.ChatContextMenu
import com.kinchat.app.features.dashboard.ui.components.ChatFilterTabs
import com.kinchat.app.features.dashboard.ui.components.ChatListSection
import com.kinchat.app.features.dashboard.ui.components.HomeHeader
import com.kinchat.app.features.dashboard.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToArchived: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAIChat: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 🚀 ড্যাশবোর্ড থেকে অটোমেটিক পারমিশন পপআপ রিমুভ করা হয়েছে। 
    // তবে আগে থেকে পারমিশন দেওয়া থাকলে সাইলেন্টলি সিঙ্ক করবে।
    LaunchedEffect(Unit) {
        val isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        if (isGranted) {
            viewModel.syncContacts()
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.clearTransientUiState() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HomeHeader(
                onNavigateToSearch = onNavigateToSearch,
                onNavigateToProfile = onNavigateToProfile,
                onNavigateToSaved = onNavigateToSaved,
                onNavigateToArchived = onNavigateToArchived,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            ChatFilterTabs(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = { viewModel.setFilter(it) }
            )

            ChatListSection(
                isLoading = uiState.isLoading,
                chats = uiState.chats,
                onChatClick = onNavigateToChat,
                onChatLongPress = viewModel::openContextMenu
            )
        }

        uiState.selectedChatForMenu?.let { chat ->
            ChatContextMenu(
                selectedChat = chat,
                onDismissRequest = viewModel::closeContextMenu,
                onPinToggle = { viewModel.pinChat(chat.id) },
                onFavoriteToggle = { viewModel.favoriteChat(chat.id) },
                onArchiveToggle = { viewModel.archiveChat(chat.id) },
                onMuteToggle = { viewModel.muteChat(chat.id) },
                onBlockToggle = { viewModel.blockChat(chat.id) },
                onDelete = { viewModel.requestDeleteChat(chat.id) }
            )
        }

        if (uiState.showConfirmDeleteDialog) {
            AlertDialog(
                onDismissRequest = viewModel::cancelDeleteChat,
                title = { Text("Delete Chat") },
                text = { Text("Are you sure you want to delete this chat?") },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmDeleteChat) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelDeleteChat) { Text("Cancel") }
                }
            )
        }
    }
}
