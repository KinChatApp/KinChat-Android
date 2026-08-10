package com.kinchat.app.features.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import com.kinchat.app.core.utils.MediaDownloader
import com.kinchat.app.domain.model.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun MessageContent(
    message: ChatMessage,
    isMe: Boolean,
    onMediaClick: () -> Unit,
    searchQuery: String? = null,
    isSearchFocused: Boolean = false
) {
    val context = LocalContext.current

    if (message.deletedAt != null) {
        Text(
            text = "🚫 You deleted a message",
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            fontSize = 15.sp
        )
        return
    }

    val content = message.content ?: ""
    
    // FIX: Extract URL from attachments first, fallback to content
    val mediaUrl = message.attachments?.firstOrNull()?.let { attachment ->
        attachment.fileUrl?.takeIf { it.isNotBlank() } ?: attachment.localUri
    } ?: content

    when (message.type) {
        "image" -> {
            MediaMessage(
                url = mediaUrl,
                isVideo = false,
                isSending = message.isSending,
                onClick = onMediaClick
            )
        }
        "video" -> {
            MediaMessage(
                url = mediaUrl,
                isVideo = true,
                isSending = message.isSending,
                onClick = onMediaClick
            )
        }
        "audio" -> {
            AudioMessagePlayer(
                url = mediaUrl,
                isMe = isMe,
                isSending = message.isSending
            )
        }
        "file", "document" -> {
            val fileName = message.metadata?.get("file_name")?.jsonPrimitive?.content
                ?: message.metadata?.get("fileName")?.jsonPrimitive?.content
                ?: "Document_File.pdf"

            DocumentMessage(
                fileName = fileName,
                isSending = message.isSending,
                isMe = isMe,
                onDownload = {
                    MediaDownloader.downloadMedia(context, mediaUrl, message.type ?: "document", fileName)
                }
            )
        }
        else -> {
            TextMessage(
                text = content,
                query = searchQuery,
                isSearchFocused = isSearchFocused,
                isMe = isMe
            )
        }
    }
}

@Composable
private fun TextMessage(text: String, query: String?, isSearchFocused: Boolean, isMe: Boolean) {
    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    if (query.isNullOrEmpty()) {
        Text(text = text, color = textColor, fontSize = 15.sp, lineHeight = 22.sp)
        return
    }

    val annotatedString = buildAnnotatedString {
        var startIndex = 0
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()

        while (startIndex < text.length) {
            val index = lowerText.indexOf(lowerQuery, startIndex)
            if (index == -1) {
                withStyle(style = SpanStyle(color = textColor)) {
                    append(text.substring(startIndex))
                }
                break
            }

            withStyle(style = SpanStyle(color = textColor)) {
                append(text.substring(startIndex, index))
            }

            withStyle(
                style = SpanStyle(
                    background = if (isSearchFocused) Color(0xFFFFA726) else Color(0xFFFFF176),
                    color = Color.Black
                )
            ) {
                append(text.substring(index, index + query.length))
            }
            startIndex = index + query.length
        }
    }

    Text(text = annotatedString, fontSize = 15.sp, lineHeight = 22.sp)
}

@Composable
private fun MediaMessage(url: String, isVideo: Boolean, isSending: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (isVideo) add(VideoFrameDecoder.Factory())
            }
            .build()
    }

    Box(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .heightIn(max = 300.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isSending, onClick = onClick)
            .alpha(if (isSending) 0.7f else 1f)
            .background(Color.Black)
    ) {
        AsyncImage(
            model = url,
            imageLoader = imageLoader,
            contentDescription = "Media Content",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 300.dp)
        )

        if (isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioMessagePlayer(url: String, isMe: Boolean, isSending: Boolean) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var duration by remember { mutableStateOf(0L) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                } else if (state == Player.STATE_ENDED) {
                    isPlaying = false
                    progress = 0f
                    exoPlayer.seekTo(0)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            if (duration > 0) {
                progress = (exoPlayer.currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            }
            delay(100)
        }
    }

    val contentColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val bgColor = if (isMe) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .widthIn(min = 200.dp, max = 250.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp)
            .alpha(if (isSending) 0.6f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                if (!isSending) {
                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                }
            },
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Slider(
            value = progress,
            onValueChange = {
                progress = it
                exoPlayer.seekTo((it * duration).toLong())
            },
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = contentColor,
                activeTrackColor = contentColor,
                inactiveTrackColor = contentColor.copy(alpha = 0.3f)
            ),
            enabled = !isSending
        )
    }
}

@Composable
private fun DocumentMessage(fileName: String, isSending: Boolean, isMe: Boolean, onDownload: () -> Unit) {
    val contentColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val iconBgColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val iconTintColor = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isMe) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp)
            .alpha(if (isSending) 0.6f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = "Document",
                tint = iconTintColor
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isSending) "Uploading File..." else fileName,
                color = contentColor,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!isSending) {
                Text(
                    text = "Download",
                    color = if (isMe) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable { onDownload() }
                        .padding(top = 2.dp)
                )
            }
        }
    }
}
