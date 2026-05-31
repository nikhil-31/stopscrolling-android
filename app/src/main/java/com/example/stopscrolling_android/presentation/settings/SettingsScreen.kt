package com.example.stopscrolling_android.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.stopscrolling_android.presentation.UsageViewModel
import com.example.stopscrolling_android.util.CsvExporter
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: UsageViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)
        
        Button(
            onClick = {
                CsvExporter.exportToCsv(context, viewModel.allRecords.value)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export Data (CSV)")
        }
        
        Button(
            onClick = {
                scope.launch {
                    viewModel.clearAllData()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Clear All Data")
        }
    }
}
