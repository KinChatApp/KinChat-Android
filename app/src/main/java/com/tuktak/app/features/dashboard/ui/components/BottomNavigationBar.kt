package com.tuktak.app.features.dashboard.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigateToChats: () -> Unit,
    onNavigateToContacts: () -> Unit
) {
    val activeColor = Color(0xFF22C55E)

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp
    ) {
        val isChatsActive = currentRoute == "chats"
        NavigationBarItem(
            selected = isChatsActive,
            onClick = onNavigateToChats,
            icon = {
                Icon(
                    // Deprecated warning ফিক্স করতে AutoMirrored আইকন ব্যবহার করা হলো
                    imageVector = if (isChatsActive) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat,
                    contentDescription = "Chats"
                )
            },
            label = { Text("Chats") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = activeColor,
                selectedTextColor = activeColor,
                indicatorColor = activeColor.copy(alpha = 0.1f)
            )
        )

        val isContactsActive = currentRoute == "contacts"
        NavigationBarItem(
            selected = isContactsActive,
            onClick = onNavigateToContacts,
            icon = {
                Icon(
                    imageVector = if (isContactsActive) Icons.Filled.Contacts else Icons.Outlined.Contacts,
                    contentDescription = "Contacts"
                )
            },
            label = { Text("Contacts") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = activeColor,
                selectedTextColor = activeColor,
                indicatorColor = activeColor.copy(alpha = 0.1f)
            )
        )
    }
}
