package com.example.stopscrolling_android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.stopscrolling_android.presentation.UsageViewModel
import com.example.stopscrolling_android.presentation.dashboard.DashboardScreen
import com.example.stopscrolling_android.presentation.onboarding.PermissionScreen
import com.example.stopscrolling_android.presentation.onboarding.PermissionViewModel
import com.example.stopscrolling_android.presentation.timeline.TimelineScreen
import com.example.stopscrolling_android.presentation.auth.AccountScreen
import com.example.stopscrolling_android.presentation.auth.AuthViewModel
import com.example.stopscrolling_android.presentation.settings.SettingsScreen
import com.example.stopscrolling_android.ui.theme.StopscrollingandroidTheme
import com.example.stopscrolling_android.usage.UsageTrackingService
import com.example.stopscrolling_android.worker.BootReceiver
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Start tracking service
        startService(Intent(this, UsageTrackingService::class.java))
        // Schedule periodic collection
        BootReceiver.scheduleUsageCollection(this)

        setContent {
            StopscrollingandroidTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val permissionViewModel: PermissionViewModel = hiltViewModel()
    val usageViewModel: UsageViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()
    var selectedTab by remember { mutableStateOf("dashboard") }

    val allPermissionsGranted by permissionViewModel.allPermissionsGranted.collectAsState()

    LifecycleResumeEffect(allPermissionsGranted) {
        permissionViewModel.checkPermissions()
        if (allPermissionsGranted) {
            authViewModel.registerDeviceOnResume()
        }
        onPauseOrDispose { }
    }

    if (!allPermissionsGranted) {
        PermissionScreen(viewModel = permissionViewModel)
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        selected = selectedTab == "dashboard",
                        onClick = {
                            selectedTab = "dashboard"
                            navController.navigate("dashboard") {
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.History, contentDescription = "Timeline") },
                        label = { Text("Timeline") },
                        selected = selectedTab == "timeline",
                        onClick = {
                            selectedTab = "timeline"
                            navController.navigate("timeline") {
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Account") },
                        label = { Text("Account") },
                        selected = selectedTab == "account",
                        onClick = {
                            selectedTab = "account"
                            navController.navigate("account") {
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = selectedTab == "settings",
                        onClick = {
                            selectedTab = "settings"
                            navController.navigate("settings") {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("dashboard") { DashboardScreen(usageViewModel) }
                composable("timeline") { TimelineScreen(usageViewModel) }
                composable("account") {
                    AccountScreen(authViewModel)
                }
                composable("settings") { SettingsScreen(usageViewModel) }
            }
        }
    }
}
