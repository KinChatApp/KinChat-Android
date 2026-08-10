package com.kinchat.app.features.chat.ui.components.bubble

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinchat.app.R
import com.kinchat.app.features.chat.ui.models.MessageUiModel

@Composable
fun AudioContent(
    message: MessageUiModel, 
    isSelectionModeEnabled: Boolean, 
    onSelect: () -> Unit, 
    onPlayToggle: () -> Unit
) {
    val textColor = if (message.isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val isPlaying = message.audio?.isPlaying == true
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .widthIn(min = 200.dp)
            .padding(6.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (message.isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                .pointerInput(message.id) {
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
                                onPlayToggle() 
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (message.isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            LinearProgressIndicator(
                progress = { message.audio?.progress ?: 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary, 
                trackColor = textColor.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.chat_voice_message), 
                color = textColor.copy(alpha = 0.7f), 
                fontSize = 11.sp
            )
        }
    }
}
