package com.example.stopscrolling_android.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PermissionScreen(
    viewModel: PermissionViewModel = hiltViewModel()
) {
    val usageStatsGranted by viewModel.isUsageStatsPermissionGranted.collectAsState()
    val accessibilityGranted by viewModel.isAccessibilityPermissionGranted.collectAsState()
    val enhancedTrackingEnabled by viewModel.enhancedTrackingEnabled.collectAsState()

    val needsUsageStats = !usageStatsGranted
    val needsAccessibility = enhancedTrackingEnabled && !accessibilityGranted

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
            text = when {
                needsUsageStats && needsAccessibility ->
                    "Stop Scrolling needs these permissions to track your app usage and help you stay productive."
                needsUsageStats ->
                    "Grant usage access to see which apps you use and for how long."
                else ->
                    "Enable the accessibility service to detect browser URLs and page titles."
            },
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (needsUsageStats) {
            PermissionItem(
                title = "Usage Access",
                description = "Needed to see which apps you are using and for how long.",
                onGrantClick = { viewModel.openUsageStatsSettings() }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (needsAccessibility) {
            PermissionItem(
                title = "Accessibility Service",
                description = "Needed to detect browser URLs and page titles.",
                onGrantClick = { viewModel.openAccessibilitySettings() }
            )
        }

        if (!needsUsageStats || !needsAccessibility) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = buildString {
                    if (!needsUsageStats) append("Usage access granted. ")
                    if (!needsAccessibility) append("Accessibility service enabled.")
                }.trim(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    onGrantClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onGrantClick) {
                Text("Grant Permission")
            }
        }
    }
}
