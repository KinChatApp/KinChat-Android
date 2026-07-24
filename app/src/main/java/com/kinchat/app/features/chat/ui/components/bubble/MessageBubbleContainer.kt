package com.kinchat.app.features.chat.ui.components.bubble

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.kinchat.app.R
import com.kinchat.app.domain.model.MessageType
import com.kinchat.app.features.chat.ui.models.MessageUiModel
import kotlin.math.roundToInt

@Composable
fun MessageBubbleContainer(
    message: MessageUiModel,
    isSelected: Boolean,
    isSelectionModeEnabled: Boolean,
    onSelect: () -> Unit,
    onSwipeReply: () -> Unit,
    onReact: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.97f else 1f, 
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    val selectionBgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
        label = "bgColor"
    )

    val topPad = if (message.isTopInGroup) 6.dp else 1.dp
    val bottomPad = if (message.showTail) 6.dp else 1.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(selectionBgColor)
            .pointerInput(message.id, "outer") {
                detectTapGestures(
                    onLongPress = {
                        if (!message.status.isDeleted) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelect()
                        }
                    },
                    onTap = { 
                        if (isSelectionModeEnabled && !message.status.isDeleted) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect() 
                        } 
                    }
                )
            }
            .padding(top = topPad, bottom = bottomPad, start = 12.dp, end = 12.dp),
        contentAlignment = if (message.isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        val bubbleColor = if (message.isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        val shape = RoundedCornerShape(
            topStart = if (!message.isMe && !message.isTopInGroup) BubbleDimens.TailRadius else BubbleDimens.FullRadius,
            topEnd = if (message.isMe && !message.isTopInGroup) BubbleDimens.TailRadius else BubbleDimens.FullRadius,
            bottomStart = if (!message.isMe && message.showTail) BubbleDimens.TailRadius else BubbleDimens.FullRadius,
            bottomEnd = if (message.isMe && message.showTail) BubbleDimens.TailRadius else BubbleDimens.FullRadius
        )

        val density = LocalDensity.current
        val swipeThresholdPx = with(density) { BubbleDimens.SwipeThreshold.toPx() }
        val swipeMaxPx = with(density) { BubbleDimens.SwipeMaxOffset.toPx() }
        var dragOffset by remember { mutableFloatStateOf(0f) }
        var isDragging by remember { mutableStateOf(false) }
        val displayOffset by animateFloatAsState(
            targetValue = dragOffset,
            animationSpec = if (isDragging) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "swipeReplyOffset"
        )

        Row(verticalAlignment = Alignment.Bottom) {
            if (!message.isMe) {
                if (message.showTail) {
                    Box(modifier = Modifier.size(BubbleDimens.AvatarSize).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer), contentAlignment = Alignment.Center) {
                        Text(message.senderName.take(1).uppercase(), color = MaterialTheme.colorScheme.onTertiaryContainer, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                } else Spacer(modifier = Modifier.width(BubbleDimens.AvatarSize))
                Spacer(modifier = Modifier.width(6.dp))
            }

            Box(modifier = Modifier.scale(scale)) {
                if (isSelected) {
                    Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, -140)) {
                        Row(
                            modifier = Modifier
                                .shadow(8.dp, RoundedCornerShape(30.dp))
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(30.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val reactions = listOf("👍" to "like", "❤️" to "love", "😂" to "laugh", "😮" to "wow", "😢" to "sad", "🙏" to "pray")
                            reactions.forEach { (emoji, type) ->
                                Text(
                                    text = emoji,
                                    fontSize = 28.sp,
                                    modifier = Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onReact(type)
                                    }
                                )
                            }
                        }
                    }
                }

                if (!message.status.isDeleted) {
                    Icon(
                        imageVector = Icons.Default.Reply,
                        contentDescription = stringResource(R.string.desc_reply),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp).alpha((dragOffset / swipeThresholdPx).coerceIn(0f, 1f))
                    )
                }

                Column(
                    modifier = Modifier
                        .offset { IntOffset(displayOffset.roundToInt(), 0) }
                        .widthIn(min = BubbleDimens.MinWidth, max = BubbleDimens.MaxWidth)
                        .pointerInput(message.id, "swipe") {
                            detectHorizontalDragGestures(
                                onDragStart = { if(!isSelectionModeEnabled) isDragging = true },
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
                                    if(!isSelectionModeEnabled) {
                                        change.consume()
                                        dragOffset = (dragOffset + amount).coerceIn(0f, swipeMaxPx)
                                    }
                                }
                            )
                        }
                        // 🚀 ফিক্স: বাবলের বডিতে সরাসরি লং-প্রেস কাজ করার লজিক
                        .pointerInput(message.id, "inner_tap") {
                            detectTapGestures(
                                onLongPress = {
                                    if (!message.status.isDeleted) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSelect()
                                    }
                                },
                                onTap = {
                                    if (isSelectionModeEnabled && !message.status.isDeleted) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelect()
                                    }
                                }
                            )
                        }
                        .shadow(elevation = 1.dp, shape = shape)
                        .clip(shape)
                        .background(bubbleColor)
                        .padding(if (message.type == MessageType.TEXT) 8.dp else 4.dp)
                ) {
                    BubbleHeader(message)
                    ReplyPreview(message.reply)

                    if (message.status.isDeleted) {
                        Text(stringResource(R.string.chat_message_deleted), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 14.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    } else {
                        content()
                    }
                    BubbleFooter(message)
                }
                ReactionBar(message)
            }
        }
    }
}
