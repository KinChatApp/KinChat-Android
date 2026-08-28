package com.kinchat.app.features.contacts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinchat.app.domain.model.UserContact

@Composable
fun RegisteredContactItem(
    contact: UserContact,
    showInitial: Boolean = false,
    initial: String = "",
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 8.dp), // Reduced vertical padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            // অ্যালফাবেট লেটার দেখানোর অংশ
            Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.CenterStart) {
                if (showInitial) {
                    Text(
                        text = initial,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp // Slightly smaller
                    )
                }
            }

            // প্রোফাইল পিকচার
            Box(modifier = Modifier.size(44.dp)) { // Reduced from 50.dp
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.contactName.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // নাম এবং স্ট্যাটাস
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.contactName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), // Reduced from 17.sp
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Online",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), // Smaller
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // কল অ্যাকশন আইকনগুলো
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Voice Call",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp).clickable { /* TODO: Call action */ } // Slightly smaller
                )
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Video Call",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp).clickable { /* TODO: Video Call action */ }
                )
            }
        }
        
        HorizontalDivider(
            modifier = Modifier.padding(start = 84.dp, end = 16.dp), // Adjusted start padding
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    }
}
