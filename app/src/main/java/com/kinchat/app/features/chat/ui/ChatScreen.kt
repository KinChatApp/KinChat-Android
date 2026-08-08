package com.kinchat.app.features.chat.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.kinchat.app.domain.model.ChatMessage
import com.kinchat.app.features.chat.ui.actions.MessageAction
import com.kinchat.app.features.chat.ui.components.ChatBottomBarSection
import com.kinchat.app.features.chat.ui.components.ChatDeleteBottomSheet
import com.kinchat.app.features.chat.ui.components.ChatMessageList
import com.kinchat.app.features.chat.ui.components.ChatNormalTopBar
import com.kinchat.app.features.chat.ui.components.ChatSelectionTopBar
import com.kinchat.app.features.chat.ui.components.SendErrorDialog
import com.kinchat.app.features.chat.ui.utils.MessagePermissions
import com.kinchat.app.features.chat.viewmodel.ChatViewModel
import com.kinchat.app.features.chat.viewmodel.PartnerUiState
import com.kinchat.app.features.chat.viewmodel.SendMessageResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onBack: () -> Unit,
    onNavigateToInfo: (String) -> Unit,
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

    val listState = rememberLazyListState()
    var replyingTo by remember { mutableStateOf<ChatMessage?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showDeleteSheet by remember { mutableStateOf(false) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var sendErrorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(chatId) { viewModel.initializeChat(chatId) }

    val displayName = (partnerState as? PartnerUiState.Success)?.name ?: "Loading..."
    val partnerId = (partnerState as? PartnerUiState.Success)?.id ?: ""
    val isSelectionMode = selectedMessages.isNotEmpty()

    val selectedMsgsList = remember(selectedMessages, messages) {
        messages.filter { it.id in selectedMessages }
    }

    val canEdit = remember(selectedMsgsList) {
        MessagePermissions.canEdit(selectedMsgsList, viewModel.currentUserId)
    }

    val canDeleteForEveryone = remember(selectedMsgsList) {
        MessagePermissions.canDeleteForEveryone(selectedMsgsList, viewModel.currentUserId)
    }

    val chatItems = remember(messages, viewModel.currentUserId, displayName) {
        ChatItemsBuilder.build(messages, viewModel.currentUserId, displayName)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSelectionMode) {
                ChatSelectionTopBar(
                    selectedCount = selectedMessages.size,
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
                    }
                )
            } else {
                ChatNormalTopBar(
                    displayName = displayName,
                    partnerId = partnerId,
                    isPartnerTyping = isPartnerTyping,
                    isPartnerOnline = isPartnerOnline,
                    isMenuExpanded = isMenuExpanded,
                    onMenuToggle = { isMenuExpanded = it },
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
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Copied Messages", textToCopy))
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
                    // 🚀 FIX: Passed media URI to ViewModel with reply support
                    val replyId = replyingTo?.id
                    replyingTo = null // Clear reply state after sending
                    viewModel.sendAttachment(uri, replyId) 
                },
                updateTypingStatus = {},
                partnerName = displayName,
                replyingToMessage = replyingTo,
                editingMessage = editingMessage,
                onCancelReply = { replyingTo = null; editingMessage = null }
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
                onMessageSelect = { viewModel.toggleSelection(it) },
                onMessageAction = { action ->
                    when (action) {
                        is MessageAction.Reply -> {
                            replyingTo = messages.find { it.id == action.message.id }
                            editingMessage = null
                        }
                        is MessageAction.React -> {
                            viewModel.addReaction(action.messageId, action.reaction)
                            viewModel.clearSelection()
                        }
                        else -> {}
                    }
                }
            )
        }
    }

    if (showDeleteSheet) {
        ChatDeleteBottomSheet(
            canDeleteForEveryone = canDeleteForEveryone,
            onDismiss = { showDeleteSheet = false },
            onDeleteForMe = { viewModel.deleteSelectedMessages("for_me"); showDeleteSheet = false },
            onDeleteForEveryone = { viewModel.deleteSelectedMessages("for_everyone"); showDeleteSheet = false }
        )
    }

    sendErrorText?.let { errorText ->
        SendErrorDialog(
            errorText = errorText,
            onDismiss = { sendErrorText = null }
        )
    }
}
