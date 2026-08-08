package com.kinchat.app.features.chat.insights.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kinchat.app.features.chat.insights.domain.model.ChatInsights
import com.kinchat.app.features.chat.insights.ui.components.BadgeCard
import com.kinchat.app.features.chat.insights.ui.components.InfoRow
import com.kinchat.app.features.chat.insights.ui.components.StatRow
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInsightsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChatInsightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Chat Insights", style = MaterialTheme.typography.titleLarge)
                        if (uiState is ChatInsightsUiState.Success) {
                            Text(
                                text = "You and ${(uiState as ChatInsightsUiState.Success).data.friendName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is ChatInsightsUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            "Gathering your memories...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is ChatInsightsUiState.Empty -> {
                    Text(
                        text = "No chat history found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ChatInsightsUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                is ChatInsightsUiState.Success -> {
                    InsightsContent(data = state.data)
                }
            }
        }
    }
}

@Composable
private fun InsightsContent(data: ChatInsights) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { OverviewSection(data) }
        item { TimelineSection(data) }
        item { CallAndInteractionSection(data) }
        item { MediaSharedSection(data) }
        item { HighlightsSection(data) }
        item { FunBadgesSection(data) }
    }
}

@Composable
private fun OverviewSection(data: ChatInsights) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Message Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text("You", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("vs", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(data.friendName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                StatRow(Icons.Rounded.Chat, "Messages", data.currentUserStats.messages, data.friendStats.messages)
                StatRow(Icons.Rounded.TextFields, "Words Typed", data.currentUserStats.words, data.friendStats.words)
                StatRow(Icons.Rounded.Numbers, "Characters", data.currentUserStats.chars, data.friendStats.chars)
            }
        }
    }
}

@Composable
private fun CallAndInteractionSection(data: ChatInsights) {
    val stats = data.extendedStats

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Call, contentDescription = null, tint = Color(0xFF4CAF50))
                Text("Calls & Interactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text("You", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("vs", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(data.friendName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                StatRow(Icons.Rounded.Call, "Audio Calls", stats.myAudioCalls, stats.friendAudioCalls)
                StatRow(Icons.Rounded.VideoCall, "Video Calls", stats.myVideoCalls, stats.friendVideoCalls)
                StatRow(
                    icon = Icons.Rounded.Timer,
                    label = "Call Duration",
                    leftValue = stats.myCallDuration,
                    rightValue = stats.friendCallDuration,
                    leftFormatted = formatDuration(stats.myCallDuration),
                    rightFormatted = formatDuration(stats.friendCallDuration)
                )
                StatRow(Icons.Rounded.Favorite, "Reactions Given", stats.myReactions, stats.friendReactions)
                StatRow(Icons.Rounded.Link, "Links Shared", stats.myLinks, stats.friendLinks)
                StatRow(
                    icon = Icons.Rounded.DataUsage,
                    label = "Data Shared",
                    leftValue = stats.myDataShared,
                    rightValue = stats.friendDataShared,
                    leftFormatted = formatBytes(stats.myDataShared),
                    rightFormatted = formatBytes(stats.friendDataShared)
                )
                
                if (stats.topReaction != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow("Top Reaction in Chat", stats.topReaction.uppercase(), highlight = true)
                }
            }
        }
    }
}

@Composable
private fun MediaSharedSection(data: ChatInsights) {
    val stats = data.mediaStats
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Image, contentDescription = null, tint = Color(0xFFE91E63))
                Text("Media Shared", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text("You", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("vs", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(data.friendName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                StatRow(Icons.Rounded.Image, "Photos", stats.myImages, stats.friendImages)
                StatRow(Icons.Rounded.Videocam, "Videos", stats.myVideos, stats.friendVideos)
                StatRow(Icons.Rounded.Mic, "Voice Msgs", stats.myAudio, stats.friendAudio)
                StatRow(Icons.Rounded.Description, "Documents", stats.myDocuments, stats.friendDocuments)
            }
        }
    }
}

@Composable
private fun TimelineSection(data: ChatInsights) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM, yyyy")
    val firstDate = data.firstMessageAt?.let { 
        try { ZonedDateTime.parse(it).format(formatter) } catch (e: Exception) { "N/A" } 
    } ?: "N/A"
    
    val lastDate = data.lastMessageAt?.let { 
        try { ZonedDateTime.parse(it).format(formatter) } catch (e: Exception) { "N/A" } 
    } ?: "N/A"

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoRow("First Message", firstDate)
                InfoRow("Started By", data.firstMessageSender, highlight = true)
                InfoRow("Latest Message", lastDate)
                InfoRow("Days Connected", "${data.daysConnected} days")
                InfoRow("Avg Messages / Day", "${data.totalMessages / maxOf(1, data.daysConnected)} msgs")
            }
        }
    }
}

@Composable
private fun HighlightsSection(data: ChatInsights) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Color(0xFFFF9800))
                Text("Highlights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoRow("Longest Message", "%,d chars".format(data.longestMessage), highlight = true)
                InfoRow("Most Active Sender", data.mostActiveSender)
                InfoRow("Most Active Day", data.mostActiveDay)
                InfoRow("Most Active Hour", data.mostActiveHour)
            }
        }
    }
}

@Composable
private fun FunBadgesSection(data: ChatInsights) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Color(0xFFFFC107))
                Text("Fun Badges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            val badges = mutableListOf<@Composable () -> Unit>()
            
            if (data.mostActiveHour.contains("AM") && data.mostActiveHour.takeWhile { it.isDigit() }.toIntOrNull() in 4..9) {
                badges.add { BadgeCard(Icons.Rounded.WbSunny, "Early Bird", "Morning texter", Color(0xFFFFC107)) }
            }
            
            val hourValue = data.mostActiveHour.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
            if ((data.mostActiveHour.contains("PM") && hourValue >= 10) || data.mostActiveHour.contains("12 AM") || data.mostActiveHour.contains("1 AM") || data.mostActiveHour.contains("2 AM")) {
                badges.add { BadgeCard(Icons.Rounded.DarkMode, "Night Owl", "Late night chats", Color(0xFF3F51B5)) }
            }
            
            if ((data.mediaStats.myAudio + data.mediaStats.friendAudio) >= 5) {
                badges.add { BadgeCard(Icons.Rounded.Mic, "Voice Lover", "Prefers speaking", Color(0xFFFF9800)) }
            }
            
            if ((data.mediaStats.myImages + data.mediaStats.friendImages) >= 10) {
                badges.add { BadgeCard(Icons.Rounded.PhotoCamera, "Photo Lover", "Shares memories", Color(0xFF2196F3)) }
            }
            
            if (data.currentUserStats.longest >= 200) {
                badges.add { BadgeCard(Icons.Rounded.Favorite, "Long Writer", "Writes essays", Color(0xFFE91E63)) }
            }
            
            if (data.totalMessages > 50) {
                badges.add { BadgeCard(Icons.Rounded.Bolt, "Active Chatter", "Always active", Color(0xFFFFC107)) }
            }
            
            if (data.totalMessages <= 50 && (data.mediaStats.myAudio + data.mediaStats.friendAudio) < 5) {
                badges.add { BadgeCard(Icons.Rounded.People, "New Friends", "Just getting started", Color(0xFF4CAF50)) }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (i in badges.indices step 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) { badges[i]() }
                        Box(modifier = Modifier.weight(1f)) {
                            if (i + 1 < badges.size) badges[i + 1]()
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    if (seconds == 0L) return "0s"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun formatBytes(bytes: Long): String {
    if (bytes == 0L) return "0 MB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        else -> String.format(Locale.US, "%.1f KB", kb)
    }
}
