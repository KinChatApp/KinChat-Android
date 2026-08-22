package com.kinchat.app.features.chat.insights.ui.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kinchat.app.features.chat.insights.domain.model.ChatInsights
import com.kinchat.app.features.chat.insights.ui.components.BadgeCard

@Composable
fun FunBadgesSection(data: ChatInsights) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Fun Badges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            val badges = mutableListOf<@Composable () -> Unit>()
            
            if (data.mostActiveHour.contains("AM") && data.mostActiveHour.takeWhile { it.isDigit() }.toIntOrNull() in 4..9) {
                badges.add { BadgeCard(Icons.Rounded.WbSunny, "Early Bird", "Morning texter", MaterialTheme.colorScheme.primary) }
            }
            
            val hourValue = data.mostActiveHour.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
            if ((data.mostActiveHour.contains("PM") && hourValue >= 10) || data.mostActiveHour.contains("12 AM") || data.mostActiveHour.contains("1 AM") || data.mostActiveHour.contains("2 AM")) {
                badges.add { BadgeCard(Icons.Rounded.DarkMode, "Night Owl", "Late night chats", MaterialTheme.colorScheme.secondary) }
            }
            
            if ((data.mediaStats.myAudio + data.mediaStats.friendAudio) >= 5) {
                badges.add { BadgeCard(Icons.Rounded.Mic, "Voice Lover", "Prefers speaking", MaterialTheme.colorScheme.tertiary) }
            }
            
            if ((data.mediaStats.myImages + data.mediaStats.friendImages) >= 10) {
                badges.add { BadgeCard(Icons.Rounded.PhotoCamera, "Photo Lover", "Shares memories", MaterialTheme.colorScheme.primary) }
            }
            
            if (data.currentUserStats.longest >= 200) {
                badges.add { BadgeCard(Icons.Rounded.Favorite, "Long Writer", "Writes essays", MaterialTheme.colorScheme.secondary) }
            }
            
            if (data.totalMessages > 50) {
                badges.add { BadgeCard(Icons.Rounded.Bolt, "Active Chatter", "Always active", MaterialTheme.colorScheme.primary) }
            }
            
            if (data.totalMessages <= 50 && (data.mediaStats.myAudio + data.mediaStats.friendAudio) < 5) {
                badges.add { BadgeCard(Icons.Rounded.People, "New Friends", "Just getting started", MaterialTheme.colorScheme.tertiary) }
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
