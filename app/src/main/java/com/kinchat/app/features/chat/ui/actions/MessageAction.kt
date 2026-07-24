package com.kinchat.app.features.chat.ui.actions

import com.kinchat.app.domain.model.MessageType
import com.kinchat.app.features.chat.ui.models.MessageUiModel

sealed interface MessageAction {
    data class ToggleSelection(val messageId: String) : MessageAction
    data class React(val messageId: String, val reaction: String) : MessageAction
    data class Copy(val text: String) : MessageAction
    data class Forward(val messageIds: Set<String>) : MessageAction
    data class Bookmark(val messageId: String) : MessageAction
    data class Reply(val message: MessageUiModel) : MessageAction
    data class PlayAudio(val message: MessageUiModel) : MessageAction
    data class PauseAudio(val message: MessageUiModel) : MessageAction
    data class JoinCall(val message: MessageUiModel) : MessageAction
    data class DownloadMedia(val url: String, val type: MessageType) : MessageAction
    data class OpenMedia(val url: String, val type: MessageType) : MessageAction
}
