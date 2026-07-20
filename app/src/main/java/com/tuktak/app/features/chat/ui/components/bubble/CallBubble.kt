package com.tuktak.app.features.chat.ui.components.bubble

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuktak.app.R
import com.tuktak.app.domain.model.CallType
import com.tuktak.app.features.chat.ui.models.MessageUiModel

@Composable
fun CallBubble(
    message: MessageUiModel,
    isSelected: Boolean,
    onSelect: (MessageUiModel?) -> Unit,
    onJoinCall: () -> Unit
) {
    val callInfo = message.call ?: return
    val scale by animateFloatAsState(if (isSelected) 0.98f else 1f, label = "scale")
    val selectionBgColor by animateColorAsState(targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent, label = "bgColor")

    val icon = when {
        callInfo.isMissedOrFailed -> Icons.Default.CallMissed
        callInfo.type == CallType.VIDEO -> Icons.Default.Videocam
        else -> Icons.Default.Call
    }

    Box(
        modifier = Modifier.fillMaxWidth().background(selectionBgColor)
            .pointerInput(message.id) {
                detectTapGestures(
                    onLongPress = { if (!message.status.isDeleted) onSelect(message) },
                    onTap = { if (isSelected) onSelect(null) }
                )
            }.padding(vertical = 4.dp, horizontal = 12.dp),
        contentAlignment = if (message.isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        val bubbleColor = MaterialTheme.colorScheme.surfaceVariant
        val textColor = MaterialTheme.colorScheme.onSurfaceVariant
        val iconTint = if (callInfo.isMissedOrFailed) MaterialTheme.colorScheme.error else if (callInfo.isIncomingRinging) MaterialTheme.colorScheme.primary else textColor

        Row(
            modifier = Modifier.scale(scale).shadow(1.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))
                .background(bubbleColor).clickable(enabled = callInfo.isIncomingRinging, onClick = onJoinCall)
                .padding(12.dp).widthIn(min = 200.dp, max = 290.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = stringResource(R.string.desc_call_icon), tint = iconTint)
            Spacer(modifier = Modifier.width(12.dp))
            
            // এখান থেকে modifier = Modifier.weight(1f) মুছে ফেলা হয়েছে
            Column { 
                Text(
                    text = stringResource(callInfo.statusTextRes),
                    color = if (callInfo.isMissedOrFailed) MaterialTheme.colorScheme.error else textColor,
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                )
                Text(text = message.formattedTime, color = textColor.copy(alpha = 0.6f), fontSize = 12.sp)
            }
            
            if (callInfo.isIncomingRinging) {
                Spacer(modifier = Modifier.width(12.dp)) // Join বাটনের আগে স্পেস দেওয়া হয়েছে
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
                    Text(text = stringResource(R.string.chat_join_call), color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
