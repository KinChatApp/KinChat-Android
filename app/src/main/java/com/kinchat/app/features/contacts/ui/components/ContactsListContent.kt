package com.kinchat.app.features.contacts.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kinchat.app.R
import com.kinchat.app.features.contacts.viewmodel.ContactsUiState

@Composable
fun ContactsListContent(
    uiState: ContactsUiState,
    onOpenChat: (String) -> Unit,
    onInvite: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
    ) {
        item {
            QuickActionsRow()
        }

        uiState.errorMsg?.let { error ->
            item {
                Box(modifier = Modifier.padding(16.dp)) {
                    ErrorBanner(message = error)
                }
            }
        }

        if (uiState.registeredContacts.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(id = R.string.section_on_kinchat),
                    count = uiState.registeredContacts.size,
                    icon = Icons.Default.Person
                )
            }
            itemsIndexed(
                items = uiState.registeredContacts,
                key = { _, it -> it.id ?: it.contactPhoneNormalized.ifEmpty { it.hashCode().toString() } }
            ) { index, contact ->
                val initial = contact.contactName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                val prevInitial = if (index > 0) uiState.registeredContacts[index - 1].contactName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "#" else ""
                val showInitial = initial != prevInitial

                RegisteredContactItem(
                    contact = contact,
                    showInitial = showInitial,
                    initial = initial,
                    onClick = { contact.registeredUserId?.let { onOpenChat(it) } }
                )
            }
        }

        if (uiState.unregisteredContacts.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(id = R.string.section_invite_to_kinchat),
                    count = uiState.unregisteredContacts.size,
                    icon = Icons.Default.PersonAdd
                )
            }
            itemsIndexed(
                items = uiState.unregisteredContacts,
                key = { _, it -> it.id ?: it.contactPhone.ifEmpty { it.hashCode().toString() } }
            ) { index, contact ->
                val initial = contact.contactName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                val prevInitial = if (index > 0) uiState.unregisteredContacts[index - 1].contactName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "#" else ""
                val showInitial = initial != prevInitial

                UnregisteredContactItem(
                    contact = contact,
                    showInitial = showInitial,
                    initial = initial,
                    onInvite = { onInvite(contact.contactPhone) }
                )
            }
        }
    }
}
