package com.kinchat.app.features.chat.ui.components.bubble

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kinchat.app.features.chat.ui.models.MessageUiModel

@Composable
fun MediaContent(
    message: MessageUiModel, 
    isSelectionModeEnabled: Boolean, 
    onSelect: () -> Unit, 
    onMediaClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val mediaUrl = message.media?.url
    val imageModel = remember(mediaUrl) {
        when {
            mediaUrl.isNullOrBlank() || mediaUrl == "null" -> null
            mediaUrl.startsWith("http") -> mediaUrl
            mediaUrl.startsWith("content://") -> android.net.Uri.parse(mediaUrl)
            mediaUrl.startsWith("/") -> java.io.File(mediaUrl)
            else -> mediaUrl
        }
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
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
                            onMediaClick() 
                        }
                    }
                )
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageModel)
                .crossfade(true)
                .build(),
            contentDescription = null, 
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(BubbleDimens.MediaHeight)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        )
    }
}
