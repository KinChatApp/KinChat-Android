package com.tuktak.app.features.chat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tuktak.app.domain.model.ChatMessage
import com.tuktak.app.features.chat.ui.actions.MessageAction
import com.tuktak.app.features.chat.ui.components.ChatInput
import com.tuktak.app.features.chat.ui.components.MessageBubble
import com.tuktak.app.features.chat.ui.components.ChatHeaderInfo
import com.tuktak.app.features.chat.ui.components.ChatHeaderActions
import com.tuktak.app.features.chat.ui.components.ChatHeaderMenu
import com.tuktak.app.features.chat.ui.mapper.MessageUiMapper
import com.tuktak.app.features.chat.ui.models.MessageUiModel
import com.tuktak.app.features.chat.viewmodel.ChatViewModel
import com.tuktak.app.features.chat.viewmodel.PartnerUiState
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private sealed class ChatListItem {
    data class Msg(val uiModel: MessageUiModel) : ChatListItem()
    data class Header(val date: LocalDate, val label: String) : ChatListItem()
}

private fun localDateOf(instantStr: String?): LocalDate {
    if (instantStr == null) return LocalDate.now()
    return try {
        Instant.parse(instantStr).atZone(ZoneId.systemDefault()).toLocalDate()
    } catch (e: Exception) {
        LocalDate.now()
    }
}

private fun dateLabelFor(date: LocalDate): String {
    val today = LocalDate.now()
    return when {
        date == today -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        date.year == today.year -> date.format(DateTimeFormatter.ofPattern("MMMM d"))
        else -> date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onBack: () -> Unit,
    onNavigateToInfo: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isPartnerTyping by viewModel.isPartnerTyping.collectAsState()
    val isPartnerOnline by viewModel.isPartnerOnline.collectAsState()
    val partnerState by viewModel.partnerState.collectAsState()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var selectedMessageId by remember { mutableStateOf<String?>(null) }
    var replyingTo by remember { mutableStateOf<ChatMessage?>(null) }
    var isMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(chatId) {
        viewModel.initializeChat(chatId)
    }

    val displayName = when (partnerState) {
        is PartnerUiState.Loading -> "Loading..."
        is PartnerUiState.Success -> (partnerState as PartnerUiState.Success).name
        is PartnerUiState.Error -> "Unknown User"
    }

    // 🚀 FIX: Partner ID বের করে নেওয়া হলো
    val partnerId = (partnerState as? PartnerUiState.Success)?.id ?: ""

    val chatItems = remember(messages, viewModel.currentUserId, displayName) {
        val result = mutableListOf<ChatListItem>()
        var lastDate: LocalDate? = null

        messages.forEachIndexed { index, msg ->
            val msgDate = localDateOf(msg.createdAt)
            if (msgDate != lastDate) {
                result.add(ChatListItem.Header(msgDate, dateLabelFor(msgDate)))
                lastDate = msgDate
            }

            val prev = messages.getOrNull(index - 1)
            val next = messages.getOrNull(index + 1)
            val prevSameGroup = prev != null && prev.senderId == msg.senderId && localDateOf(prev.createdAt) == msgDate
            val nextSameGroup = next != null && next.senderId == msg.senderId && localDateOf(next.createdAt) == msgDate

            val uiModel = MessageUiMapper.mapToUiModel(
                entity = msg,
                currentUserId = viewModel.currentUserId,
                partnerName = displayName,
                isTopInGroup = !prevSameGroup,
                showTail = !nextSameGroup
            )

            result.add(ChatListItem.Msg(uiModel))
        }
        result.reversed()
    }

    val showScrollToBottom by remember { derivedStateOf { listState.firstVisibleItemIndex > 3 } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ChatHeaderInfo(
                        partnerAvatarUrl = null,
                        displayName = displayName,
                        isPartnerTyping = isPartnerTyping,
                        isPartnerOnline = isPartnerOnline,
                        onBack = onBack,
                        onGoToInfo = { 
                            // 🚀 FIX: chatId এর পরিবর্তে partnerId পাস করা হলো
                            if (partnerId.isNotEmpty()) {
                                onNavigateToInfo(partnerId) 
                            }
                        } 
                    )
                },
                actions = {
                    ChatHeaderActions(
                        isMessageSelected = selectedMessageId != null,
                        isSaved = false,
                        onToggleSave = { selectedMessageId?.let { viewModel.toggleSaveMessage(it) } },
                        onAudioCall = { },
                        onVideoCall = { }
                    ) {
                        ChatHeaderMenu(
                            isMenuExpanded = isMenuExpanded,
                            isMuted = false,
                            isBlocked = false,
                            onMenuToggle = { isMenuExpanded = it },
                            onGoToInfo = { 
                                // 🚀 FIX: chatId এর পরিবর্তে partnerId পাস করা হলো
                                if (partnerId.isNotEmpty()) {
                                    onNavigateToInfo(partnerId) 
                                }
                            }, 
                            onAction = { }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            ChatInput(
                onSendMessage = { text ->
                    viewModel.sendMessage(text, replyingTo?.id)
                    replyingTo = null
                },
                updateTypingStatus = { },
                partnerName = displayName,
                replyingToMessage = replyingTo,
                replyingToIsMe = replyingTo?.senderId == viewModel.currentUserId,
                onCancelReply = { replyingTo = null }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                reverseLayout = true
            ) {
                items(
                    items = chatItems,
                    key = { item ->
                        when (item) {
                            is ChatListItem.Msg -> item.uiModel.id
                            is ChatListItem.Header -> "header_${item.date}"
                        }
                    },
                    contentType = { item ->
                        when (item) {
                            is ChatListItem.Msg -> "message_bubble"
                            is ChatListItem.Header -> "date_header"
                        }
                    }
                ) { item ->
                    when (item) {
                        is ChatListItem.Header -> DateSeparator(item.label)
                        is ChatListItem.Msg -> {
                            MessageBubble(
                                message = item.uiModel,
                                isSelected = selectedMessageId == item.uiModel.id,
                                onSelect = { selectedMessageId = if (selectedMessageId == item.uiModel.id) null else item.uiModel.id },
                                onAction = { action ->
                                    when (action) {
                                        is MessageAction.Reply -> {
                                            replyingTo = messages.find { it.id == action.message.id }
                                        }
                                        is MessageAction.DeleteForMe -> viewModel.deleteMessage(action.message.id, "for_me")
                                        is MessageAction.DeleteForEveryone -> viewModel.deleteMessage(action.message.id, "for_everyone")
                                        is MessageAction.PlayAudio -> { /* TODO: Play audio logic */ }
                                        is MessageAction.PauseAudio -> { /* TODO: Pause audio logic */ }
                                        is MessageAction.JoinCall -> { /* TODO: Join Call logic */ }
                                        is MessageAction.DownloadMedia -> { /* TODO: Download via ViewModel/UseCase */ }
                                        is MessageAction.OpenMedia -> { /* TODO: Open media viewer */ }
                                    }
                                    selectedMessageId = null
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showScrollToBottom,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                SmallFloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll to latest")
                }
            }
        }
    }
}

@Composable
private fun DateSeparator(label: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center) {
        Box(
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
        }
    }
}
