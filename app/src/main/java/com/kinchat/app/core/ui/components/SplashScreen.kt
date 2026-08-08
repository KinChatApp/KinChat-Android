package com.kinchat.app.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kinchat.app.R
import kotlinx.coroutines.delay

private const val DEFAULT_SPLASH_DURATION_MILLIS = 2000L
private val SPLASH_LOGO_SIZE = 150.dp

/**
 * Branded splash screen shown on cold app start.
 *
 * Displays the KinChat logo for [splashDurationMillis], then invokes
 * [onSplashFinished] exactly once so the caller can decide the next
 * destination (e.g. based on auth state). [onSplashFinished] is `suspend`
 * because resolving the next destination (e.g. checking auth state) may
 * itself require a suspending call.
 */
@Composable
fun SplashScreen(
    onSplashFinished: suspend () -> Unit,
    splashDurationMillis: Long = DEFAULT_SPLASH_DURATION_MILLIS
) {
    LaunchedEffect(Unit) {
        delay(splashDurationMillis)
        onSplashFinished()
    }

    val splashBackgroundColor = if (isSystemInDarkTheme()) Color.Black else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(splashBackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash),
            contentDescription = "Splash Screen Logo",
            modifier = Modifier.size(SPLASH_LOGO_SIZE)
        )
    }
}
