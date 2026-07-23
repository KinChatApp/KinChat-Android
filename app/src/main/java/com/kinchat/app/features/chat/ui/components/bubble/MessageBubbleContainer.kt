package com.kinchat.app.features.chat.ui.components.bubble

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinchat.app.R
import com.kinchat.app.domain.model.MessageType
import com.kinchat.app.features.chat.ui.models.MessageUiModel
import kotlin.math.roundToInt

@Composable
fun MessageBubbleContainer(
    message: MessageUiModel,
    isSelected: Boolean,
    onSelect: (MessageUiModel?) -> Unit,
    onSwipeReply: () -> Unit,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(if (isSelected) 0.98f else 1f, label = "scale")
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
            .pointerInput(message.id) {
                detectTapGestures(
                    onLongPress = { if (!message.status.isDeleted) onSelect(message) },
                    onTap = { if (isSelected) onSelect(null) }
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
                        .pointerInput(message.id) {
                            detectHorizontalDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd = {
                                    isDragging = false
                                    if (dragOffset > swipeThresholdPx) onSwipeReply()
                                    dragOffset = 0f
                                },
                                onDragCancel = { isDragging = false; dragOffset = 0f },
                                onHorizontalDrag = { change, amount ->
                                    change.consume()
                                    dragOffset = (dragOffset + amount).coerceIn(0f, swipeMaxPx)
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
