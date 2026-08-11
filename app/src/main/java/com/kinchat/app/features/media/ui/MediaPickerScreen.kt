@file:OptIn(ExperimentalMaterial3Api::class)

package com.kinchat.app.features.media.ui

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import com.kinchat.app.features.media.ui.components.*
import com.kinchat.app.features.media.ui.utils.*

@Composable
fun MediaPickerScreen(
    onDismiss: () -> Unit,
    onMediaSelected: (List<Uri>, String?) -> Unit,
    viewModel: MediaPickerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val mediaPermissions = remember { requiredMediaPermissions() }

    var hasPermission by remember { mutableStateOf(hasMediaPermission(context)) }
    var hasPartialAccess by remember { mutableStateOf(isPartialMediaAccess(context)) }
    var showPermissionSettings by remember { mutableStateOf(false) }

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions.entries.any { it.value }
        hasPermission = granted
        hasPartialAccess = granted && isPartialMediaAccess(context)
        if (granted) {
            viewModel.loadMedia()
        } else {
            showPermissionSettings = isPermanentlyDenied(context, mediaPermissions)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            viewModel.state.value.cameraUri?.let { onMediaSelected(listOf(it), null) }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCamera(context, viewModel, cameraLauncher)
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (hasPermission) {
            viewModel.loadMedia()
        } else {
            permissionLauncher.launch(mediaPermissions)
        }
    }

    LaunchedEffect(state.showSelectionLimitMessage) {
        if (state.showSelectionLimitMessage) {
            Toast.makeText(context, "Selection limit reached", Toast.LENGTH_SHORT).show()
            viewModel.resetSelectionLimitMessage()
        }
    }

    if (!hasPermission) {
        PermissionRequiredContent(
            showOpenSettings = showPermissionSettings,
            onRequestAgain = { permissionLauncher.launch(mediaPermissions) },
            onOpenSettings = { openAppSettings(context) }
        )
        return
    }

    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState) {
        snapshotFlow {
            val info = gridState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to info.totalItemsCount
        }.collect { (lastIndex, totalCount) ->
            val current = viewModel.state.value
            if (lastIndex >= 0 && totalCount > 0 && lastIndex >= totalCount - 8 &&
                current.hasMore && !current.isLoadingMore && !current.isLoading
            ) {
                viewModel.loadMore()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            MediaPickerTopAppBar(
                albumName = state.currentAlbum?.name ?: "Recent",
                hasPartialAccess = hasPartialAccess,
                hasSelectedItems = state.selectedItems.isNotEmpty(),
                onToggleAlbum = { viewModel.toggleAlbumSheet(true) },
                onClose = onDismiss,
                onAddMore = {
                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))
                },
                onDone = { viewModel.setShowPreview(true) }
            )

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> {
                    ErrorContent(modifier = Modifier.fillMaxWidth().weight(1f)) { viewModel.retry() }
                }
                state.mediaItems.isEmpty() -> {
                    EmptyContent(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        hasPartialAccess = hasPartialAccess,
                        onAddMore = {
                            if (hasPartialAccess) {
                                permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))
                            }
                        }
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        state = gridState,
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        item {
                            CameraTile(
                                onClick = {
                                    if (hasCameraPermission(context)) launchCamera(context, viewModel, cameraLauncher)
                                    else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            )
                        }

                        items(state.mediaItems, key = { it.id }) { item ->
                            val selectedIndex = state.selectedItems.indexOfFirst { it.id == item.id }
                            MediaGridItem(
                                item = item,
                                isSelected = item.id in state.selectedIds,
                                selectionNumber = if (selectedIndex != -1) selectedIndex + 1 else null,
                                onClick = { viewModel.toggleSelection(item) },
                                imageLoader = imageLoader
                            )
                        }

                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }

            if (state.selectedItems.isNotEmpty()) {
                MediaPickerBottomBar(
                    selectedCount = state.selectedItems.size,
                    onNext = { viewModel.setShowPreview(true) }
                )
            }
        }

        if (state.isAlbumSheetVisible) {
            MediaAlbumBottomSheet(
                albums = state.albums,
                currentAlbumId = state.currentAlbum?.id,
                getAlbumId = { it.id },
                getAlbumName = { it.name },
                getAlbumMediaCount = { it.mediaCount },
                getAlbumCoverUri = { it.coverUri },
                onAlbumSelected = { viewModel.selectAlbum(it) },
                onDismiss = { viewModel.toggleAlbumSheet(false) },
                imageLoader = imageLoader
            )
        }

        AnimatedVisibility(
            visible = state.showPreview,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            MediaPreviewOverlay(
                selectedItems = state.selectedItems,
                captionText = state.captionText,
                onCaptionChange = viewModel::setCaptionText,
                onBack = { viewModel.setShowPreview(false) },
                onSend = {
                    onMediaSelected(
                        state.selectedItems.map { it.uri },
                        state.captionText.trim().takeIf { it.isNotBlank() }
                    )
                },
                imageLoader = imageLoader
            )
        }
    }
}
