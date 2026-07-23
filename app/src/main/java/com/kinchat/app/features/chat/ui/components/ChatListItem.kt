package com.kinchat.app.features.chat.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kinchat.app.domain.model.ChatThread
import com.kinchat.app.ui.theme.BrandPrimary
import com.kinchat.app.ui.theme.ForegroundLight
import com.kinchat.app.ui.theme.MutedForegroundLight
import com.kinchat.app.ui.theme.Success

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListItem(
    chat: ChatThread,
    contactName: String? = null,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val displayName = contactName ?: chat.name
    val hasUnread = chat.unreadCount > 0
    val lastMsg = chat.lastMessage ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        AsyncImage(
            model = chat.avatarUrl ?: "https://api.dicebear.com/6.x/initials/svg?seed=$displayName",
            contentDescription = displayName,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ForegroundLight,
                        fontSize = 16.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = chat.timestamp ?: "",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                        color = if (hasUnread) BrandPrimary else MutedForegroundLight,
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Last Message & Icons
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (lastMsg.startsWith("Photo")) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = "Photo",
                            tint = MutedForegroundLight,
                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                        )
                    } else if (lastMsg.contains(Regex("^\\d+:\\d+"))) {
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = "Voice Message",
                            tint = Success,
                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                        )
                    } else if (lastMsg.startsWith("Document")) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = "Document",
                            tint = MutedForegroundLight,
                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                        )
                    }

                    // Group Sender Highlight (e.g. "Mom: ")
                    val annotatedMsg = buildAnnotatedString {
                        val colonIndex = lastMsg.indexOf(":")
                        if (colonIndex != -1 && colonIndex < 15 && !lastMsg.contains(Regex("^\\d+:\\d+"))) {
                            withStyle(style = SpanStyle(color = BrandPrimary, fontWeight = FontWeight.Medium)) {
                                append(lastMsg.substring(0, colonIndex + 1))
                            }
                            append(lastMsg.substring(colonIndex + 1))
                        } else {
                            append(lastMsg)
                        }
                    }

                    Text(
                        text = annotatedMsg,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (hasUnread) ForegroundLight else MutedForegroundLight
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                if (hasUnread) {
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 22.dp, minHeight = 22.dp)
                            .background(BrandPrimary, CircleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Filled.DoneAll,
                        contentDescription = "Read",
                        tint = Success,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
