package com.kinchat.app.features.chat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kinchat.app.domain.model.ChatMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageActionMenu(
    message: ChatMessage,
    isMe: Boolean,
    onClose: () -> Unit,
    onAction: (String, ChatMessage) -> Unit
) {
    // 🚀 ইমোজি পিকার দেখানোর স্টেট
    var showFullEmojiPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        if (showFullEmojiPicker) {
            // 🌟 ফুল ইমোজি পিকার ভিউ 🌟
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp) // গ্রিডের সর্বোচ্চ হাইট
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showFullEmojiPicker = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text("Select Emoji", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    // 🚀 আলাদা ফাইল থেকে ইমোজি লোড করা হচ্ছে
                    items(EmojiDataSource.allEmojis) { emoji ->
                        TextButton(
                            onClick = {
                                onAction("react_$emoji", message)
                                onClose()
                            },
                            modifier = Modifier.size(48.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
        } else {
            // 🌟 সাধারণ মেনু ভিউ 🌟
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // ডিফল্ট ৬টি রিঅ্যাকশন + প্লাস আইকন
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 🚀 আলাদা ফাইল থেকে ডিফল্ট ইমোজি লোড করা হচ্ছে
                    EmojiDataSource.defaultReactions.forEach { emoji ->
                        TextButton(
                            onClick = {
                                onAction("react_$emoji", message)
                                onClose()
                            },
                            modifier = Modifier.size(48.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                    
                    // 🚀 প্লাস আইকন (এতে ক্লিক করলে ফুল পিকার খুলবে)
                    IconButton(
                        onClick = { showFullEmojiPicker = true },
                        modifier = Modifier.size(48.dp).padding(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = "More Emojis",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                // রেগুলার অ্যাকশন মেনু
                if (message.type == "text") {
                    DropdownMenuItem(
                        text = { Text("Copy", fontWeight = FontWeight.Medium) },
                        onClick = { onAction("copy", message); onClose() },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Reply", fontWeight = FontWeight.Medium) },
                    onClick = { onAction("reply", message); onClose() },
                    leadingIcon = { Icon(Icons.Default.Reply, contentDescription = null) }
                )

                DropdownMenuItem(
                    text = { Text("Forward", fontWeight = FontWeight.Medium) },
                    onClick = { onAction("forward", message); onClose() },
                    leadingIcon = { Icon(Icons.Default.Forward, contentDescription = null) }
                )

                if (isMe && message.type == "text") {
                    DropdownMenuItem(
                        text = { Text("Edit", fontWeight = FontWeight.Medium) },
                        onClick = { onAction("edit", message); onClose() },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                if (isMe) {
                    DropdownMenuItem(
                        text = { Text("Delete for everyone", fontWeight = FontWeight.Medium) },
                        onClick = { onAction("delete_for_everyone", message); onClose() },
                        leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error)
                    )
                }

                DropdownMenuItem(
                    text = { Text("Delete for me", fontWeight = FontWeight.Medium) },
                    onClick = { onAction("delete_for_me", message); onClose() },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error)
                )

                if (!isMe) {
                    DropdownMenuItem(
                        text = { Text("Report", fontWeight = FontWeight.Medium) },
                        onClick = { onAction("report", message); onClose() },
                        leadingIcon = { Icon(Icons.Default.Report, contentDescription = null, tint = Color.Gray) }
                    )
                }
            }
        }
    }
}
