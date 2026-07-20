package com.tuktak.app.features.chat.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun ChatHeaderActions(
    isMessageSelected: Boolean,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onAudioCall: () -> Unit,
    onVideoCall: () -> Unit,
    defaultActions: @Composable RowScope.() -> Unit
) {
    Row {
        if (isMessageSelected) {
            IconButton(onClick = onToggleSave) {
                Icon(
                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save Message",
                    tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // 🚀 আপডেট: অডিও এবং ভিডিও কলের আইকন যুক্ত করা হয়েছে
            IconButton(onClick = onVideoCall) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Video Call",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onAudioCall) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Audio Call",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            defaultActions() // এখানে Menu কম্পোনেন্ট কল হবে
        }
    }
}
