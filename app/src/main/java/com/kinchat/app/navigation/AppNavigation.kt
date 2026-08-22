package com.kinchat.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kinchat.app.core.ui.components.DeveloperFloatingButton
import com.kinchat.app.features.dashboard.ui.components.BottomNavigationBar
import com.kinchat.app.navigation.graph.authNavGraph
import com.kinchat.app.navigation.graph.chatNavGraph
import com.kinchat.app.navigation.graph.contactsNavGraph
import com.kinchat.app.navigation.graph.dashboardNavGraph
import com.kinchat.app.navigation.graph.developerNavGraph
import com.kinchat.app.navigation.graph.searchNavGraph
import com.kinchat.app.navigation.graph.settingsNavGraph

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String // 🚀 সরাসরি রুট রিসিভ করবে
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = resolveBaseRoute(navBackStackEntry?.destination?.route)

    val showBottomBar = isBottomBarRoute(currentRoute)
    val activeTab = resolveActiveTab(currentRoute)
    val showDeveloperFab = isDeveloperFabVisible(currentRoute)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    BottomNavigationBar(
                        currentRoute = activeTab,
                        onNavigateToChats = { navController.navigateToTab(NavRoutes.DASHBOARD) },
                        onNavigateToContacts = { navController.navigateToTab(NavRoutes.CONTACTS) },
                        onNavigateToSettings = { navController.navigateToTab(NavRoutes.SETTINGS) }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination, // 🚀 কোনো ডিলে ছাড়া সরাসরি রেন্ডার
                modifier = Modifier.padding(innerPadding)
            ) {
                authNavGraph(navController)
                dashboardNavGraph(navController)
                contactsNavGraph(navController)
                settingsNavGraph(navController)
                searchNavGraph(navController)
                chatNavGraph(navController)
                developerNavGraph(navController)
            }
        }

        if (showDeveloperFab) {
            DeveloperFloatingButton(
                onClick = { navController.navigate(NavRoutes.DEVELOPER_LOGS) }
            )
        }
    }
}
