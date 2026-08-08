package com.kinchat.app.features.chat.info.ui.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun ChatInfoEffects(
    error: String?,
    messageCleared: Boolean,
    onErrorConsumed: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            onErrorConsumed()
        }
    }

    LaunchedEffect(messageCleared) {
        if (messageCleared) {
            Toast.makeText(context, "Chat cleared successfully", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }
}
