package com.kinchat.app.features.chat.ui.components.bubble

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun MessageReactionPopup(
    haptic: HapticFeedback,
    onReact: (String) -> Unit
) {
    // dismissOnBackPress = false so the popup never swallows the Back press;
    // Back is handled by the screen-level BackHandler which clears the
    // selection (and therefore this reaction picker) first.
    Popup(
        alignment = Alignment.TopCenter,
        offset = IntOffset(0, -140),
        properties = PopupProperties(focusable = false, dismissOnBackPress = false)
    ) {
        Row(
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(30.dp))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(30.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val reactions = listOf(
                "👍" to "like",
                "❤️" to "love",
                "😂" to "laugh",
                "😮" to "wow",
                "😢" to "sad",
                "🙏" to "pray"
            )
            
            reactions.forEach { (emoji, type) ->
                Text(
                    text = emoji,
                    fontSize = 28.sp,
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onReact(type)
                    }
                )
            }
        }
    }
}
