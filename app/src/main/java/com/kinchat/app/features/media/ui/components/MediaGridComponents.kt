package com.kinchat.app.features.media.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kinchat.app.features.media.domain.model.MediaItem
import com.kinchat.app.features.media.domain.model.MediaType
import com.kinchat.app.features.media.ui.utils.formatDuration

@Composable
fun CameraTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CameraAlt, contentDescription = "Take a photo")
            Spacer(modifier = Modifier.height(4.dp))
            Text("Camera", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun MediaGridItem(
    item: MediaItem,
    isSelected: Boolean,
    selectionNumber: Int?,
    onClick: () -> Unit,
    imageLoader: ImageLoader
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RectangleShape)
            .clickable { onClick() }
            .semantics {
                contentDescription = if (item.type == MediaType.VIDEO) {
                    "Video, duration ${formatDuration(item.durationMs ?: 0)}"
                } else {
                    "Photo"
                }
                if (isSelected) this.selected = true
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .crossfade(true)
                .build(),
            imageLoader = imageLoader,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (item.type == MediaType.VIDEO) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(2.dp))
                Text(formatDuration(item.durationMs ?: 0), color = MaterialTheme.colorScheme.surface, style = MaterialTheme.typography.labelSmall)
            }
        }

        if (isSelected) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)))
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    selectionNumber.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.2f), CircleShape)
            )
        }
    }
}
