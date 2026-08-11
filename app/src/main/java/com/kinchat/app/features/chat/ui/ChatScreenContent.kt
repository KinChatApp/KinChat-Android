package com.kinchat.app.features.chat.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.kinchat.app.domain.model.ChatMessage
import com.kinchat.app.features.chat.ui.actions.MessageAction
import com.kinchat.app.features.chat.ui.components.ChatBottomBarSection
import com.kinchat.app.features.chat.ui.components.ChatDeleteBottomSheet
import com.kinchat.app.features.chat.ui.components.ChatMessageList
import com.kinchat.app.features.chat.ui.components.ChatNormalTopBar
import com.kinchat.app.features.chat.ui.components.ChatSelectionTopBar
import com.kinchat.app.features.chat.ui.components.SendErrorDialog
import com.kinchat.app.features.chat.ui.utils.MessagePermissions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreenContent(
    messages: List<ChatMessage>,
    selectedMessages: Set<String>,
    selectedMsgsList: List<ChatMessage>,
    currentUserId: String,
    displayName: String,
    partnerId: String,
    isPartnerTyping: Boolean,
    isPartnerOnline: Boolean,
    replyingTo: ChatMessage?,
    editingMessage: ChatMessage?,
    showDeleteSheet: Boolean,
    isMenuExpanded: Boolean,
    sendErrorText: String?,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onNavigateToInfo: (String) -> Unit,
    onMenuToggle: (Boolean) -> Unit,
    onClearSelection: () -> Unit,
    onToggleSave: () -> Unit,
    onPinRequested: () -> Unit,
    onInfoRequested: () -> Unit,
    onEditRequested: () -> Unit,
    onReplyRequested: () -> Unit,
    onForwardRequested: () -> Unit,
    onCopyRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    onSendMessage: suspend (String) -> Boolean,
    onMediaSelected: (Uri) -> Unit,
    onCancelReply: () -> Unit,
    onDismissDeleteSheet: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
    onDismissSendError: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onAddReaction: (String, String) -> Unit,
    onReplyToMessage: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val isSelectionMode = selectedMessages.isNotEmpty()

    val canEdit = remember(selectedMsgsList) {
        MessagePermissions.canEdit(selectedMsgsList, currentUserId)
    }

    val canDeleteForEveryone = remember(selectedMsgsList) {
        MessagePermissions.canDeleteForEveryone(selectedMsgsList, currentUserId)
    }

    val chatItems = remember(messages, currentUserId, displayName) {
        ChatItemsBuilder.build(messages, currentUserId, displayName)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSelectionMode) {
                ChatSelectionTopBar(
                    selectedCount = selectedMessages.size,
                    onClearSelection = onClearSelection,
                    onToggleSave = onToggleSave,
                    onPinRequested = onPinRequested,
                    onInfoRequested = onInfoRequested
                )
            } else {
                ChatNormalTopBar(
                    displayName = displayName,
                    partnerId = partnerId,
                    isPartnerTyping = isPartnerTyping,
                    isPartnerOnline = isPartnerOnline,
                    isMenuExpanded = isMenuExpanded,
                    onMenuToggle = onMenuToggle,
                    onBack = onBack,
                    onNavigateToInfo = onNavigateToInfo
                )
            }
        },
        bottomBar = {
            ChatBottomBarSection(
                isSelectionMode = isSelectionMode,
                canEdit = canEdit,
                canReply = selectedMessages.size == 1,
                onEditRequested = onEditRequested,
                onReplyRequested = onReplyRequested,
                onForwardRequested = onForwardRequested,
                onCopyRequested = onCopyRequested,
                onDeleteRequested = onDeleteRequested,
                onSendMessage = onSendMessage,
                onMediaSelected = onMediaSelected,
                updateTypingStatus = {},
                partnerName = displayName,
                replyingToMessage = replyingTo,
                editingMessage = editingMessage,
                onCancelReply = onCancelReply
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            ChatMessageList(
                chatItems = chatItems,
                messagesCount = messages.size,
                listState = listState,
                selectedMessages = selectedMessages,
                isSelectionMode = isSelectionMode,
                onMessageSelect = onToggleSelection,
                onMessageAction = { action ->
                    when (action) {
                        is MessageAction.Reply -> onReplyToMessage(action.message.id)
                        is MessageAction.React -> onAddReaction(action.messageId, action.reaction)
                        else -> {}
                    }
                }
            )
        }
    }

    if (showDeleteSheet) {
        ChatDeleteBottomSheet(
            canDeleteForEveryone = canDeleteForEveryone,
            onDismiss = onDismissDeleteSheet,
            onDeleteForMe = onDeleteForMe,
            onDeleteForEveryone = onDeleteForEveryone
        )
    }

    sendErrorText?.let { errorText ->
        SendErrorDialog(
            errorText = errorText,
            onDismiss = onDismissSendError
        )
    }
}
