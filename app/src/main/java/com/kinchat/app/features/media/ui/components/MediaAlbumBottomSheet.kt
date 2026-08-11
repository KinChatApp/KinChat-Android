package com.kinchat.app.features.media.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MediaAlbumBottomSheet(
    albums: List<T>,
    currentAlbumId: Any?,
    getAlbumId: (T) -> Any,
    getAlbumName: (T) -> String,
    getAlbumMediaCount: (T) -> Int,
    getAlbumCoverUri: (T) -> Any?,
    onAlbumSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    imageLoader: ImageLoader
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(modifier = Modifier.navigationBarsPadding()) {
            items(albums, key = { getAlbumId(it) }) { album ->
                val isCurrent = getAlbumId(album) == currentAlbumId
                ListItem(
                    headlineContent = {
                        Text(
                            text = getAlbumName(album),
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    supportingContent = { Text("${getAlbumMediaCount(album)} items") },
                    leadingContent = {
                        AsyncImage(
                            model = getAlbumCoverUri(album),
                            imageLoader = imageLoader,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    },
                    trailingContent = if (isCurrent) {
                        { Icon(Icons.Default.Check, contentDescription = "Current album") }
                    } else null,
                    colors = ListItemDefaults.colors(
                        containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ),
                    modifier = Modifier.clickable { onAlbumSelected(album) }
                )
            }
        }
    }
}
