package com.kinchat.app.features.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinchat.app.ui.theme.BrandPrimary
import com.kinchat.app.ui.theme.ForegroundLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeHeader(
    onNavigateToSearch: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSaved: () -> Unit = {},
    onNavigateToArchived: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
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
            IconButton(
                onClick = onNavigateToSearch,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = ForegroundLight,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
        )
    )
}
