package com.kinchat.app.features.chat.ui.components.bubble

import androidx.compose.runtime.Composable
import com.kinchat.app.domain.model.MessageType
import com.kinchat.app.features.chat.ui.actions.MessageAction
import com.kinchat.app.features.chat.ui.models.MessageUiModel

@Composable
fun BubbleContents(
    message: MessageUiModel,
    isSelectionModeEnabled: Boolean,
    onAction: (MessageAction) -> Unit,
    onSelect: () -> Unit
) {
    // 🚀 মেসেজ ডিলিট হলে সেটি ইমেজ/অডিও যাই হোক না কেন, টেক্সট হিসেবে দেখাবে
    if (message.status.isDeleted) {
        TextContent(
            message = message,
            isSelectionModeEnabled = isSelectionModeEnabled,
            onSelect = onSelect
        )
        return
    }

    when (message.type) {
        MessageType.TEXT -> {
            TextContent(
                message = message,
                isSelectionModeEnabled = isSelectionModeEnabled,
                onSelect = onSelect
            )
        }
        MessageType.IMAGE, MessageType.VIDEO -> {
            MediaContent(
                message = message,
                isSelectionModeEnabled = isSelectionModeEnabled,
                onSelect = onSelect,
                onMediaClick = {
                    message.media?.url?.let { onAction(MessageAction.OpenMedia(it, message.type)) }
                }
            )
        }
        MessageType.AUDIO -> {
            AudioContent(
                message = message,
                isSelectionModeEnabled = isSelectionModeEnabled,
                onSelect = onSelect,
                onPlayToggle = {
                    if (message.audio?.isPlaying == true) {
                        onAction(MessageAction.PauseAudio(message))
                    } else {
                        onAction(MessageAction.PlayAudio(message))
                    }
                }
            )
        }
        MessageType.DOCUMENT -> {
            DocumentContent(
                message = message,
                isSelectionModeEnabled = isSelectionModeEnabled,
                onSelect = onSelect,
                onDownload = {
                    message.media?.url?.let { onAction(MessageAction.DownloadMedia(it, message.type)) }
                }
            )
        }
        else -> {
            TextContent(
                message = message,
                isSelectionModeEnabled = isSelectionModeEnabled,
                onSelect = onSelect
            )
        }
    }
}
