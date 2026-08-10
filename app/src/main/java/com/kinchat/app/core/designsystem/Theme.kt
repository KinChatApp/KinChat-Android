package com.kinchat.app.core.designsystem

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandPrimaryForeground,
    
    // Sent Chat Bubble
    primaryContainer = BrandPrimary,
    onPrimaryContainer = BrandPrimaryForeground,
    
    secondary = BrandSecondary,
    onSecondary = BrandSecondaryForeground,
    
    tertiary = BrandAccent, // Success/Online indicators
    error = BrandError,
    
    background = BackgroundLight,
    onBackground = ForegroundLight,
    
    surface = SurfaceLight,
    onSurface = SurfaceForegroundLight,
    
    // Received Chat Bubble
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = SurfaceVariantForegroundLight,
    
    // Secondary Texts (Timestamps, etc.)
    outline = MutedForegroundLight 
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandPrimaryForeground,
    
    // Sent Chat Bubble
    primaryContainer = BrandPrimary,
    onPrimaryContainer = BrandPrimaryForeground,
    
    secondary = BrandSecondary,
    onSecondary = BrandSecondaryForeground,
    
    tertiary = BrandAccent, 
    error = BrandError,
    
    background = BackgroundDark,
    onBackground = ForegroundDark,
    
    surface = SurfaceDark,
    onSurface = SurfaceForegroundDark,
    
    // Received Chat Bubble
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = SurfaceVariantForegroundDark,
    
    outline = MutedForegroundDark
)

@Composable
fun KinChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Kept false to strictly use your provided colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
