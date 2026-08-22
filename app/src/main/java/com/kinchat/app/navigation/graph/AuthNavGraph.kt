package com.kinchat.app.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.kinchat.app.features.auth.ui.LoginScreen
import com.kinchat.app.navigation.NavRoutes

/** Login destination. (Splash is now handled natively) */
fun NavGraphBuilder.authNavGraph(
    navController: NavHostController // 🚀 authRepository প্যারামিটার রিমুভ করা হয়েছে
) {
    composable(NavRoutes.LOGIN) {
        LoginScreen(
            onLoginSuccess = {
                navController.navigate(NavRoutes.DASHBOARD) {
                    popUpTo(NavRoutes.LOGIN) { inclusive = true }
                }
            }
        )
    }
}
