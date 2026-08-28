package com.kinchat.app.features.contacts.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
fun UnregisteredContactItem(
    contact: UserContact,
    showInitial: Boolean = false,
    initial: String = "",
    onInvite: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp), // Reduced vertical padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            // অ্যালফাবেট লেটার
            Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.CenterStart) {
                if (showInitial) {
                    Text(
                        text = initial,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp // Smaller
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp) // Reduced from 50.dp
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.contactName.take(2).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

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
                Text(
                    text = "Not on KinChat",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), // Smaller
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onInvite,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp) // Smaller button
            ) {
                Text("Invite", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        
        HorizontalDivider(
            modifier = Modifier.padding(start = 84.dp, end = 16.dp), // Adjusted start padding
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    }
}
