package com.kinchat.app.features.media.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PermissionRequiredContent(
    showOpenSettings: Boolean,
    onRequestAgain: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Storage permission is needed to access your photos and videos",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRequestAgain) { Text("Grant access") }
            
            if (showOpenSettings) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onOpenSettings) { Text("Open Settings") }
            }
        }
    }
}

@Composable
fun ErrorContent(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Couldn't load your media. Please try again.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
fun EmptyContent(
    modifier: Modifier = Modifier,
    hasPartialAccess: Boolean,
    onAddMore: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (hasPartialAccess) {
                "No media found in your current selection. Tap below to choose more photos or videos."
            } else {
                "No photos or videos found on this device."
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        if (hasPartialAccess) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAddMore) { Text("Choose photos & videos") }
        }
    }
}
