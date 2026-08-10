package com.kinchat.app.features.chat.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun SendMicButton(
    isTextPresent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isTextPresent,
            transitionSpec = {
                (fadeIn(tween(150)) + scaleIn(initialScale = 0.7f)) togetherWith
                        (fadeOut(tween(100)) + scaleOut(targetScale = 0.7f))
            },
            label = "sendMicSwap"
        ) { hasText ->
            Icon(
                imageVector = if (hasText) Icons.Default.Send else Icons.Default.Mic,
                contentDescription = if (hasText) "Send" else "Record voice message",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
