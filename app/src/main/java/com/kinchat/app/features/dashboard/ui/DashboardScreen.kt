package com.kinchat.app.features.dashboard.ui

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.kinchat.app.features.chat.ui.components.ChatContextMenu
import com.kinchat.app.features.dashboard.ui.components.ChatFilterTabs
import com.kinchat.app.features.dashboard.ui.components.ChatListSection
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

    // Transient UI (context menu / delete dialog) lives in the ViewModel, which
    // survives tab switches (state is saved/restored). Clear it whenever the
    // dashboard leaves composition so it cannot reappear unexpectedly on return.
    DisposableEffect(Unit) {
        onDispose { viewModel.clearTransientUiState() }
    }

    var selectedFilter by remember { mutableStateOf("All") }

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
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            ChatListSection(
                isLoading = uiState.isLoading,
                chats = uiState.chats,
                onChatClick = onNavigateToChat,
                onChatLongPress = viewModel::openContextMenu
            )
        }

        // সঠিক Context Menu কল করা হচ্ছে
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
