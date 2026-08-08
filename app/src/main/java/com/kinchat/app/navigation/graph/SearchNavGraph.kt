package com.kinchat.app.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.kinchat.app.features.search.ui.SearchScreen
import com.kinchat.app.navigation.NavRoutes

/** Search destination. */
fun NavGraphBuilder.searchNavGraph(navController: NavHostController) {
    composable(NavRoutes.SEARCH) {
        SearchScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToChat = { userId -> navController.navigate(NavRoutes.chatRoute(userId)) },
            onNavigateToChatWithMessage = { userId, messageId ->
                navController.navigate(NavRoutes.chatRoute(userId, messageId))
            }
        )
    }
}
