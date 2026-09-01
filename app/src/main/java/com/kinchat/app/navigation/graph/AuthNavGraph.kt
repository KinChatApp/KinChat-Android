package com.kinchat.app.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.kinchat.app.features.auth.ui.LoginScreen
import com.kinchat.app.features.auth.ui.OnboardingContactsScreen
import com.kinchat.app.navigation.NavRoutes

fun NavGraphBuilder.authNavGraph(
    navController: NavHostController
) {
    composable(NavRoutes.LOGIN) {
        LoginScreen(
            onLoginSuccess = {
                // 🚀 লগইন সাকসেস হলে ড্যাশবোর্ডে না গিয়ে পারমিশন পেজে যাবে
                navController.navigate("onboarding_contacts") {
                    popUpTo(NavRoutes.LOGIN) { inclusive = true }
                }
            }
        )
    }

    composable("onboarding_contacts") {
        OnboardingContactsScreen(
            onFinish = {
                // 🚀 পারমিশন দেওয়া বা স্কিপ করা হলে ড্যাশবোর্ডে যাবে
                navController.navigate(NavRoutes.DASHBOARD) {
                    popUpTo("onboarding_contacts") { inclusive = true }
                }
            }
        )
    }
}
