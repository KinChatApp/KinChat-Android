package com.kinchat.app.features.chat.ui

import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.kinchat.app.domain.model.ChatMessage
import com.kinchat.app.features.chat.ui.utils.ChatClipboardHelper
import com.kinchat.app.features.chat.viewmodel.ChatViewModel
import com.kinchat.app.features.chat.viewmodel.PartnerUiState
import com.kinchat.app.features.chat.viewmodel.SendMessageResult
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    chatId: String,
    returnedMediaUris: List<Uri>? = null,
    returnedCaption: String? = null,
    returnedReplyId: String? = null,
    onMediaProcessed: () -> Unit = {},
    onBack: () -> Unit,
    onNavigateToInfo: (String) -> Unit,
    onNavigateToMediaPicker: (String?) -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val messages by viewModel.messages.collectAsState()
    val isPartnerTyping by viewModel.isPartnerTyping.collectAsState()
    val isPartnerOnline by viewModel.isPartnerOnline.collectAsState()
    val partnerState by viewModel.partnerState.collectAsState()
    val selectedMessages by viewModel.selectedMessages.collectAsState()

    var replyingTo by remember { mutableStateOf<ChatMessage?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showDeleteSheet by remember { mutableStateOf(false) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var sendErrorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(chatId) { viewModel.initializeChat(chatId) }

    // Handle returned media from Custom Media Picker
    LaunchedEffect(returnedMediaUris) {
        if (!returnedMediaUris.isNullOrEmpty()) {
            if (!returnedCaption.isNullOrBlank()) {
                viewModel.sendMessage(returnedCaption, returnedReplyId)
            }
            returnedMediaUris.forEach { uri ->
                viewModel.sendAttachment(uri, returnedReplyId)
            }
            onMediaProcessed()
        }
    }

    val displayName = (partnerState as? PartnerUiState.Success)?.name ?: "Loading..."
    val partnerId = (partnerState as? PartnerUiState.Success)?.id ?: ""

    val selectedMsgsList = remember(selectedMessages, messages) {
        messages.filter { it.id in selectedMessages }
    }

    ChatScreenContent(
        messages = messages,
        selectedMessages = selectedMessages,
        selectedMsgsList = selectedMsgsList,
        currentUserId = viewModel.currentUserId,
        displayName = displayName,
        partnerId = partnerId,
        isPartnerTyping = isPartnerTyping,
        isPartnerOnline = isPartnerOnline,
        replyingTo = replyingTo,
        editingMessage = editingMessage,
        showDeleteSheet = showDeleteSheet,
        isMenuExpanded = isMenuExpanded,
        sendErrorText = sendErrorText,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onNavigateToInfo = onNavigateToInfo,
        onMenuToggle = { isMenuExpanded = it },
        onClearSelection = { viewModel.clearSelection() },
        onToggleSave = {
            viewModel.toggleSaveMessage(selectedMessages.first())
            scope.launch { snackbarHostState.showSnackbar("Saved to Bookmarks") }
            viewModel.clearSelection()
        },
        onPinRequested = {
            scope.launch { snackbarHostState.showSnackbar("Pin feature coming soon") }
            viewModel.clearSelection()
        },
        onInfoRequested = {
            scope.launch { snackbarHostState.showSnackbar("Message Info coming soon") }
            viewModel.clearSelection()
        },
        onEditRequested = {
            editingMessage = selectedMsgsList.first()
            replyingTo = null
            viewModel.clearSelection()
        },
        onReplyRequested = {
            replyingTo = selectedMsgsList.first()
            editingMessage = null
            viewModel.clearSelection()
        },
        onForwardRequested = {
            scope.launch { snackbarHostState.showSnackbar("Forwarding not implemented yet") }
            viewModel.clearSelection()
        },
        onCopyRequested = {
            val textToCopy = selectedMsgsList.joinToString("\n") { it.content ?: "" }
            ChatClipboardHelper.copyToClipboard(context, textToCopy)
            scope.launch { snackbarHostState.showSnackbar("Messages Copied") }
            viewModel.clearSelection()
        },
        onDeleteRequested = { showDeleteSheet = true },
        onSendMessage = { text ->
            if (editingMessage != null) {
                viewModel.editMessage(editingMessage!!.id, text)
                editingMessage = null
                true
            } else {
                val replyId = replyingTo?.id
                replyingTo = null
                when (val result = viewModel.sendMessage(text, replyId)) {
                    is SendMessageResult.Success -> true
                    is SendMessageResult.Failure -> {
                        sendErrorText = result.reason
                        false
                    }
                }
            }
        },
        onMediaSelected = { uri ->
            val currentReplyId = replyingTo?.id
            replyingTo = null
            
            // Magic URI চেক করা হচ্ছে
            if (uri.toString() == "kinchat://open_media_picker") {
                onNavigateToMediaPicker(currentReplyId) // কাস্টম পিকার ওপেন হবে
            } else {
                viewModel.sendAttachment(uri, currentReplyId) // ডকুমেন্ট ফাইল সেন্ড হবে
            }
        },
        onCancelReply = {
            replyingTo = null
            editingMessage = null
        },
        onDismissDeleteSheet = { showDeleteSheet = false },
        onDeleteForMe = {
            viewModel.deleteSelectedMessages("for_me")
            showDeleteSheet = false
        },
        onDeleteForEveryone = {
            viewModel.deleteSelectedMessages("for_everyone")
            showDeleteSheet = false
        },
        onDismissSendError = { sendErrorText = null },
        onToggleSelection = { viewModel.toggleSelection(it) },
        onAddReaction = { messageId, reaction ->
            viewModel.addReaction(messageId, reaction)
            viewModel.clearSelection()
        },
        onReplyToMessage = { messageId ->
            replyingTo = messages.find { it.id == messageId }
            editingMessage = null
        }
    )
}
