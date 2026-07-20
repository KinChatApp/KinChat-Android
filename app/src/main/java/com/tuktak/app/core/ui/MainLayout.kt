package com.tuktak.app.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tuktak.app.features.call.ui.GlobalCallOverlay

/**
 * Android translation of layout.tsx.
 * Acts as the root container, applying global overlays like Call Screens
 * independently of the main navigation stack.
 */
@Composable
fun MainLayout(
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. The Main App Content (Dashboard, NavHost, etc.) equivalent to {children}
        content()
        
        // 2. Global Overlays equivalent to layout.tsx global components
        GlobalCallOverlay()
    }
}
