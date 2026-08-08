package com.kinchat.app.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.kinchat.app.features.developer.logs.ui.DeveloperLogScreen
import com.kinchat.app.navigation.NavRoutes

/** Developer log-viewer destination. */
fun NavGraphBuilder.developerNavGraph(navController: NavHostController) {
    composable(NavRoutes.DEVELOPER_LOGS) {
        DeveloperLogScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
