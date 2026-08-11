package com.kinchat.app.features.chat.ui.components.bubble

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinchat.app.R
import com.kinchat.app.features.chat.ui.models.MessageUiModel

@Composable
fun DocumentContent(
    message: MessageUiModel, 
    isSelectionModeEnabled: Boolean, 
    onSelect: () -> Unit, 
    onDownload: () -> Unit
) {
    val textColor = if (message.isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val timeColor = textColor.copy(alpha = 0.7f)
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .widthIn(min = 220.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(textColor.copy(alpha = 0.06f))
            .padding(10.dp)
            .pointerInput(message.id, isSelectionModeEnabled) {
                detectTapGestures(
                    onLongPress = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelect() 
                    },
                    onTap = { 
                        if (isSelectionModeEnabled) { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect() 
                        } else {
                            onDownload() 
                        }
                    }
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), 
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.InsertDriveFile, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.primary, 
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.media?.fileName ?: stringResource(R.string.chat_document), 
                color = textColor, 
                fontSize = 14.sp, 
                fontWeight = FontWeight.Medium, 
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = message.media?.formattedSize ?: "", 
                color = timeColor, 
                fontSize = 11.sp
            )
        }
        Icon(
            Icons.Default.Download, 
            contentDescription = stringResource(R.string.desc_download), 
            tint = timeColor, 
            modifier = Modifier.size(18.dp)
        )
    }
}
