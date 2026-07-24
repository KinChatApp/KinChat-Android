package com.kinchat.app.features.chat.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatNormalTopBar(
    displayName: String,
    partnerId: String,
    isPartnerTyping: Boolean,
    isPartnerOnline: Boolean,
    isMenuExpanded: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onBack: () -> Unit,
    onNavigateToInfo: (String) -> Unit
) {
    TopAppBar(
        title = {
            ChatHeaderInfo(null, displayName, isPartnerTyping, isPartnerOnline, onBack) {
                if (partnerId.isNotEmpty()) onNavigateToInfo(partnerId)
            }
        },
        actions = {
            ChatHeaderActions(
                isMessageSelected = false,
                isSaved = false,
                onToggleSave = { },
                onAudioCall = { },
                onVideoCall = { }
            ) {
                ChatHeaderMenu(
                    isMenuExpanded = isMenuExpanded,
                    isMuted = false,
                    isBlocked = false,
                    onMenuToggle = onMenuToggle,
                    onGoToInfo = { if (partnerId.isNotEmpty()) onNavigateToInfo(partnerId) },
                    onAction = { }
                )
            }
        }
    )
}
