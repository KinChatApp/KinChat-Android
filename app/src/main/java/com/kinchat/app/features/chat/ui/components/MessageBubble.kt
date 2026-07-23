package com.kinchat.app.features.chat.ui.components

import androidx.compose.runtime.*
import com.kinchat.app.features.chat.ui.actions.MessageAction
import com.kinchat.app.features.chat.ui.models.MessageUiModel
import com.kinchat.app.features.chat.ui.components.bubble.*

@Composable
fun MessageBubble(
    message: MessageUiModel,
    isSelected: Boolean,
    onSelect: (MessageUiModel?) -> Unit,
    onAction: (MessageAction) -> Unit
) {
    if (message.call != null) {
        CallBubble(message = message, isSelected = isSelected, onSelect = onSelect, onJoinCall = { onAction(MessageAction.JoinCall(message)) })
    } else {
        MessageBubbleContainer(
            message = message,
            isSelected = isSelected,
            onSelect = onSelect,
            onSwipeReply = { if (!message.status.isDeleted) onAction(MessageAction.Reply(message)) }
        ) {
            BubbleContents(message = message, onAction = onAction)
        }
    }
}
