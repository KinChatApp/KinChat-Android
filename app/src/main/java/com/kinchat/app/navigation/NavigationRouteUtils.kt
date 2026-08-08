package com.kinchat.app.navigation

private val BOTTOM_BAR_ROUTES = setOf(NavRoutes.DASHBOARD, NavRoutes.CONTACTS, NavRoutes.SETTINGS)
private val DEVELOPER_FAB_HIDDEN_ROUTES = setOf(NavRoutes.SPLASH, NavRoutes.DEVELOPER_LOGS)
private const val DEFAULT_ACTIVE_TAB = "chats"

/**
 * Extracts the base route (without path arguments, e.g. "chat/123" -> "chat")
 * for the current back-stack entry's full route string.
 */
internal fun resolveBaseRoute(fullRoute: String?): String? = fullRoute?.substringBefore("/")

/** Whether the shared bottom navigation bar should be shown for [route]. */
internal fun isBottomBarRoute(route: String?): Boolean = route in BOTTOM_BAR_ROUTES

/**
 * Maps the current base [route] to the tab identifier expected by
 * [com.kinchat.app.features.dashboard.ui.components.BottomNavigationBar].
 */
internal fun resolveActiveTab(route: String?): String =
    if (route == NavRoutes.DASHBOARD) DEFAULT_ACTIVE_TAB else route ?: DEFAULT_ACTIVE_TAB

/** Whether the global draggable developer icon should be shown for [route]. */
internal fun isDeveloperFabVisible(route: String?): Boolean = route !in DEVELOPER_FAB_HIDDEN_ROUTES
