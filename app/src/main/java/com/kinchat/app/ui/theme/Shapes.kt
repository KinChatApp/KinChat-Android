package com.kinchat.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

val ChatBubbleShapeSent = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
val ChatBubbleShapeReceived = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
val BottomSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
