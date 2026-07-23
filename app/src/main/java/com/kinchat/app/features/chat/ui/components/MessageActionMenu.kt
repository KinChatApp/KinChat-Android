package com.kinchat.app.features.chat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kinchat.app.domain.model.ChatMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageActionMenu(
    message: ChatMessage,
    isMe: Boolean,
    onClose: () -> Unit,
    onAction: (String, ChatMessage) -> Unit
) {
    val reactionMap = mapOf(
        "like" to "👍", "love" to "❤️", "laugh" to "😂",
        "wow" to "😮", "sad" to "😢", "pray" to "🙏"
    )

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Reactions Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                reactionMap.forEach { (key, emoji) ->
                    TextButton(
                        onClick = {
                            onAction("react_$key", message)
                            onClose()
                        },
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            // Actions
            if (message.type == "text") {
                DropdownMenuItem(
                    text = { Text("Copy", fontWeight = FontWeight.Medium) },
                    onClick = { onAction("copy", message); onClose() },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                )
            }
            
            DropdownMenuItem(
                text = { Text("Reply", fontWeight = FontWeight.Medium) },
                onClick = { onAction("reply", message); onClose() },
                leadingIcon = { Icon(Icons.Default.Reply, contentDescription = null) }
            )

            DropdownMenuItem(
                text = { Text("Forward", fontWeight = FontWeight.Medium) },
                onClick = { onAction("forward", message); onClose() },
                leadingIcon = { Icon(Icons.Default.Forward, contentDescription = null) }
            )

            if (isMe && message.type == "text") {
                DropdownMenuItem(
                    text = { Text("Edit", fontWeight = FontWeight.Medium) },
                    onClick = { onAction("edit", message); onClose() },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            if (isMe) {
                DropdownMenuItem(
                    text = { Text("Delete for everyone", fontWeight = FontWeight.Medium) },
                    onClick = { onAction("delete_for_everyone", message); onClose() },
                    leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error)
                )
            }

            DropdownMenuItem(
                text = { Text("Delete for me", fontWeight = FontWeight.Medium) },
                onClick = { onAction("delete_for_me", message); onClose() },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error)
            )

            if (!isMe) {
                DropdownMenuItem(
                    text = { Text("Report", fontWeight = FontWeight.Medium) },
                    onClick = { onAction("report", message); onClose() },
                    leadingIcon = { Icon(Icons.Default.Report, contentDescription = null, tint = Color.Gray) }
                )
            }
        }
    }
}
