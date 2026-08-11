package com.kinchat.app.features.chat.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kinchat.app.domain.model.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatInput(
    onSendMessage: suspend (String) -> Boolean,
    onMediaSelected: (Uri) -> Unit = {},
    updateTypingStatus: (Boolean) -> Unit,
    partnerName: String,
    replyingToMessage: ChatMessage? = null,
    editingMessage: ChatMessage? = null,
    replyingToIsMe: Boolean = false,
    onCancelReply: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onMediaSelected(it) }
    }

    // এখানে PickVisualMedia এর জায়গায় PickMultipleVisualMedia ব্যবহার করা হয়েছে
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        // একাধিক ফাইল সিলেক্ট হলে লুপের মাধ্যমে একটা একটা করে সেন্ড করা হবে
        uris.forEach { uri ->
            onMediaSelected(uri)
        }
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

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column {
            ReplyEditBar(
                replyingToMessage = replyingToMessage,
                editingMessage = editingMessage,
                replyingToIsMe = replyingToIsMe,
                partnerName = partnerName,
                onCancel = {
                    onCancelReply()
                    if (editingMessage != null) text = ""
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                ChatInputTextField(
                    text = text,
                    onTextChange = { text = it },
                    partnerName = partnerName,
                    onAttachClick = { filePickerLauncher.launch("*/*") },
                    onCameraClick = { 
                        mediaPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        ) 
                    },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                val isTextPresent = text.isNotBlank()

                SendMicButton(
                    isTextPresent = isTextPresent,
                    onClick = {
                        if (isTextPresent) {
                            coroutineScope.launch {
                                val success = onSendMessage(text.trim())
                                if (success) {
                                    text = ""
                                    onCancelReply()
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
