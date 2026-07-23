package com.kinchat.app.features.chat.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ChatHeaderMenu(
    isMenuExpanded: Boolean,
    isMuted: Boolean,
    isBlocked: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onGoToInfo: () -> Unit,
    onAction: (String) -> Unit
) {
    Box {
        IconButton(onClick = { onMenuToggle(true) }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { onMenuToggle(false) }
        ) {
            DropdownMenuItem(
                text = { Text("View contact") },
                onClick = {
                    onGoToInfo()
                    onMenuToggle(false)
                }
            )
            DropdownMenuItem(
                text = { Text("Search") },
                onClick = {
                    onAction("search")
                    onMenuToggle(false)
                }
            )
            DropdownMenuItem(
                text = { Text("Media & documents") },
                onClick = {
                    onAction("media")
                    onMenuToggle(false)
                }
            )
            DropdownMenuItem(
                text = { Text(if (isMuted) "Unmute notifications" else "Mute notifications") },
                onClick = {
                    onAction("mute")
                    onMenuToggle(false)
                }
            )
            DropdownMenuItem(
                text = { Text("Clear chat") },
                onClick = {
                    onAction("clear")
                    onMenuToggle(false)
                }
            )
            DropdownMenuItem(
                text = { Text("Report") },
                onClick = {
                    onAction("report")
                    onMenuToggle(false)
                }
            )
            DropdownMenuItem(
                text = { Text(if (isBlocked) "Unblock" else "Block", color = if (isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) },
                onClick = {
                    onAction("block")
                    onMenuToggle(false)
                }
            )
        }
    }
}
