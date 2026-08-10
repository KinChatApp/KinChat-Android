package com.kinchat.app.features.chat.ui.components.bubble

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinchat.app.R
import com.kinchat.app.domain.model.MessageType
import com.kinchat.app.features.chat.ui.models.MessageUiModel
import com.kinchat.app.features.chat.ui.components.bubble.gestures.SwipeToReplyBox
import com.kinchat.app.features.chat.ui.components.bubble.utils.rememberMessageBubbleShape

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

    val handleTap = {
        if (isSelectionModeEnabled && !message.status.isDeleted) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSelect() 
        } 
    }

    val handleLongPress = {
        if (!message.status.isDeleted) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onSelect()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(selectionBgColor)
            .pointerInput(message.id, "outer") {
                detectTapGestures(
                    onLongPress = { handleLongPress() },
                    onTap = { handleTap() }
                )
            }
            .padding(top = topPad, bottom = bottomPad, start = 12.dp, end = 12.dp),
        contentAlignment = if (message.isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        val bubbleColor = if (message.isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        val shape = rememberMessageBubbleShape(
            isMe = message.isMe,
            isTopInGroup = message.isTopInGroup,
            showTail = message.showTail
        )

        Row(verticalAlignment = Alignment.Bottom) {
            if (!message.isMe) {
                if (message.showTail) {
                    MessageAvatar(senderName = message.senderName)
                } else {
                    Spacer(modifier = Modifier.width(BubbleDimens.AvatarSize))
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Box(modifier = Modifier.scale(scale)) {
                if (isSelected) {
                    MessageReactionPopup(
                        haptic = haptic,
                        onReact = onReact
                    )
                }

                SwipeToReplyBox(
                    messageId = message.id,
                    isSelectionModeEnabled = isSelectionModeEnabled,
                    isDeleted = message.status.isDeleted,
                    onSwipeReply = onSwipeReply
                ) { swipeModifier ->
                    Column(
                        modifier = swipeModifier
                            .widthIn(min = BubbleDimens.MinWidth, max = BubbleDimens.MaxWidth)
                            .pointerInput(message.id, "inner_tap") {
                                detectTapGestures(
                                    onLongPress = { handleLongPress() },
                                    onTap = { handleTap() }
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
                            Text(
                                text = stringResource(R.string.chat_message_deleted),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        } else {
                            content()
                        }
                        BubbleFooter(message)
                    }
                }
                ReactionBar(message)
            }
        }
    }
}
