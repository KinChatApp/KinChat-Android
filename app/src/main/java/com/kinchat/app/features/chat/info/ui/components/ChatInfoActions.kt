package com.kinchat.app.features.chat.info.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
        // Settings Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !actionLoading, onClick = onMuteToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Mute notifications",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Mute notifications",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Switch(
                    checked = isMuted,
                    onCheckedChange = { onMuteToggle() },
                    enabled = !actionLoading
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Danger Zone
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            DangerActionItem(
                icon = Icons.Default.DeleteOutline,
                text = "Clear chat",
                onClick = { showClearDialog = true },
                enabled = !actionLoading
            )
            Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), modifier = Modifier.padding(start = 56.dp))
            
            DangerActionItem(
                icon = Icons.Default.Block,
                text = if (isBlocked) "Unblock contact" else "Block contact",
                onClick = { showBlockDialog = true },
                enabled = !actionLoading
            )
            Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), modifier = Modifier.padding(start = 56.dp))

            DangerActionItem(
                icon = Icons.Default.WarningAmber,
                text = "Report contact",
                onClick = { showReportDialog = true },
                enabled = !actionLoading
            )
        }
    }

    // Dialogs
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Chat") },
            text = { Text("Are you sure you want to clear messages in this chat?") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    onClearChatClick()
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text(if (isBlocked) "Unblock User" else "Block Contact") },
            text = { Text(if (isBlocked) "Unblock this user?" else "Blocked contacts will no longer be able to call you or send you messages.") },
            confirmButton = {
                TextButton(onClick = {
                    showBlockDialog = false
                    onBlockClick()
                }) {
                    Text(if (isBlocked) "Unblock" else "Block", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report User") },
            text = { Text("Report this user to KinChat?") },
            confirmButton = {
                TextButton(onClick = {
                    showReportDialog = false
                    onReportClick()
                }) {
                    Text("Report", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DangerActionItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
    }
}
