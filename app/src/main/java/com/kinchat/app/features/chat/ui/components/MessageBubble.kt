package com.kinchat.app.features.chat.ui.components

import androidx.compose.runtime.*
import com.kinchat.app.features.chat.ui.actions.MessageAction
import com.kinchat.app.features.chat.ui.models.MessageUiModel
import com.kinchat.app.features.chat.ui.components.bubble.*

@Composable
fun MessageBubble(
    message: MessageUiModel,
    isSelected: Boolean,
    isSelectionModeEnabled: Boolean = false,
    showReactionPicker: Boolean = false,
    onSelect: () -> Unit,
    onAction: (MessageAction) -> Unit
) {
    if (message.call != null) {
        CallBubble(
            message = message,
            isSelected = isSelected,
            onSelect = { onSelect() },
            onJoinCall = { onAction(MessageAction.JoinCall(message)) }
        )
    } else {
        MessageBubbleContainer(
            message = message,
            isSelected = isSelected,
            isSelectionModeEnabled = isSelectionModeEnabled,
            showReactionPicker = showReactionPicker,
            onSelect = onSelect,
            onSwipeReply = { if (!message.status.isDeleted) onAction(MessageAction.Reply(message)) },
            onReact = { reaction -> if (!message.status.isDeleted) onAction(MessageAction.React(message.id, reaction)) }
        ) {
            BubbleContents(
                message = message,
                isSelectionModeEnabled = isSelectionModeEnabled,
                onAction = onAction,
                onSelect = onSelect // 🚀 অন-সিলেক্ট পাস করা হলো যেন টেক্সট থেকে ইভেন্ট পাঠানো যায়
            )
        }
    }
}
