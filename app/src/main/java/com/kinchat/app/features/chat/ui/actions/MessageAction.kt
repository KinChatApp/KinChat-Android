package com.kinchat.app.features.chat.ui.actions

import com.kinchat.app.domain.model.MessageType
import com.kinchat.app.features.chat.ui.models.MessageUiModel

sealed interface MessageAction {
    data class Reply(val message: MessageUiModel) : MessageAction
    data class PlayAudio(val message: MessageUiModel) : MessageAction
    data class PauseAudio(val message: MessageUiModel) : MessageAction
    data class DeleteForMe(val message: MessageUiModel) : MessageAction
    data class DeleteForEveryone(val message: MessageUiModel) : MessageAction
    data class JoinCall(val message: MessageUiModel) : MessageAction
    data class DownloadMedia(val url: String, val type: MessageType) : MessageAction
    data class OpenMedia(val url: String, val type: MessageType) : MessageAction
}
