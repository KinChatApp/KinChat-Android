package com.kinchat.app.features.chat.ui.components.bubble

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kinchat.app.features.chat.ui.components.bubble.utils.LinkPreviewCacheManager
import com.kinchat.app.features.chat.ui.components.bubble.utils.LinkPreviewData
import com.kinchat.app.features.chat.ui.components.bubble.utils.fetchLinkPreview

@Composable
fun LinkPreviewWidget(
    url: String, 
    isMe: Boolean, 
    isSelectionModeEnabled: Boolean, 
    onSelect: () -> Unit
) {
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
            }
        }
    }
}
