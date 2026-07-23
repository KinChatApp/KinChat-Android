package com.kinchat.app.features.chat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kinchat.app.domain.model.Chat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatContextMenu(
    selectedChat: Chat?,
    onDismissRequest: () -> Unit,
    onPinToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onArchiveToggle: () -> Unit,
    onMuteToggle: () -> Unit,
    onBlockToggle: () -> Unit,
    onDelete: () -> Unit
) {
    if (selectedChat == null) return

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = selectedChat.name,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            ContextMenuItem(
                icon = if (selectedChat.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                label = if (selectedChat.isPinned) "Unpin Chat" else "Pin Chat",
                onClick = { onPinToggle(); onDismissRequest() }
            )

            ContextMenuItem(
                icon = if (selectedChat.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                label = if (selectedChat.isFavorite) "Remove from Favorites" else "Add to Favorites",
                onClick = { onFavoriteToggle(); onDismissRequest() }
            )

            ContextMenuItem(
                icon = Icons.Default.Archive,
                label = if (selectedChat.isArchived) "Unarchive Chat" else "Archive Chat",
                onClick = { onArchiveToggle(); onDismissRequest() }
            )

            ContextMenuItem(
                icon = if (selectedChat.isMuted) Icons.Outlined.NotificationsActive else Icons.Default.NotificationsOff,
                label = if (selectedChat.isMuted) "Unmute Notifications" else "Mute Notifications",
                onClick = { onMuteToggle(); onDismissRequest() }
            )

            ContextMenuItem(
                icon = Icons.Default.Block,
                label = if (selectedChat.isBlocked) "Unblock User" else "Block User",
                onClick = { onBlockToggle(); onDismissRequest() }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            ContextMenuItem(
                icon = Icons.Default.Delete,
                label = "Delete Chat",
                iconTint = MaterialTheme.colorScheme.error,
                textColor = MaterialTheme.colorScheme.error,
                onClick = { onDelete(); onDismissRequest() }
            )
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    label: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = textColor
        )
    }
}
