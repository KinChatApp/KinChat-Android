package com.kinchat.app.features.chat.insights.ui.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kinchat.app.features.chat.insights.domain.model.ChatInsights
import com.kinchat.app.features.chat.insights.ui.components.InfoRow
import com.kinchat.app.features.chat.insights.ui.components.StatRow
import com.kinchat.app.features.chat.insights.ui.utils.formatBytes
import com.kinchat.app.features.chat.insights.ui.utils.formatDuration

@Composable
fun CallAndInteractionSection(data: ChatInsights) {
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
