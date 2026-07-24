package com.kinchat.app.features.chat.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSelectionTopBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onToggleSave: () -> Unit,
    onPinRequested: () -> Unit,
    onInfoRequested: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectedCount Selected", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onClearSelection) { Icon(Icons.Default.Close, "Clear") }
        },
        actions = {
            if (selectedCount == 1) {
                IconButton(onClick = onToggleSave) { Icon(Icons.Default.StarBorder, "Save") }
                IconButton(onClick = onPinRequested) { Icon(Icons.Default.PushPin, "Pin") }
                IconButton(onClick = onInfoRequested) { Icon(Icons.Default.Info, "Info") }
            }
        }
    )
}
