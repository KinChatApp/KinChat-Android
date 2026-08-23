package com.kinchat.app.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.kinchat.app.features.dashboard.ui.DashboardScreen
import com.kinchat.app.navigation.NavRoutes
import com.kinchat.app.navigation.navigateToTab

fun NavGraphBuilder.dashboardNavGraph(navController: NavHostController) {
    composable(NavRoutes.DASHBOARD) {
        DashboardScreen(
            // 🚀 Navigation-এর সময় chatId এবং chatName দুটোই পাঠানো হচ্ছে
            onNavigateToChat = { chatId, chatName -> 
                navController.navigate(NavRoutes.chatRoute(chatId = chatId, chatName = chatName)) 
            },
            onNavigateToSearch = { navController.navigate(NavRoutes.SEARCH) },
            onNavigateToProfile = { /* Profile screen not yet implemented */ },
            onNavigateToSaved = { /* Saved-messages screen not yet implemented */ },
            onNavigateToArchived = { /* Archived-chats screen not yet implemented */ },
            onNavigateToSettings = { navController.navigateToTab(NavRoutes.SETTINGS) },
            onNavigateToAIChat = {
                navController.navigate(NavRoutes.chatRoute(chatId = NavRoutes.AI_ASSISTANT_CHAT_ID, chatName = "AI Assistant"))
            }
        )
    }
}
