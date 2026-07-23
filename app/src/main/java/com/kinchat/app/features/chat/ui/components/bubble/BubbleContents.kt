package com.kinchat.app.features.chat.ui.components.bubble

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kinchat.app.R
import com.kinchat.app.domain.model.MessageType
import com.kinchat.app.features.chat.ui.actions.MessageAction
import com.kinchat.app.features.chat.ui.models.MessageUiModel

@Composable
fun BubbleContents(message: MessageUiModel, onAction: (MessageAction) -> Unit) {
    when (message.type) {
        MessageType.TEXT -> TextContent(message)
        MessageType.IMAGE, MessageType.VIDEO -> MediaContent(
            message = message,
            onMediaClick = { message.media?.url?.let { onAction(MessageAction.OpenMedia(it, message.type)) } }
        )
        MessageType.AUDIO -> AudioContent(
            message = message,
            onPlayToggle = {
                if (message.audio?.isPlaying == true) onAction(MessageAction.PauseAudio(message))
                else onAction(MessageAction.PlayAudio(message))
            }
        )
        MessageType.DOCUMENT -> DocumentContent(
            message = message,
            onDownload = { message.media?.url?.let { onAction(MessageAction.DownloadMedia(it, message.type)) } }
        )
        else -> TextContent(message)
    }
}

@Composable
private fun TextContent(message: MessageUiModel) {
    val textColor = if (message.isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Text(text = message.content, color = textColor, fontSize = 16.sp, lineHeight = 22.sp, modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
private fun MediaContent(message: MessageUiModel, onMediaClick: () -> Unit) {
    val textColor = if (message.isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Column {
        Box(modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable { onMediaClick() }) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(message.media?.url)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.chat_media_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(BubbleDimens.MediaHeight).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            )
            if (message.type == MessageType.VIDEO) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Videocam, contentDescription = "Play Video", tint = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.size(48.dp))
                }
            }
        }
        if (message.content.isNotBlank()) {
            Text(text = message.content, color = textColor, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp))
        }
    }
}

@Composable
private fun AudioContent(message: MessageUiModel, onPlayToggle: () -> Unit) {
    val textColor = if (message.isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val isPlaying = message.audio?.isPlaying == true

    Row(modifier = Modifier.widthIn(min = 200.dp).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(if (message.isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer).clickable { onPlayToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(if (isPlaying) R.string.desc_pause_audio else R.string.desc_play_audio),
                tint = if (message.isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            LinearProgressIndicator(
                progress = { message.audio?.progress ?: 0f },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary, trackColor = textColor.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(R.string.chat_voice_message), color = textColor.copy(alpha = 0.7f), fontSize = 11.sp) 
        }
    }
}

@Composable
private fun DocumentContent(message: MessageUiModel, onDownload: () -> Unit) {
    val textColor = if (message.isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val timeColor = textColor.copy(alpha = 0.7f)

    Row(
        modifier = Modifier.widthIn(min = 220.dp).clip(RoundedCornerShape(10.dp)).background(textColor.copy(alpha = 0.06f)).clickable { onDownload() }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = message.media?.fileName ?: stringResource(R.string.chat_document), color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = message.media?.formattedSize ?: "", color = timeColor, fontSize = 11.sp)
        }
        Icon(Icons.Default.Download, contentDescription = stringResource(R.string.desc_download), tint = timeColor, modifier = Modifier.size(18.dp))
    }
}
