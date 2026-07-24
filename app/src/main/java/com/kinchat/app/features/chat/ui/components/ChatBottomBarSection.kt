package com.kinchat.app.features.chat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.kinchat.app.domain.model.ChatMessage

@Composable
fun ChatBottomBarSection(
    isSelectionMode: Boolean,
    canEdit: Boolean,
    canReply: Boolean,
    onEditRequested: () -> Unit,
    onReplyRequested: () -> Unit,
    onForwardRequested: () -> Unit,
    onCopyRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    onSendMessage: suspend (String) -> Boolean,
    updateTypingStatus: () -> Unit,
    partnerName: String,
    replyingToMessage: ChatMessage?,
    editingMessage: ChatMessage?,
    onCancelReply: () -> Unit
) {
    Column {
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            ChatSelectionBottomBar(
                canEdit = canEdit,
                canReply = canReply,
                onEditRequested = onEditRequested,
                onReplyRequested = onReplyRequested,
                onForwardRequested = onForwardRequested,
                onCopyRequested = onCopyRequested,
                onDeleteRequested = onDeleteRequested
            )
        }

        AnimatedVisibility(
            visible = !isSelectionMode,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            ChatInput(
                onSendMessage = onSendMessage,
                updateTypingStatus = { updateTypingStatus() },
                partnerName = partnerName,
                replyingToMessage = replyingToMessage,
                editingMessage = editingMessage,
                onCancelReply = onCancelReply
            )
        }
    }
}
