package com.kinchat.app.features.chat.ui.components.bubble

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
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

// 🚀 ഗ্লোবাল ক্যাশ মেমরি তৈরি করা হলো (In-Memory Caching)
object LinkPreviewCacheManager {
    val cache = ConcurrentHashMap<String, LinkPreviewData>()
}

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
    val linkColor = if (message.isMe) MaterialTheme.colorScheme.inversePrimary else MaterialTheme.colorScheme.primary
    val uriHandler = LocalUriHandler.current

    val linkRegex = android.util.Patterns.WEB_URL.toRegex()
    val matchResult = linkRegex.find(message.content)
    val firstUrl = matchResult?.value

    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        for (match in linkRegex.findAll(message.content)) {
            append(message.content.substring(lastIndex, match.range.first))
            pushStringAnnotation(tag = "URL", annotation = match.value)
            withStyle(
                style = SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                )
            ) {
                append(match.value)
            }
            pop()
            lastIndex = match.range.last + 1
        }
        append(message.content.substring(lastIndex))
    }

    Column {
        ClickableText(
            text = annotatedString,
            style = LocalTextStyle.current.copy(color = textColor, fontSize = 16.sp, lineHeight = 22.sp),
            modifier = Modifier.padding(horizontal = 4.dp),
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        var url = annotation.item
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            url = "https://$url"
                        }
                        try {
                            uriHandler.openUri(url)
                        } catch (e: Exception) {
                            // No app to handle URL
                        }
                    }
            }
        )

        if (firstUrl != null) {
            Spacer(modifier = Modifier.height(8.dp))
            LinkPreviewWidget(url = firstUrl, isMe = message.isMe)
        }
    }
}

@Composable
private fun LinkPreviewWidget(url: String, isMe: Boolean) {
    // 🚀 ইনিশিয়াল ভ্যালু হিসেবে ক্যাশ চেক করা হচ্ছে
    val previewData = remember { mutableStateOf<LinkPreviewData?>(LinkPreviewCacheManager.cache[url]) }
    val uriHandler = LocalUriHandler.current

    val bgColor = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
    val contentColor = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(url) {
        // 🚀 ক্যাশে না থাকলে তবেই ওয়েবসাইট থেকে ডেটা আনবে
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
                .clickable {
                    try {
                        uriHandler.openUri(data.url)
                    } catch (e: Exception) { /* Handle Exception */ }
                }
        ) {
            if (data.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(data.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                )
            }
            Column(modifier = Modifier.padding(10.dp)) {
                if (data.title.isNotBlank()) {
                    Text(
                        text = data.title,
                        color = contentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (data.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = data.description,
                        color = contentColor.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = url,
                    color = contentColor.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

data class LinkPreviewData(
    val url: String,
    val title: String,
    val description: String,
    val imageUrl: String
)

suspend fun fetchLinkPreview(urlStr: String): LinkPreviewData? = withContext(Dispatchers.IO) {
    try {
        var url = urlStr
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }

        val document = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")
            .header("Accept-Language", "en-US,en;q=0.9")
            .referrer("https://www.google.com/")
            .followRedirects(true)
            .ignoreHttpErrors(true)
            .timeout(10000)
            .get()

        val title = document.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() } ?: document.title()
        val description = document.select("meta[property=og:description]").attr("content").takeIf { it.isNotBlank() }
            ?: document.select("meta[name=description]").attr("content")

        val imageUrl = document.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() } ?: ""

        if (title.contains("Error Facebook", ignoreCase = true) || title.equals("Error", ignoreCase = true) || title.contains("Log in to Facebook", ignoreCase = true)) {
            return@withContext null
        }

        if (title.isBlank() && description.isBlank() && imageUrl.isBlank()) {
            return@withContext null
        }

        LinkPreviewData(url, title, description, imageUrl)
    } catch (e: Exception) {
        Log.e("LinkPreview", "Error fetching preview for $urlStr: ${e.message}")
        null
    }
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
