package com.kinchat.app.core.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private const val DEVELOPER_FAB_LABEL = "</>"
private val DEVELOPER_FAB_BOTTOM_PADDING = 120.dp
private val DEVELOPER_FAB_END_PADDING = 16.dp

/**
 * Global, draggable shortcut that opens the developer log viewer.
 *
 * Position is remembered with [rememberSaveable] so it survives both
 * configuration changes and being temporarily removed from composition
 * (e.g. while the splash or developer-log screen itself is shown).
 */
@Composable
fun DeveloperFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by rememberSaveable { mutableStateOf(0f) }
    var offsetY by rememberSaveable { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = DEVELOPER_FAB_BOTTOM_PADDING, end = DEVELOPER_FAB_END_PADDING),
        contentAlignment = Alignment.BottomEnd
    ) {
        SmallFloatingActionButton(
            onClick = onClick,
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                },
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ) {
            Text(text = DEVELOPER_FAB_LABEL, fontWeight = FontWeight.Bold)
        }
    }
}
