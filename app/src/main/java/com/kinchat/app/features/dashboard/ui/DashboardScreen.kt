package com.kinchat.app.features.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.kinchat.app.features.dashboard.ui.components.ChatContextMenuBottomSheet
import com.kinchat.app.features.dashboard.ui.components.ChatListItem
import com.kinchat.app.features.dashboard.ui.components.ChatListSkeleton
import com.kinchat.app.features.dashboard.ui.components.HomeHeader
import com.kinchat.app.features.dashboard.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    onNavigateToChat: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToArchived: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAIChat: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
        // Bottom bar is now handled globally in AppNavigation
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                Column {
                    repeat(5) { ChatListSkeleton() }
                }
            } else if (uiState.chats.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No chats yet", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.chats, key = { it.id }) { chat ->
                        ChatListItem(
                            chat = chat,
                            onClick = { onNavigateToChat(chat.id) },
                            onLongPress = { viewModel.openContextMenu(chat) }
                        )
                    }
                }
            }
        }

        uiState.selectedChatForMenu?.let { chat ->
            ChatContextMenuBottomSheet(
                chat = chat,
                onDismiss = viewModel::closeContextMenu,
                onDeleteClick = viewModel::requestDeleteChat
            )
        }

        if (uiState.showConfirmDeleteDialog) {
            AlertDialog(
                onDismissRequest = viewModel::cancelDeleteChat,
                title = { Text("Delete Chat") },
                text = { Text("Are you sure you want to delete this chat?") },
                confirmButton = {
                    TextButton(onClick = { viewModel.uiState.value.selectedChatForMenu?.id?.let { viewModel.confirmDeleteChat(it) } }) {
                        Text("Delete", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelDeleteChat) { Text("Cancel") }
                }
            )
        }
    }
}
