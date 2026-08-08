package com.kinchat.app.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun BatteryOptimizationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Keep Message Notifications Reliable")
        },
        text = {
            Text(
                "Your phone's battery optimization settings may delay or " +
                "prevent message notifications when the app is running " +
                "in the background. To ensure notifications arrive reliably, " +
                "please adjust your battery settings."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Adjust Battery Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}
