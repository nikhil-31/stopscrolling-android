package com.example.stopscrolling_android.presentation.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PermissionScreen(
    viewModel: PermissionViewModel = hiltViewModel(),
    onAllPermissionsGranted: () -> Unit
) {
    val usageStatsGranted by viewModel.isUsageStatsPermissionGranted.collectAsState()
    val accessibilityGranted by viewModel.isAccessibilityPermissionGranted.collectAsState()

    LaunchedEffect(usageStatsGranted, accessibilityGranted) {
        if (usageStatsGranted && accessibilityGranted) {
            onAllPermissionsGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Permissions Required",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Stop Scrolling needs these permissions to track your app usage and help you stay productive.",
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        PermissionItem(
            title = "Usage Access",
            description = "Needed to see which apps you are using and for how long.",
            isGranted = usageStatsGranted,
            onGrantClick = { viewModel.openUsageStatsSettings() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        PermissionItem(
            title = "Accessibility Service",
            description = "Needed to detect browser URLs and page titles.",
            isGranted = accessibilityGranted,
            onGrantClick = { viewModel.openAccessibilitySettings() }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { viewModel.checkPermissions() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Refresh Status")
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (isGranted) {
                Text(text = "Granted", color = MaterialTheme.colorScheme.primary)
            } else {
                Button(onClick = onGrantClick) {
                    Text("Grant Permission")
                }
            }
        }
    }
}
