package com.kinchat.app.features.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kinchat.app.features.dashboard.viewmodel.HeaderViewModel
import com.kinchat.app.ui.theme.BrandPrimary
import com.kinchat.app.ui.theme.ForegroundLight
import com.valentinilk.shimmer.shimmer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeHeader(
    onNavigateToSearch: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToArchived: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HeaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isDropdownExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // K - Chat Bubble Logo
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = BrandPrimary,
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "K",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // KinChat Title
                Text(
                    text = "KinChat",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = ForegroundLight
                    )
                )
            }
        },
        actions = {
            IconButton(onClick = onNavigateToSearch) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = ForegroundLight,
                    modifier = Modifier.size(28.dp)
                )
            }

            Box(modifier = Modifier.padding(end = 16.dp, start = 8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable { isDropdownExpanded = true }
                ) {
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .shimmer()
                                .background(Color.LightGray)
                        )
                    } else if (uiState.avatarUrl != null) {
                        AsyncImage(
                            model = uiState.avatarUrl,
                            contentDescription = "Profile Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default Avatar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.matchParentSize().padding(4.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Profile", color = MaterialTheme.colorScheme.onSurface) },
                        onClick = { isDropdownExpanded = false; onNavigateToProfile() }
                    )
                    DropdownMenuItem(
                        text = { Text("Saved Messages", color = MaterialTheme.colorScheme.onSurface) },
                        onClick = { isDropdownExpanded = false; onNavigateToSaved() }
                    )
                    DropdownMenuItem(
                        text = { Text("Archived", color = MaterialTheme.colorScheme.onSurface) },
                        onClick = { isDropdownExpanded = false; onNavigateToArchived() }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DropdownMenuItem(
                        text = { Text("Settings", color = MaterialTheme.colorScheme.onSurface) },
                        onClick = { isDropdownExpanded = false; onNavigateToSettings() }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
        )
    )
}
