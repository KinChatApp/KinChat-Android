package com.kinchat.app.features.chat.info.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun BlockContactConfirmDialog(
    isBlocked: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isBlocked) "Unblock User" else "Block Contact") },
        text = { 
            Text(if (isBlocked) "Unblock this user?" else "Blocked contacts will no longer be able to call you or send you messages.") 
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(if (isBlocked) "Unblock" else "Block", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
