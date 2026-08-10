package com.kinchat.app.features.chat.ui.components.bubble.utils

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kinchat.app.features.chat.ui.components.bubble.BubbleDimens

/**
 * Utility to calculate the dynamic rounded corner shape of a message bubble.
 */
@Composable
fun rememberMessageBubbleShape(
    isMe: Boolean,
    isTopInGroup: Boolean,
    showTail: Boolean
): RoundedCornerShape {
    return remember(isMe, isTopInGroup, showTail) {
        RoundedCornerShape(
            topStart = if (!isMe && !isTopInGroup) BubbleDimens.TailRadius else BubbleDimens.FullRadius,
            topEnd = if (isMe && !isTopInGroup) BubbleDimens.TailRadius else BubbleDimens.FullRadius,
            bottomStart = if (!isMe && showTail) BubbleDimens.TailRadius else BubbleDimens.FullRadius,
            bottomEnd = if (isMe && showTail) BubbleDimens.TailRadius else BubbleDimens.FullRadius
        )
    }
}
