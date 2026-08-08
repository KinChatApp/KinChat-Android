package com.kinchat.app.navigation

import androidx.navigation.NavHostController

/**
 * Navigates to a top-level bottom-navigation tab destination, preserving the
 * standard single-top + state-restoration behavior expected of a bottom nav bar.
 */
internal fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(NavRoutes.DASHBOARD) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
