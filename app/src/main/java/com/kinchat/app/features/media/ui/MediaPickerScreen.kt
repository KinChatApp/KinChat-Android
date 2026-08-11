package com.kinchat.app.features.media.ui

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kinchat.app.features.media.domain.model.MediaAlbum
import com.kinchat.app.features.media.domain.model.MediaItem
import com.kinchat.app.features.media.domain.model.MediaType
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPickerScreen(
    onDismiss: () -> Unit,
    onMediaSelected: (List<Uri>, String?) -> Unit,
    viewModel: MediaPickerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var captionText by remember { mutableStateOf("") }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions.entries.any { it.value } // True if at least partial access
        hasPermission = granted
        if (granted) viewModel.loadMedia()
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraUri != null) {
            onMediaSelected(listOf(cameraUri!!), null)
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissionsToRequest)
    }

    if (!hasPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Storage Permission Required", style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.clickable { viewModel.toggleAlbumSheet(true) }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(state.currentAlbum?.name ?: "Recent", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Album")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                },
                actions = {
                    if (state.selectedItems.isNotEmpty()) {
                        IconButton(onClick = { showPreview = true }) { Icon(Icons.Default.Check, contentDescription = "Done") }
                    }
                }
            )

            // Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    Box(
                        modifier = Modifier.aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceVariant).clickable {
                            val uri = createCameraFileUri(context)
                            cameraUri = uri
                            cameraLauncher.launch(uri)
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Camera")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Camera", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                items(state.mediaItems, key = { it.id }) { item ->
                    val selectedIndex = state.selectedItems.indexOfFirst { it.id == item.id }
                    MediaGridItem(
                        item = item,
                        isSelected = selectedIndex != -1,
                        selectionNumber = if (selectedIndex != -1) selectedIndex + 1 else null,
                        onClick = { viewModel.toggleSelection(item) }
                    )
                }
            }

            // Bottom Bar
            if (state.selectedItems.isNotEmpty()) {
                Surface(tonalElevation = 4.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${state.selectedItems.size} selected", style = MaterialTheme.typography.titleMedium)
                        Button(onClick = { showPreview = true }) {
                            Text("Next")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Send, contentDescription = "Next", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        if (state.isAlbumSheetVisible) {
            ModalBottomSheet(onDismissRequest = { viewModel.toggleAlbumSheet(false) }) {
                LazyColumn {
                    items(state.albums) { album ->
                        ListItem(
                            headlineContent = { Text(album.name) },
                            supportingContent = { Text("${album.mediaCount} items") },
                            leadingContent = {
                                AsyncImage(
                                    model = album.coverUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                                )
                            },
                            modifier = Modifier.clickable { viewModel.selectAlbum(album) }
                        )
                    }
                }
            }
        }

        // Preview & Caption Overlay
        AnimatedVisibility(
            visible = showPreview,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = { Text("Preview", color = Color.White) },
                        navigationIcon = {
                            IconButton(onClick = { showPreview = false }) { Icon(Icons.Default.Close, contentDescription = "Back", tint = Color.White) }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                    
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = state.selectedItems.lastOrNull()?.uri,
                            contentDescription = "Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Surface(color = Color.DarkGray, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp).navigationBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = captionText,
                                onValueChange = { captionText = it },
                                placeholder = { Text("Add a caption...", color = Color.LightGray) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            FloatingActionButton(
                                onClick = { onMediaSelected(state.selectedItems.map { it.uri }, captionText.takeIf { it.isNotBlank() }) },
                                containerColor = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaGridItem(item: MediaItem, isSelected: Boolean, selectionNumber: Int?, onClick: () -> Unit) {
    Box(
        modifier = Modifier.aspectRatio(1f).clickable { onClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        if (item.type == MediaType.VIDEO) {
            Row(
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Video", tint = Color.White, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(2.dp))
                Text(formatDuration(item.durationMs ?: 0), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }

        if (isSelected) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(selectionNumber.toString(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        } else {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).background(Color.Black.copy(alpha = 0.2f), CircleShape))
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun createCameraFileUri(context: Context): Uri {
    val folder = File(context.cacheDir, "camera_images").apply { mkdirs() }
    val file = File(folder, "IMG_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
