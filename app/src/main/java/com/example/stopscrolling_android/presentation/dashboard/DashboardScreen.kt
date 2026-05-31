package com.example.stopscrolling_android.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.stopscrolling_android.presentation.UsageViewModel

@Composable
fun DashboardScreen(viewModel: UsageViewModel) {
    val records by viewModel.allRecords.collectAsState()
    
    val totalScreenTime = remember(records) {
        records.sumOf { it.durationSeconds }
    }
    
    val topApps = remember(records) {
        records.groupBy { it.appName }
            .mapValues { entry -> entry.value.sumOf { it.durationSeconds } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = "Today's Usage", style = MaterialTheme.typography.headlineMedium)
        }
        
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Total Screen Time", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = formatDuration(totalScreenTime),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        item {
            Text(text = "Top Apps", style = MaterialTheme.typography.titleLarge)
        }
        
        items(topApps.size) { index ->
            val (appName, duration) = topApps[index]
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = appName)
                Text(text = formatDuration(duration))
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m ${s}s"
}
