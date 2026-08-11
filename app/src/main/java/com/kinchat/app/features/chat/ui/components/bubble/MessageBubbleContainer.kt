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
import androidx.compose.ui.zIndex
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
    showReactionPicker: Boolean = false,
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

    // 🚀 ডাইনামিক চেক: মেসেজে কোনো রিঅ্যাকশন আছে কিনা
    val hasReactions = message.reactions.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isSelected) 100f else 0f)
            .background(selectionBgColor)
            .pointerInput(message.id, isSelectionModeEnabled, message.status.isDeleted, "outer") {
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
                // ১. মেসেজ বাবল (ডাইনামিক প্যাডিং: রিঅ্যাকশন থাকলে 16dp স্পেস, না থাকলে 0dp)
                Box(modifier = Modifier.padding(bottom = if (hasReactions) 16.dp else 0.dp)) {
                    SwipeToReplyBox(
                        messageId = message.id,
                        isSelectionModeEnabled = isSelectionModeEnabled,
                        isDeleted = message.status.isDeleted,
                        onSwipeReply = onSwipeReply
                    ) { swipeModifier ->
                        Column(
                            modifier = swipeModifier
                                .widthIn(min = BubbleDimens.MinWidth, max = BubbleDimens.MaxWidth)
                                .pointerInput(message.id, isSelectionModeEnabled, message.status.isDeleted, "inner_tap") {
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
                }

                // ২. রিঅ্যাকশন আইকন (যদি রিঅ্যাকশন থাকে তবেই রেন্ডার হবে)
                if (hasReactions) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-8).dp) 
                    ) {
                        ReactionBar(message)
                    }
                }

                // ৩. লং-প্রেস পপআপ (শুধু সিঙ্গেল-সিলেকশনে দেখানো হয় যেন মাল্টি-সিলেক্টে
                // একাধিক পপআপ ওভারল্যাপ না হয়)
                if (isSelected && showReactionPicker) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-48).dp)
                    ) {
                        MessageReactionPopup(
                            haptic = haptic,
                            onReact = onReact
                        )
                    }
                }
            }
        }
    }
}
