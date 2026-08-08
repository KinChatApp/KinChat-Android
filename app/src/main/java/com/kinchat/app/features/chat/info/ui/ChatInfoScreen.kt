package com.kinchat.app.features.chat.info.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kinchat.app.features.chat.info.ui.components.ChatInfoActions
import com.kinchat.app.features.chat.info.ui.components.ChatInfoEffects
import com.kinchat.app.features.chat.info.ui.components.ChatInfoHeader
import com.kinchat.app.features.chat.info.ui.components.ChatInfoMedia
import com.kinchat.app.features.chat.info.ui.components.ChatInfoTopBar
import com.kinchat.app.features.chat.info.ui.components.ChatInsightsItem

@Composable
fun ChatInfoScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMedia: (userId: String) -> Unit,
    onNavigateToInsights: (userId: String) -> Unit,
    viewModel: ChatInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ChatInfoEffects(
        error = uiState.error,
        messageCleared = uiState.messageCleared,
        onErrorConsumed = viewModel::clearError,
        onNavigateBack = onNavigateBack
    )

    Scaffold(
        topBar = { ChatInfoTopBar(onNavigateBack = onNavigateBack) },
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
                    onAudioCallClick = { },
                    onVideoCallClick = { },
                    onSearchClick = { }
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
                ChatInsightsItem(
                    onNavigateToInsights = {
                        uiState.profile?.id?.let { onNavigateToInsights(it) }
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
