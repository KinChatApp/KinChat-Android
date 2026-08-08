package com.kinchat.app.features.chat.info.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChatInfoActions(
    isMuted: Boolean,
    isBlocked: Boolean,
    actionLoading: Boolean,
    onMuteToggle: () -> Unit,
    onClearChatClick: () -> Unit,
    onBlockClick: () -> Unit,
    onReportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        ChatInfoSettingsSection(
            isMuted = isMuted,
            actionLoading = actionLoading,
            onMuteToggle = onMuteToggle
        )

        Spacer(modifier = Modifier.height(12.dp))

        ChatInfoDangerZone(
            isBlocked = isBlocked,
            actionLoading = actionLoading,
            onClearClick = { showClearDialog = true },
            onBlockClick = { showBlockDialog = true },
            onReportClick = { showReportDialog = true }
        )
    }

    if (showClearDialog) {
        ClearChatConfirmDialog(
            onConfirm = {
                showClearDialog = false
                onClearChatClick()
            },
            onDismiss = { showClearDialog = false }
        )
    }

    if (showBlockDialog) {
        BlockContactConfirmDialog(
            isBlocked = isBlocked,
            onConfirm = {
                showBlockDialog = false
                onBlockClick()
            },
            onDismiss = { showBlockDialog = false }
        )
    }

    if (showReportDialog) {
        ReportContactConfirmDialog(
            onConfirm = {
                showReportDialog = false
                onReportClick()
            },
            onDismiss = { showReportDialog = false }
        )
    }
}
