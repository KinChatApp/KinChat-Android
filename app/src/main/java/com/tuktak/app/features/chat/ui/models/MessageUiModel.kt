package com.tuktak.app.features.chat.ui.models

import androidx.annotation.StringRes
import com.tuktak.app.domain.model.MessageType
import com.tuktak.app.domain.model.TickState
import com.tuktak.app.domain.model.ReactionType
import com.tuktak.app.domain.model.CallType
import com.tuktak.app.domain.model.CallStatus

data class MessageUiModel(
    val id: String,
    val content: String,
    val rawTimestamp: Long,
    val formattedTime: String,
    val type: MessageType,
    val isMe: Boolean,
    val isTopInGroup: Boolean,
    val showTail: Boolean,
    val status: MessageStatusUiState,
    val senderName: String,
    
    val media: MediaUiState? = null,
    val audio: AudioUiState? = null,
    val call: CallUiState? = null,
    val reply: ReplyPreviewUiState? = null,
    val reactions: List<ReactionUiState> = emptyList()
)

data class MessageStatusUiState(
    val isDeleted: Boolean = false,
    val isForwarded: Boolean = false,
    val isEdited: Boolean = false,
    val tickState: TickState = TickState.SENT
)

data class MediaUiState(
    val url: String,
    val fileName: String? = null,
    val rawSizeBytes: Long? = null,
    val formattedSize: String = ""
)

data class AudioUiState(
    val url: String,
    val durationSeconds: Int,
    val isPlaying: Boolean = false,
    val progress: Float = 0f
)

data class CallUiState(
    val type: CallType,
    val status: CallStatus,
    val durationSeconds: Int? = null,
    val isMissedOrFailed: Boolean,
    val isIncomingRinging: Boolean,
    @StringRes val statusTextRes: Int
)

data class ReplyPreviewUiState(
    val messageId: String,
    val senderName: String,
    val previewText: String
)

data class ReactionUiState(
    val type: ReactionType,
    val count: Int,
    val isSelectedByMe: Boolean
)
