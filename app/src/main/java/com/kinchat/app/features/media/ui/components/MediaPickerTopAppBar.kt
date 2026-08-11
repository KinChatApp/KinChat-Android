package com.kinchat.app.features.media.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPickerTopAppBar(
    albumName: String,
    hasPartialAccess: Boolean,
    hasSelectedItems: Boolean,
    onToggleAlbum: () -> Unit,
    onClose: () -> Unit,
    onAddMore: () -> Unit,
    onDone: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier
                    .clickable(onClick = onToggleAlbum)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = albumName,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Album"
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }
        },
        actions = {
            if (hasPartialAccess) {
                IconButton(onClick = onAddMore) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add more photos"
                    )
                }
            }
            if (hasSelectedItems) {
                IconButton(onClick = onDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done"
                    )
                }
            }
        }
    )
}
