package com.kinchat.app.features.chat.info.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChatInfoDangerZone(
    isBlocked: Boolean,
    actionLoading: Boolean,
    onClearClick: () -> Unit,
    onBlockClick: () -> Unit,
    onReportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        DangerActionItem(
            icon = Icons.Default.DeleteOutline,
            text = "Clear chat",
            onClick = onClearClick,
            enabled = !actionLoading
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.padding(start = 56.dp)
        )
        
        DangerActionItem(
            icon = Icons.Default.Block,
            text = if (isBlocked) "Unblock contact" else "Block contact",
            onClick = onBlockClick,
            enabled = !actionLoading
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.padding(start = 56.dp)
        )

        DangerActionItem(
            icon = Icons.Default.WarningAmber,
            text = "Report contact",
            onClick = onReportClick,
            enabled = !actionLoading
        )
    }
}
