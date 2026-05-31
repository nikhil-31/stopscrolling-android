package com.example.stopscrolling_android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
    
    val usageStatsGranted by permissionViewModel.isUsageStatsPermissionGranted.collectAsState()
    val accessibilityGranted by permissionViewModel.isAccessibilityPermissionGranted.collectAsState()
    
    val showOnboarding = !usageStatsGranted || !accessibilityGranted

    if (showOnboarding) {
        PermissionScreen(
            viewModel = permissionViewModel,
            onAllPermissionsGranted = { permissionViewModel.checkPermissions() }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        selected = true, // Simplified for brevity
                        onClick = { navController.navigate("dashboard") }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.History, contentDescription = "Timeline") },
                        label = { Text("Timeline") },
                        selected = false,
                        onClick = { navController.navigate("timeline") }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = false,
                        onClick = { navController.navigate("settings") }
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
                composable("settings") { SettingsScreen(usageViewModel) }
            }
        }
    }
}
