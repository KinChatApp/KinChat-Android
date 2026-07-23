package com.kinchat.app.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.kinchat.app.R
import com.kinchat.app.domain.repository.AuthRepository
import com.kinchat.app.features.auth.ui.LoginScreen
import com.kinchat.app.features.chat.info.ui.ChatInfoScreen
import com.kinchat.app.features.chat.ui.ChatScreen
import com.kinchat.app.features.contacts.ui.ContactsScreen
import com.kinchat.app.features.dashboard.ui.DashboardScreen
import com.kinchat.app.features.dashboard.ui.components.BottomNavigationBar
import com.kinchat.app.features.search.ui.SearchScreen
import com.kinchat.app.features.settings.ui.SettingsScreen
import kotlinx.coroutines.delay

@Composable
fun AppNavigation(
    navController: NavHostController,
    authRepository: AuthRepository
) {
    // Get current route to determine visibility and active tab
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("/")

    // Which screens should show the bottom navigation bar
    val showBottomBar = currentRoute in listOf("dashboard", "contacts", "settings")
    val activeTab = if (currentRoute == "dashboard") "chats" else currentRoute ?: "chats"

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    currentRoute = activeTab,
                    onNavigateToChats = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToContacts = {
                        navController.navigate("contacts") {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings") {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding)
        ) {

            composable("splash") {
                LaunchedEffect(Unit) {
                    delay(2000)
                    val nextRoute = if (authRepository.isUserLoggedIn()) "dashboard" else "login"
                    navController.navigate(nextRoute) {
                        popUpTo("splash") { inclusive = true }
                    }
                }

                val splashBg = if (isSystemInDarkTheme()) Color.Black else Color.White

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(splashBg),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.splash),
                        contentDescription = "Splash Screen Logo",
                        modifier = Modifier.size(150.dp)
                    )
                }
            }

            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            composable("dashboard") {
                DashboardScreen(
                    onNavigateToChat = { chatId ->
                        navController.navigate("chat/$chatId")
                    },
                    onNavigateToSearch = {
                        navController.navigate("search")
                    },
                    onNavigateToProfile = {},
                    onNavigateToSaved = {},
                    onNavigateToArchived = {},
                    onNavigateToSettings = {
                        navController.navigate("settings") {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToAIChat = {
                        navController.navigate("chat/de438bb4-d954-4c31-9ad1-9dd34b85d981")
                    }
                )
            }

            composable("contacts") {
                ContactsScreen(
                    onNavigateToChat = { chatId ->
                        navController.navigate("chat/$chatId")
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBlocked = { /* To be implemented */ },
                    onNavigateToDevices = { /* To be implemented */ },
                    onNavigateToFeedback = { /* To be implemented */ },
                    onNavigateToPrivacy = { /* To be implemented */ },
                    onNavigateToAbout = { /* To be implemented */ },
                    onNavigateToLogin = {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable("search") {
                SearchScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChat = { userId ->
                        navController.navigate("chat/$userId")
                    },
                    onNavigateToChatWithMessage = { userId, messageId ->
                        navController.navigate("chat/$userId?messageId=$messageId")
                    }
                )
            }

            composable(
                route = "chat/{chatId}",
                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: ""

                ChatScreen(
                    chatId = chatId,
                    onBack = { navController.popBackStack() },
                    onNavigateToInfo = { id ->
                        navController.navigate("chatInfo/$id")
                    }
                )
            }

            composable(
                route = "chatInfo/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                ChatInfoScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMedia = { id -> }
                )
            }
        }
    }
}
