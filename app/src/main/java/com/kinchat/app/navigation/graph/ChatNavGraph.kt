package com.kinchat.app.navigation.graph

import android.net.Uri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kinchat.app.features.chat.info.ui.ChatInfoScreen
import com.kinchat.app.features.chat.insights.ui.ChatInsightsScreen
import com.kinchat.app.features.chat.ui.ChatScreen
import com.kinchat.app.features.media.ui.MediaPickerScreen
import com.kinchat.app.navigation.NavRoutes

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

        // Get results back from Media Picker
        val savedStateHandle = backStackEntry.savedStateHandle
        val selectedMedia = savedStateHandle.get<List<String>>("selected_media_uris")
        val caption = savedStateHandle.get<String>("selected_media_caption")
        val replyId = savedStateHandle.get<String>("selected_media_reply_id")

        ChatScreen(
            chatId = chatId,
            returnedMediaUris = selectedMedia?.map { Uri.parse(it) },
            returnedCaption = caption,
            returnedReplyId = replyId,
            onMediaProcessed = { 
                savedStateHandle.remove<List<String>>("selected_media_uris")
                savedStateHandle.remove<String>("selected_media_caption")
                savedStateHandle.remove<String>("selected_media_reply_id")
            },
            onBack = { navController.popBackStack() },
            onNavigateToInfo = { id -> navController.navigate(NavRoutes.chatInfoRoute(id)) },
            onNavigateToMediaPicker = { currentReplyId -> 
                navController.navigate(NavRoutes.chatMediaPickerRoute(chatId, currentReplyId))
            }
        )
    }

    composable(
        route = NavRoutes.CHAT_MEDIA_PICKER_ROUTE,
        arguments = listOf(
            navArgument(NavRoutes.CHAT_ID_ARG) { type = NavType.StringType },
            navArgument(NavRoutes.REPLY_ID_ARG) { type = NavType.StringType; nullable = true; defaultValue = null }
        )
    ) { backStackEntry ->
        val replyId = backStackEntry.arguments?.getString(NavRoutes.REPLY_ID_ARG)
        
        MediaPickerScreen(
            onDismiss = { navController.popBackStack() },
            onMediaSelected = { uris, caption ->
                navController.previousBackStackEntry?.savedStateHandle?.set("selected_media_uris", uris.map { it.toString() })
                navController.previousBackStackEntry?.savedStateHandle?.set("selected_media_caption", caption)
                navController.previousBackStackEntry?.savedStateHandle?.set("selected_media_reply_id", replyId)
                navController.popBackStack()
            }
        )
    }

    composable(
        route = NavRoutes.CHAT_INFO_ROUTE,
        arguments = listOf(navArgument(NavRoutes.CHAT_INFO_USER_ID_ARG) { type = NavType.StringType })
    ) {
        ChatInfoScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToMedia = { },
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
