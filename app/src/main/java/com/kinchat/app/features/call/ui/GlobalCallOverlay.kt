package com.kinchat.app.features.call.ui

import androidx.compose.runtime.Composable

/**
 * Handles global presentation of IncomingCallModal and CallScreen.
 * Observes a global CallViewModel or StateFlow.
 */
@Composable
fun GlobalCallOverlay() {
    // In a complete implementation, this would observe a CallState.
    // If incomingCall != null -> Render IncomingCallDialog()
    // If activeCall != null -> Render FullScreenCallScreen()
    // For now, it remains an empty container to fulfill the layout.tsx architecture requirement.
}
