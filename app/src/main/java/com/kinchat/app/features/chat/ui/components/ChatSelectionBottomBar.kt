package com.kinchat.app.features.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChatSelectionBottomBar(
    canEdit: Boolean,
    canReply: Boolean,
    onEditRequested: () -> Unit,
    onReplyRequested: () -> Unit,
    onForwardRequested: () -> Unit,
    onCopyRequested: () -> Unit,
    onDeleteRequested: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (canEdit) {
            IconButton(onClick = onEditRequested) { Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (canReply) {
            IconButton(onClick = onReplyRequested) { Icon(Icons.Default.Reply, "Reply", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        IconButton(onClick = onForwardRequested) { Icon(Icons.AutoMirrored.Filled.Send, "Forward", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        IconButton(onClick = onCopyRequested) { Icon(Icons.Default.ContentCopy, "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        IconButton(onClick = onDeleteRequested) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
    }
}
