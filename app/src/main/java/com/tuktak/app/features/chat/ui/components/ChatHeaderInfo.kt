package com.tuktak.app.features.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun ChatHeaderInfo(
    partnerAvatarUrl: String?,
    displayName: String, 
    isPartnerTyping: Boolean,
    isPartnerOnline: Boolean,
    onBack: () -> Unit,
    onGoToInfo: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val mutedForegroundColor = MaterialTheme.colorScheme.onSurfaceVariant
    val successOnlineColor = MaterialTheme.colorScheme.primary 

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = primaryColor
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onGoToInfo)
                .padding(end = 8.dp)
        ) {
            AsyncImage(
                model = partnerAvatarUrl ?: "https://api.dicebear.com/6.x/initials/svg?seed=$displayName",
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(surfaceVariantColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = onSurfaceColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = when {
                        isPartnerTyping -> "typing..."
                        isPartnerOnline -> "Online"
                        else -> "Offline"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 13.sp,
                    color = when {
                        isPartnerTyping -> primaryColor
                        isPartnerOnline -> successOnlineColor
                        else -> mutedForegroundColor
                    }
                )
            }
        }
    }
}
