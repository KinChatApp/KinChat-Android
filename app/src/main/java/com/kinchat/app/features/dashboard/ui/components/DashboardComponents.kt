package com.kinchat.app.features.dashboard.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kinchat.app.core.utils.ChatFormatters
import com.kinchat.app.domain.model.Chat
import com.kinchat.app.domain.model.TickState
import com.valentinilk.shimmer.shimmer

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListItem(
    chat: Chat,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatAvatar(avatarUrl = chat.avatarUrl)
        Spacer(modifier = Modifier.width(16.dp))
        ChatContent(chat = chat, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ChatAvatar(avatarUrl: String?) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.matchParentSize().padding(8.dp)
            )
        }
    }
}

@Composable
private fun ChatContent(chat: Chat, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        ChatHeader(
            name = chat.name,
            timestamp = chat.timestamp,
            hasUnread = chat.unreadCount > 0
        )
        Spacer(modifier = Modifier.height(4.dp))
        ChatSubtitle(chat = chat)
    }
}

@Composable
private fun ChatHeader(name: String, timestamp: Long, hasUnread: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = ChatFormatters.formatChatTime(timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = if (hasUnread) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChatSubtitle(chat: Chat) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 🚀 FIX: FAILED স্টেট হ্যান্ডেল করা হয়েছে
            if (chat.isLastMessageFromMe && chat.tickState != null) {
                val (icon, tint) = when (chat.tickState) {
                    TickState.SENDING -> Icons.Default.Schedule to Color.Gray
                    TickState.SENT -> Icons.Default.Check to Color.Gray
                    TickState.DELIVERED -> Icons.Default.DoneAll to Color.Gray
                    TickState.READ -> Icons.Default.DoneAll to Color(0xFF34B7F1)
                    TickState.FAILED -> Icons.Default.ErrorOutline to MaterialTheme.colorScheme.error
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Message Status",
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = chat.lastMessage ?: "No messages yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (chat.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(20.dp)
                    .background(Color(0xFF22C55E), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chat.unreadCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ChatListSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shimmer(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.fillMaxWidth(0.5f).height(16.dp).background(Color.LightGray))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.8f).height(14.dp).background(Color.LightGray))
        }
    }
}

@Composable
fun AIChatFAB(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = Color(0xFF22C55E),
        contentColor = Color.White
    ) {
        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Chat")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatContextMenuBottomSheet(
    chat: Chat,
    onDismiss: () -> Unit,
    onPinClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    onArchiveClick: () -> Unit = {},
    onMuteClick: () -> Unit = {},
    onBlockClick: () -> Unit = {},
    onDeleteClick: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = chat.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Pin Chat") },
                leadingContent = { Icon(Icons.Default.PushPin, contentDescription = null) },
                modifier = Modifier.clickable { onPinClick() }
            )
            ListItem(
                headlineContent = { Text("Add to Favorites") },
                leadingContent = { Icon(Icons.Default.StarBorder, contentDescription = null) },
                modifier = Modifier.clickable { onFavoriteClick() }
            )
            ListItem(
                headlineContent = { Text("Archive Chat") },
                leadingContent = { Icon(Icons.Default.Archive, contentDescription = null) },
                modifier = Modifier.clickable { onArchiveClick() }
            )
            ListItem(
                headlineContent = { Text("Mute Notifications") },
                leadingContent = { Icon(Icons.Default.NotificationsOff, contentDescription = null) },
                modifier = Modifier.clickable { onMuteClick() }
            )
            ListItem(
                headlineContent = { Text("Block User") },
                leadingContent = { Icon(Icons.Default.Block, contentDescription = null) },
                modifier = Modifier.clickable { onBlockClick() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            ListItem(
                headlineContent = { Text("Delete Chat", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { onDeleteClick() }
            )
        }
    }
}
