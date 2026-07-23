package com.kinchat.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.kinchat.app.core.ui.MainLayout
import com.kinchat.app.domain.repository.AuthRepository
import com.kinchat.app.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint
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
                        // 🚀 Clean Code: Navigation is moved to a separate file 🚀
                        AppNavigation(
                            navController = navController,
                            authRepository = authRepository
                        )
                    }
                }
            }
        }
    }
}
