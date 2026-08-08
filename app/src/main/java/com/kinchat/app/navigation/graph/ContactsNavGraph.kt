package com.kinchat.app.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.kinchat.app.features.contacts.ui.ContactsScreen
import com.kinchat.app.navigation.NavRoutes

/** Contacts destination. */
fun NavGraphBuilder.contactsNavGraph(navController: NavHostController) {
    composable(NavRoutes.CONTACTS) {
        ContactsScreen(
            onNavigateToChat = { chatId -> navController.navigate(NavRoutes.chatRoute(chatId)) }
        )
    }
}
