package com.kinchat.app.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.judemanutd.autostarter.AutoStartPermissionHelper

@Composable
fun BatteryOptimizationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val isAutoStartAvailable = AutoStartPermissionHelper.getInstance().isAutoStartPermissionAvailable(context)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isAutoStartAvailable) "Background Execution & Battery Settings" else "Keep Message Notifications Reliable")
        },
        text = {
            Text(
                if (isAutoStartAvailable) {
                    "Your phone requires 'Auto-Start' or 'Background execution' permission to receive notifications when the app is closed. Please enable this and adjust your battery settings to ensure reliable notifications."
                } else {
                    "Your phone's battery optimization settings may delay or prevent message notifications when the app is running in the background. To ensure notifications arrive reliably, please adjust your battery settings."
                }
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (isAutoStartAvailable) {
                    AutoStartPermissionHelper.getInstance().getAutoStartPermission(context)
                }
                onConfirm() 
            }) {
                Text(if (isAutoStartAvailable) "Open Settings" else "Adjust Battery Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}
