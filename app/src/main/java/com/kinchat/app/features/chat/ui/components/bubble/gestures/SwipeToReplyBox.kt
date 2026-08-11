package com.kinchat.app.features.chat.ui.components.bubble.gestures

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kinchat.app.R
import com.kinchat.app.features.chat.ui.components.bubble.BubbleDimens
import kotlin.math.roundToInt

@Composable
fun SwipeToReplyBox(
    messageId: String,
    isSelectionModeEnabled: Boolean,
    isDeleted: Boolean,
    onSwipeReply: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    
    val swipeThresholdPx = remember(density) { with(density) { BubbleDimens.SwipeThreshold.toPx() } }
    val swipeMaxPx = remember(density) { with(density) { BubbleDimens.SwipeMaxOffset.toPx() } }
    
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    
    val displayOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = if (isDragging) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "swipeReplyOffset"
    )

    Box(modifier = modifier) {
        if (!isDeleted) {
            Icon(
                imageVector = Icons.Default.Reply,
                contentDescription = stringResource(R.string.desc_reply),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
                    .alpha((dragOffset / swipeThresholdPx).coerceIn(0f, 1f))
            )
        }

        val swipeModifier = Modifier
            .offset { IntOffset(displayOffset.roundToInt(), 0) }
            .pointerInput(messageId, isSelectionModeEnabled, "swipe") {
                detectHorizontalDragGestures(
                    onDragStart = { if (!isSelectionModeEnabled) isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        if (dragOffset > swipeThresholdPx) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSwipeReply()
                        }
                        dragOffset = 0f
                    },
                    onDragCancel = { isDragging = false; dragOffset = 0f },
                    onHorizontalDrag = { change, amount ->
                        if (!isSelectionModeEnabled) {
                            change.consume()
                            dragOffset = (dragOffset + amount).coerceIn(0f, swipeMaxPx)
                        }
                    }
                )
            }

        content(swipeModifier)
    }
}
