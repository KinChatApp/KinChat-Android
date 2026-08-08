package com.kinchat.app.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.kinchat.app.core.ui.components.SplashScreen
import com.kinchat.app.domain.repository.AuthRepository
import com.kinchat.app.features.auth.ui.LoginScreen
import com.kinchat.app.navigation.NavRoutes

/** Splash + login destinations. */
fun NavGraphBuilder.authNavGraph(
    navController: NavHostController,
    authRepository: AuthRepository
) {
    composable(NavRoutes.SPLASH) {
        SplashScreen(
            onSplashFinished = {
                val nextRoute = if (authRepository.isUserLoggedIn()) {
                    NavRoutes.DASHBOARD
                } else {
                    NavRoutes.LOGIN
                }
                navController.navigate(nextRoute) {
                    popUpTo(NavRoutes.SPLASH) { inclusive = true }
                }
            }
        )
    }

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
