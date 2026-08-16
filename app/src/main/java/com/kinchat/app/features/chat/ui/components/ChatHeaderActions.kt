package com.kinchat.app.features.chat.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton
import com.zegocloud.uikit.service.defines.ZegoUIKitUser
import com.kinchat.app.core.logging.AppLogger

@Composable
fun ChatHeaderActions(
    isMessageSelected: Boolean,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onAudioCall: () -> Unit = {}, 
    onVideoCall: () -> Unit = {},
    targetUserId: String = "", 
    targetUserName: String = "User",
    defaultActions: @Composable RowScope.() -> Unit
) {
    val cleanUserId = targetUserId.replace("-", "").trim()

    LaunchedEffect(cleanUserId) {
        if (cleanUserId.isNotBlank()) {
            AppLogger.d("ZegoCloud", "✅ Composing Call Buttons for ID: '$cleanUserId'")
        }
    }

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
            // 🚀 ম্যাজিক ট্রিক: key ব্যবহার করে কম্পোজকে বাধ্য করা হলো নতুন আইডি পেলে বাটন রিক্রিয়েট করতে
            key(cleanUserId) {
                if (cleanUserId.isNotBlank()) {
                    // Video Call Button
                    AndroidView(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp),
                        factory = { context ->
                            ZegoSendCallInvitationButton(context).apply {
                                setIsVideoCall(true) 
                                resourceID = "zego_uikit_call" 
                                setInvitees(listOf(ZegoUIKitUser(cleanUserId, targetUserName)))
                            }
                        }
                    )

                    // Audio Call Button
                    AndroidView(
                        modifier = Modifier
                            .padding(end = 8.dp)
                        .size(40.dp),
                        factory = { context ->
                            ZegoSendCallInvitationButton(context).apply {
                                setIsVideoCall(false) 
                                resourceID = "zego_uikit_call" 
                                setInvitees(listOf(ZegoUIKitUser(cleanUserId, targetUserName)))
                            }
                        }
                    )
                } else {
                    // আইডি লোড হওয়ার আগ পর্যন্ত নরমাল আইকন দেখাবে (যাতে অ্যাপ ক্র্যাশ না করে)
                    IconButton(onClick = {}, enabled = false) {
                        Icon(Icons.Default.Videocam, contentDescription = "Loading", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                    IconButton(onClick = {}, enabled = false) {
                        Icon(Icons.Default.Call, contentDescription = "Loading", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }
            
            defaultActions() 
        }
    }
}
