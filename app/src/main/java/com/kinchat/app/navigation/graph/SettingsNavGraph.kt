package com.kinchat.app.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.kinchat.app.features.settings.ui.SettingsScreen
import com.kinchat.app.navigation.NavRoutes

/** Settings destination. */
fun NavGraphBuilder.settingsNavGraph(navController: NavHostController) {
    composable(NavRoutes.SETTINGS) {
        SettingsScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToBlocked = { /* To be implemented */ },
            onNavigateToDevices = { /* To be implemented */ },
            onNavigateToFeedback = { /* To be implemented */ },
            onNavigateToPrivacy = { /* To be implemented */ },
            onNavigateToAbout = { /* To be implemented */ },
            onNavigateToLogin = {
                navController.navigate(NavRoutes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }
}
