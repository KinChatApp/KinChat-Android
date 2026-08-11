package com.kinchat.app.features.chat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kinchat.app.features.chat.ui.ChatListItem
import com.kinchat.app.features.chat.ui.actions.MessageAction
import kotlinx.coroutines.launch

@Composable
fun ChatMessageList(
    chatItems: List<ChatListItem>,
    messagesCount: Int,
    listState: LazyListState,
    selectedMessages: Set<String>,
    isSelectionMode: Boolean,
    onMessageSelect: (String) -> Unit,
    onMessageAction: (MessageAction) -> Unit
) {
    val scope = rememberCoroutineScope()
    val showScrollToBottom by remember { derivedStateOf { listState.firstVisibleItemIndex > 3 } }

    // Only auto-scroll to the newest message when the user is already at (or
    // essentially at) the newest message. Never yank the list while the user is
    // reading older messages.
    val isNearNewest by remember { derivedStateOf { listState.firstVisibleItemIndex <= 1 } }

    LaunchedEffect(messagesCount) {
        if (messagesCount > 0 && isNearNewest) {
            listState.scrollToItem(0)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
    ) {
        items(
            items = chatItems,
            key = {
                when (it) {
                    is ChatListItem.Msg -> it.uiModel.id
                    is ChatListItem.Header -> "header_${it.date}"
                }
            }
        ) { item ->
            when (item) {
                is ChatListItem.Header -> DateSeparator(item.label)
                is ChatListItem.Msg -> {
                    MessageBubble(
                        message = item.uiModel,
                        isSelected = selectedMessages.contains(item.uiModel.id),
                        isSelectionModeEnabled = isSelectionMode,
                        showReactionPicker = selectedMessages.size == 1,
                        onSelect = { onMessageSelect(item.uiModel.id) },
                        onAction = onMessageAction
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = showScrollToBottom,
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.BottomEnd)
            .padding(16.dp),
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
