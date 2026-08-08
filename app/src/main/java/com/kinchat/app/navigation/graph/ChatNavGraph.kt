package com.kinchat.app.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kinchat.app.features.chat.info.ui.ChatInfoScreen
import com.kinchat.app.features.chat.insights.ui.ChatInsightsScreen
import com.kinchat.app.features.chat.ui.ChatScreen
import com.kinchat.app.navigation.NavRoutes

/** Chat, chat-info, and chat-insights destinations. */
fun NavGraphBuilder.chatNavGraph(navController: NavHostController) {

    composable(
        route = NavRoutes.CHAT_ROUTE,
        arguments = listOf(
            navArgument(NavRoutes.CHAT_ID_ARG) { type = NavType.StringType },
            navArgument(NavRoutes.MESSAGE_ID_ARG) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val chatId = backStackEntry.arguments?.getString(NavRoutes.CHAT_ID_ARG) ?: ""

        ChatScreen(
            chatId = chatId,
            onBack = { navController.popBackStack() },
            onNavigateToInfo = { id -> navController.navigate(NavRoutes.chatInfoRoute(id)) }
        )
    }

    composable(
        route = NavRoutes.CHAT_INFO_ROUTE,
        arguments = listOf(navArgument(NavRoutes.CHAT_INFO_USER_ID_ARG) { type = NavType.StringType })
    ) {
        ChatInfoScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToMedia = { /* Media viewer navigation not yet implemented */ },
            onNavigateToInsights = { id -> navController.navigate(NavRoutes.chatInsightsRoute(id)) }
        )
    }

    composable(
        route = NavRoutes.CHAT_INSIGHTS_ROUTE,
        arguments = listOf(navArgument(NavRoutes.CHAT_INSIGHTS_USER_ID_ARG) { type = NavType.StringType })
    ) {
        ChatInsightsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
