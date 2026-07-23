package com.kinchat.app.features.chat.info.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kinchat.app.features.chat.info.ui.components.ChatInfoActions
import com.kinchat.app.features.chat.info.ui.components.ChatInfoHeader
import com.kinchat.app.features.chat.info.ui.components.ChatInfoMedia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInfoScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMedia: (userId: String) -> Unit,
    viewModel: ChatInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.messageCleared) {
        if (uiState.messageCleared) {
            Toast.makeText(context, "Chat cleared successfully", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact info") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                ChatInfoHeader(
                    profile = uiState.profile,
                    isLoading = uiState.isLoading,
                    onAudioCallClick = { /* TODO: Trigger Global Audio Call Intent */ },
                    onVideoCallClick = { /* TODO: Trigger Global Video Call Intent */ },
                    onSearchClick = { /* TODO: Implement Search In Chat */ }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                ChatInfoMedia(
                    mediaCount = uiState.mediaCount,
                    isLoading = uiState.isLoading,
                    onMediaClick = { 
                        uiState.profile?.id?.let { onNavigateToMedia(it) } 
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                ChatInfoActions(
                    isMuted = uiState.isMuted,
                    isBlocked = uiState.isBlocked,
                    actionLoading = uiState.actionLoading,
                    onMuteToggle = { viewModel.toggleMute() },
                    onClearChatClick = { viewModel.clearChat() },
                    onBlockClick = { viewModel.toggleBlock() },
                    onReportClick = { viewModel.reportUser() }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
