package com.kinchat.app.features.chat.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinchat.app.domain.model.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatInput(
    onSendMessage: suspend (String) -> Boolean,
    onMediaSelected: (Uri) -> Unit = {}, // 🚀 নতুন: মিডিয়া সিলেক্ট করার কলব্যাক
    updateTypingStatus: (Boolean) -> Unit,
    partnerName: String,
    replyingToMessage: ChatMessage? = null,
    editingMessage: ChatMessage? = null,
    replyingToIsMe: Boolean = false,
    onCancelReply: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }

    // 🚀 ফাইল/ডকুমেন্ট পিকার লঞ্চার (সব ধরনের ফাইলের জন্য)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onMediaSelected(it) }
    }

    // 🚀 ইমেজ পিকার লঞ্চার (ক্যামেরা আইকনের জন্য শুধু ছবি/ভিডিও)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onMediaSelected(it) }
    }

    LaunchedEffect(editingMessage) {
        if (editingMessage != null) {
            text = editingMessage.content ?: ""
        }
    }

    LaunchedEffect(text) {
        if (text.isNotEmpty()) {
            updateTypingStatus(true)
            delay(2000)
            updateTypingStatus(false)
        } else {
            updateTypingStatus(false)
        }
    }

    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Column {
            AnimatedVisibility(
                visible = replyingToMessage != null || editingMessage != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (editingMessage != null) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (editingMessage != null) "Edit Message" else if (replyingToIsMe) "Replying to yourself" else "Replying to $partnerName",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = editingMessage?.content ?: replyingToMessage?.content ?: "[Media]",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = {
                        onCancelReply()
                        if (editingMessage != null) text = ""
                    }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 🚀 Attach File Button - ক্লিক করলে ফাইল ম্যানেজার ওপেন হবে
                    IconButton(onClick = { filePickerLauncher.launch("*/*") }) { 
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = MaterialTheme.colorScheme.onSurfaceVariant) 
                    }
                    
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message $partnerName") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                    )
                    
                    // 🚀 Camera/Image Button - ক্লিক করলে গ্যালারি ওপেন হবে
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) { 
                        Icon(Icons.Default.CameraAlt, contentDescription = "Gallery", tint = MaterialTheme.colorScheme.onSurfaceVariant) 
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                val isTextPresent = text.isNotBlank()
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            if (isTextPresent) {
                                coroutineScope.launch {
                                    val success = onSendMessage(text.trim())
                                    if (success) {
                                        text = ""
                                        onCancelReply() 
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isTextPresent,
                        transitionSpec = { (fadeIn(tween(150)) + scaleIn(initialScale = 0.7f)) togetherWith (fadeOut(tween(100)) + scaleOut(targetScale = 0.7f)) },
                        label = "sendMicSwap"
                    ) { hasText ->
                        Icon(
                            imageVector = if (hasText) Icons.Default.Send else Icons.Default.Mic,
                            contentDescription = if (hasText) "Send" else "Record voice message",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
