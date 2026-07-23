package com.kinchat.app.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// Custom Colors Mapped from Provided CSS
// ==========================================

// Brand Colors
val BrandPrimary = Color(0xFF0F766E)
val BrandPrimaryHover = Color(0xFF115E59)
val BrandPrimaryActive = Color(0xFF134E4A)
val BrandPrimaryForeground = Color(0xFFFFFFFF)

val BrandSecondary = Color(0xFFE2E8F0)
val BrandSecondaryForeground = Color(0xFF0F172A)

val BrandAccent = Color(0xFF14B8A6)
val BrandAccentForeground = Color(0xFFFFFFFF)

// Missing Variables restored to fix Theme.kt Error
val BrandError = Color(0xFFEF4444)

// Light Theme Colors
val BackgroundLight = Color(0xFFF8FAFC)
val ForegroundLight = Color(0xFF0F172A)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceForegroundLight = Color(0xFF0F172A)
val MutedLight = Color(0xFFF1F5F9)
val MutedForegroundLight = Color(0xFF64748B)
val BorderLight = Color(0xFFE2E8F0)

// Missing Variables restored to fix Theme.kt Error
val SurfaceVariantLight = Color(0xFFF1F5F9) 
val SurfaceVariantForegroundLight = Color(0xFF0F172A)

// Status Colors
val Success = Color(0xFF22C55E)
val Warning = Color(0xFFF59E0B)
val Destructive = Color(0xFFEF4444)
val Info = Color(0xFF3B82F6)

// Chat Specific Colors
val ChatBackgroundLight = Color(0xFFF8FAFC)
val ChatSentBubble = Color(0xFF0F766E)
val ChatSentForeground = Color(0xFFFFFFFF)
val ChatReceivedBubble = Color(0xFFFFFFFF)
val ChatReceivedForeground = Color(0xFF0F172A)
val ChatDateSeparator = Color(0xFF94A3B8)

val ChatOnline = Color(0xFF22C55E)
val ChatOffline = Color(0xFF94A3B8)
val ChatTyping = Color(0xFF14B8A6)

// Message Status
val MessageSent = Color(0xFF94A3B8)
val MessageDelivered = Color(0xFF3B82F6)
val MessageRead = Color(0xFF14B8A6)
val MessageFailed = Color(0xFFEF4444)

// Dark Theme Colors (Derived for consistency, uses dark slate backgrounds)
val BackgroundDark = Color(0xFF0F172A)
val ForegroundDark = Color(0xFFF8FAFC)
val SurfaceDark = Color(0xFF1E293B) 
val SurfaceForegroundDark = Color(0xFFF8FAFC)
val SurfaceVariantDark = Color(0xFF334155) 
val SurfaceVariantForegroundDark = Color(0xFFF8FAFC)
val MutedDark = Color(0xFF334155)
val MutedForegroundDark = Color(0xFF94A3B8)
val BorderDark = Color(0xFF334155)
