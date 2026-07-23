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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.kinchat.app.features.dashboard.ui.components.ChatContextMenuBottomSheet
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
    
    // সিলেক্টেড ট্যাবের স্টেট
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
            
            // চ্যাট লিস্টের উপরে ফিল্টার ট্যাব
            ChatFilterTabs(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            // নতুন ছোট করা চ্যাট লিস্ট সেকশন
            ChatListSection(
                isLoading = uiState.isLoading,
                chats = uiState.chats, // ভবিষ্যতে এখানে selectedFilter অনুযায়ী ফিল্টার করা লিস্ট পাস করবেন
                onChatClick = onNavigateToChat,
                onChatLongPress = viewModel::openContextMenu
            )
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
