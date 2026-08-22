package com.kinchat.app.features.chat.ui.components.bubble

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinchat.app.core.designsystem.LocalExtendedColors
import com.kinchat.app.features.chat.ui.models.MessageUiModel

@Composable
fun TextContent(
    message: MessageUiModel,
    isSelectionModeEnabled: Boolean,
    onSelect: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current
    val haptic = LocalHapticFeedback.current

    val textColor = if (message.isMe) {
        extendedColors.bubbleSentText
    } else {
        extendedColors.bubbleReceivedText
    }

    // 🚀 1. ডিলিট হওয়া মেসেজের জন্য কাস্টম লজিক
    if (message.status.isDeleted) {
        val deletedText = if (message.isMe) "🚫 You deleted this message" else "🚫 This message was deleted"
        
        Text(
            text = deletedText,
            color = textColor.copy(alpha = 0.8f),
            fontStyle = FontStyle.Italic,
            fontSize = 15.sp,
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .pointerInput(message.id, isSelectionModeEnabled) {
                    detectTapGestures(
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelect()
                        },
                        onTap = {
                            if (isSelectionModeEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect()
                            }
                        }
                    )
                }
        )
        return // ডিলিট হলে আর নিচের নরমাল টেক্সট/লিংক রেন্ডার করবে না
    }

    // 🚀 2. নরমাল মেসেজের জন্য সাধারণ লজিক
    val linkColor = if (message.isMe) extendedColors.bubbleSentText else extendedColors.linkColor
    val uriHandler = LocalUriHandler.current

    val linkRegex = android.util.Patterns.WEB_URL.toRegex()
    val matchResult = linkRegex.find(message.content)
    val firstUrl = matchResult?.value

    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        for (match in linkRegex.findAll(message.content)) {
            append(message.content.substring(lastIndex, match.range.first))
            pushStringAnnotation(tag = "URL", annotation = match.value)
            withStyle(style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Medium)) {
                append(match.value)
            }
            pop()
            lastIndex = match.range.last + 1
        }
        append(message.content.substring(lastIndex))
    }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Column {
        Text(
            text = annotatedString,
            color = textColor,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .pointerInput(message.id, isSelectionModeEnabled) {
                    detectTapGestures(
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelect()
                        },
                        onTap = { pos ->
                            if (isSelectionModeEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect()
                            } else {
                                layoutResult?.let { layout ->
                                    val offset = layout.getOffsetForPosition(pos)
                                    annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                        .firstOrNull()?.let { annotation ->
                                            var url = annotation.item
                                            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                                url = "https://$url"
                                            }
                                            try { uriHandler.openUri(url) } catch (e: Exception) {}
                                        }
                                }
                            }
                        }
                    )
                },
            onTextLayout = { layoutResult = it }
        )

        if (firstUrl != null) {
            Spacer(modifier = Modifier.height(8.dp))
            LinkPreviewWidget(
                url = firstUrl,
                isMe = message.isMe,
                isSelectionModeEnabled = isSelectionModeEnabled,
                onSelect = onSelect
            )
        }
    }
}
