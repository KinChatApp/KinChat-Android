package com.kinchat.app.features.chat.ui.components.bubble

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kinchat.app.R
import com.kinchat.app.domain.model.MessageType
import com.kinchat.app.features.chat.ui.actions.MessageAction
import com.kinchat.app.features.chat.ui.models.MessageUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.util.concurrent.ConcurrentHashMap

object LinkPreviewCacheManager {
    val cache = ConcurrentHashMap<String, LinkPreviewData>()
}

@Composable
fun BubbleContents(
    message: MessageUiModel,
    isSelectionModeEnabled: Boolean,
    onAction: (MessageAction) -> Unit,
    onSelect: () -> Unit
) {
    when (message.type) {
        MessageType.TEXT -> TextContent(message, isSelectionModeEnabled, onSelect)
        MessageType.IMAGE, MessageType.VIDEO -> MediaContent(
            message = message,
            isSelectionModeEnabled = isSelectionModeEnabled,
            onSelect = onSelect,
            onMediaClick = { message.media?.url?.let { onAction(MessageAction.OpenMedia(it, message.type)) } }
        )
        MessageType.AUDIO -> AudioContent(
            message = message,
            isSelectionModeEnabled = isSelectionModeEnabled,
            onSelect = onSelect,
            onPlayToggle = {
                if (message.audio?.isPlaying == true) onAction(MessageAction.PauseAudio(message))
                else onAction(MessageAction.PlayAudio(message))
            }
        )
        MessageType.DOCUMENT -> DocumentContent(
            message = message,
            isSelectionModeEnabled = isSelectionModeEnabled,
            onSelect = onSelect,
            onDownload = { message.media?.url?.let { onAction(MessageAction.DownloadMedia(it, message.type)) } }
        )
        else -> TextContent(message, isSelectionModeEnabled, onSelect)
    }
}

@Composable
private fun TextContent(message: MessageUiModel, isSelectionModeEnabled: Boolean, onSelect: () -> Unit) {
    val textColor = if (message.isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val linkColor = if (message.isMe) MaterialTheme.colorScheme.inversePrimary else MaterialTheme.colorScheme.primary
    val uriHandler = LocalUriHandler.current
    val haptic = LocalHapticFeedback.current

    val linkRegex = android.util.Patterns.WEB_URL.toRegex()
    val matchResult = linkRegex.find(message.content)
    val firstUrl = matchResult?.value

    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        for (match in linkRegex.findAll(message.content)) {
            append(message.content.substring(lastIndex, match.range.first))
            pushStringAnnotation(tag = "URL", annotation = match.value)
            withStyle(style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Medium)) {
                append(match.value)
            }
            pop()
            lastIndex = match.range.last + 1
        }
        append(message.content.substring(lastIndex))
    }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Column {
        Text(
            text = annotatedString,
            color = textColor,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .pointerInput(message.id) {
                    detectTapGestures(
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelect()
                        },
                        onTap = { pos ->
                            if (isSelectionModeEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect()
                            } else {
                                layoutResult?.let { layout ->
                                    val offset = layout.getOffsetForPosition(pos)
                                    annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                        .firstOrNull()?.let { annotation ->
                                            var url = annotation.item
                                            if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "https://$url" }
                                            try { uriHandler.openUri(url) } catch (e: Exception) {}
                                        }
                                }
                            }
                        }
                    )
                },
            onTextLayout = { layoutResult = it }
        )

        if (firstUrl != null) {
            Spacer(modifier = Modifier.height(8.dp))
            LinkPreviewWidget(url = firstUrl, isMe = message.isMe, isSelectionModeEnabled = isSelectionModeEnabled, onSelect = onSelect)
        }
    }
}

@Composable
private fun LinkPreviewWidget(url: String, isMe: Boolean, isSelectionModeEnabled: Boolean, onSelect: () -> Unit) {
    val previewData = remember { mutableStateOf<LinkPreviewData?>(LinkPreviewCacheManager.cache[url]) }
    val uriHandler = LocalUriHandler.current
    val haptic = LocalHapticFeedback.current

    val bgColor = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
    val contentColor = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(url) {
        if (previewData.value == null) {
            val fetchedData = fetchLinkPreview(url)
            if (fetchedData != null) {
                LinkPreviewCacheManager.cache[url] = fetchedData
                previewData.value = fetchedData
            }
        }
    }

    previewData.value?.let { data ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor)
                .pointerInput(url) {
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
                                try { uriHandler.openUri(data.url) } catch (e: Exception) {}
                            }
                        }
                    )
                }
        ) {
            if (data.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(data.imageUrl).crossfade(true).build(),
                    contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(140.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                )
            }
            Column(modifier = Modifier.padding(10.dp)) {
                if (data.title.isNotBlank()) Text(text = data.title, color = contentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (data.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = data.description, color = contentColor.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

data class LinkPreviewData(val url: String, val title: String, val description: String, val imageUrl: String)

suspend fun fetchLinkPreview(urlStr: String): LinkPreviewData? = withContext(Dispatchers.IO) {
    try {
        var url = urlStr
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"
        val document = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(5000).get()
        val title = document.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() } ?: document.title()
        val imageUrl = document.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() } ?: ""
        if (title.isBlank() && imageUrl.isBlank()) return@withContext null
        LinkPreviewData(url, title, "", imageUrl)
    } catch (e: Exception) { null }
}

@Composable
private fun MediaContent(message: MessageUiModel, isSelectionModeEnabled: Boolean, onSelect: () -> Unit, onMediaClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(modifier = Modifier.clip(RoundedCornerShape(14.dp))
        .pointerInput(message.id) {
            detectTapGestures(
                onLongPress = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSelect() },
                onTap = { if (isSelectionModeEnabled) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelect() } else onMediaClick() }
            )
        }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(message.media?.url).crossfade(true).build(),
            contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(BubbleDimens.MediaHeight).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun AudioContent(message: MessageUiModel, isSelectionModeEnabled: Boolean, onSelect: () -> Unit, onPlayToggle: () -> Unit) {
    val textColor = if (message.isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val isPlaying = message.audio?.isPlaying == true
    val haptic = LocalHapticFeedback.current
    
    Row(modifier = Modifier.widthIn(min = 200.dp).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(if (message.isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                .pointerInput(message.id) {
                    detectTapGestures(
                        onLongPress = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSelect() },
                        onTap = { if (isSelectionModeEnabled) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelect() } else onPlayToggle() }
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
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary, trackColor = textColor.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(R.string.chat_voice_message), color = textColor.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun DocumentContent(message: MessageUiModel, isSelectionModeEnabled: Boolean, onSelect: () -> Unit, onDownload: () -> Unit) {
    val textColor = if (message.isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val timeColor = textColor.copy(alpha = 0.7f)
    val haptic = LocalHapticFeedback.current
    
    Row(
        modifier = Modifier.widthIn(min = 220.dp).clip(RoundedCornerShape(10.dp)).background(textColor.copy(alpha = 0.06f)).padding(10.dp)
            .pointerInput(message.id) {
                detectTapGestures(
                    onLongPress = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSelect() },
                    onTap = { if (isSelectionModeEnabled) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelect() } else onDownload() }
                )
            },
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
