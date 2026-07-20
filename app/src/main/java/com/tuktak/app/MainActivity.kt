package com.tuktak.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tuktak.app.R
import com.tuktak.app.core.ui.MainLayout
import com.tuktak.app.domain.repository.AuthRepository
import com.tuktak.app.features.auth.ui.LoginScreen
import com.tuktak.app.features.chat.info.ui.ChatInfoScreen
import com.tuktak.app.features.chat.ui.ChatScreen
import com.tuktak.app.features.contacts.ui.ContactsScreen
import com.tuktak.app.features.dashboard.ui.DashboardScreen
import com.tuktak.app.features.search.ui.SearchScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import java.io.PrintWriter
import java.io.StringWriter
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🚀 MASTER TRICK: CRASH CATCHER START 🚀
        val sharedPrefs = getSharedPreferences("CrashLogs", Context.MODE_PRIVATE)
        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            val sw = StringWriter()
            exception.printStackTrace(PrintWriter(sw))
            val exceptionAsString = sw.toString()

            sharedPrefs.edit().putString("last_crash", exceptionAsString).commit()
            defaultExceptionHandler?.uncaughtException(thread, exception)
        }
        // 🚀 CRASH CATCHER END 🚀

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    var crashLogToShow by remember {
                        mutableStateOf(sharedPrefs.getString("last_crash", null))
                    }

                    if (crashLogToShow != null) {
                        AlertDialog(
                            onDismissRequest = {
                                sharedPrefs.edit().remove("last_crash").apply()
                                crashLogToShow = null
                            },
                            title = { Text("App Crashed Last Time! \uD83D\uDEA8") },
                            text = {
                                Text(
                                    text = crashLogToShow ?: "",
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    sharedPrefs.edit().remove("last_crash").apply()
                                    crashLogToShow = null
                                }) {
                                    Text("Clear & Close")
                                }
                            }
                        )
                    }

                    val navController = rememberNavController()

                    MainLayout {
                        NavHost(navController = navController, startDestination = "splash") {
                            
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
                                        // 🚀 এখানে সাইজ পারফেক্ট 150.dp করে দেওয়া হলো 🚀
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
                                    onNavigateToSettings = {},
                                    onNavigateToContacts = {
                                        navController.navigate("contacts") {
                                            popUpTo("dashboard") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onNavigateToAIChat = {}
                                )
                            }

                            composable("contacts") {
                                ContactsScreen(
                                    onNavigateToChat = { chatId ->
                                        navController.navigate("chat/$chatId")
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

                            composable("chat/{chatId}") { backStackEntry ->
                                val chatId = backStackEntry.arguments?.getString("chatId") ?: ""

                                ChatScreen(
                                    chatId = chatId,
                                    onBack = { navController.popBackStack() },
                                    onNavigateToInfo = { id ->
                                        navController.navigate("chatInfo/$id")
                                    }
                                )
                            }

                            composable("chatInfo/{userId}") { backStackEntry ->
                                ChatInfoScreen(
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToMedia = { id ->
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
