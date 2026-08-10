package com.kinchat.app.features.chat.insights.ui.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kinchat.app.features.chat.insights.domain.model.ChatInsights
import com.kinchat.app.features.chat.insights.ui.components.InfoRow
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun TimelineSection(data: ChatInsights) {
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
