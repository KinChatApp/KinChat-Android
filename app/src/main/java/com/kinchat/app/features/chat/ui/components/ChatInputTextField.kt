package com.kinchat.app.features.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.kinchat.app.core.designsystem.LocalExtendedColors

@Composable
fun ChatInputTextField(
    text: String,
    onTextChange: (String) -> Unit,
    partnerName: String,
    onAttachClick: () -> Unit,
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(LocalExtendedColors.current.inputFieldBackground)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAttachClick) {
            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = "Attach",
                tint = LocalExtendedColors.current.attachmentIcon
            )
        }

        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Message $partnerName", color = LocalExtendedColors.current.inputPlaceholder) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            maxLines = 5,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
        )

        IconButton(onClick = onCameraClick) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Gallery",
                tint = LocalExtendedColors.current.attachmentIcon
            )
        }
    }
}
