package com.kinchat.app.features.chat.ui.components.bubble

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinchat.app.R
import com.kinchat.app.domain.model.TickState
import com.kinchat.app.features.chat.ui.models.MessageUiModel
import com.kinchat.app.features.chat.ui.models.ReplyPreviewUiState
import com.kinchat.app.features.chat.ui.mapper.ReactionMapper

@Composable
fun BubbleHeader(message: MessageUiModel) {
    if (message.status.isForwarded && !message.status.isDeleted) {
        val timeColor = (if (message.isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)) {
            Icon(Icons.Default.Reply, contentDescription = null, tint = timeColor, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.chat_forwarded), color = timeColor, fontSize = 11.sp, fontStyle = FontStyle.Italic)
        }
    }
}

@Composable
fun ReplyPreview(reply: ReplyPreviewUiState?) {
    if (reply == null) return
    Box(
        modifier = Modifier
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            .padding(8.dp)
    ) {
        Column {
            Text(text = reply.senderName, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = reply.previewText, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
        }
    }
}

@Composable
fun ColumnScope.BubbleFooter(message: MessageUiModel) {
    val timeColor = (if (message.isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f)
    Row(
        modifier = Modifier
            .align(Alignment.End)
            .padding(top = 2.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (message.status.isEdited) {
            Text(text = "${stringResource(R.string.chat_edited)} ", color = timeColor, fontSize = 10.sp, fontStyle = FontStyle.Italic)
        }
        Text(text = message.formattedTime, color = timeColor, fontSize = 11.sp)
        if (message.isMe) {
            Spacer(modifier = Modifier.width(4.dp))
            when (message.status.tickState) {
                TickState.SENDING -> Text("...", color = timeColor, fontSize = 11.sp)
                TickState.SENT -> Icon(Icons.Default.Done, contentDescription = stringResource(R.string.desc_sent), tint = timeColor, modifier = Modifier.size(15.dp))
                TickState.DELIVERED -> Icon(Icons.Default.DoneAll, contentDescription = stringResource(R.string.desc_delivered), tint = timeColor, modifier = Modifier.size(16.dp))
                TickState.READ -> Icon(Icons.Default.DoneAll, contentDescription = stringResource(R.string.desc_read), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun ReactionBar(message: MessageUiModel) {
    if (message.reactions.isEmpty() || message.status.isDeleted) return
    Row(
        modifier = Modifier
            .offset(y = 12.dp, x = if (message.isMe) (-12).dp else 12.dp)
            .shadow(2.dp, CircleShape)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        message.reactions.take(3).forEach { react ->
            Text(text = ReactionMapper.toEmoji(react.type), fontSize = 12.sp)
        }
        if (message.reactions.size > 1) {
            Text(text = "${message.reactions.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 4.dp))
        }
    }
}
