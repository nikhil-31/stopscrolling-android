package com.example.stopscrolling_android.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.stopscrolling_android.presentation.UsageViewModel
import com.example.stopscrolling_android.util.CsvExporter
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    usageViewModel: UsageViewModel,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val backendSettings by settingsViewModel.backendSettings.collectAsState()
    val unsyncedCount by settingsViewModel.unsyncedCount.collectAsState()
    val syncStatus by settingsViewModel.syncStatus.collectAsState()
    val isSyncing by settingsViewModel.isSyncing.collectAsState()

    LaunchedEffect(Unit) {
        settingsViewModel.refreshUnsyncedCount()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Backend", style = MaterialTheme.typography.titleMedium)

                RowWithSwitch(
                    label = "Sync events to backend",
                    checked = backendSettings.syncEnabled,
                    onCheckedChange = settingsViewModel::updateSyncEnabled
                )

                OutlinedTextField(
                    value = backendSettings.apiBaseUrl,
                    onValueChange = settingsViewModel::updateApiBaseUrl,
                    label = { Text("API base URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "Use http://10.0.2.2 on the emulator (maps to host localhost). " +
                        "Devices register via POST /api/devices/. " +
                        "Timeline uses GET /api/insights/timeline/; sync uses GET /api/sync/?device_id=….",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val isCheckingConnection by settingsViewModel.isCheckingConnection.collectAsState()

                OutlinedButton(
                    onClick = settingsViewModel::testConnection,
                    enabled = !isCheckingConnection && !isSyncing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isCheckingConnection) "Checking…" else "Test connection")
                }

                Text(
                    text = "$unsyncedCount events pending sync",
                    style = MaterialTheme.typography.bodyMedium
                )

                syncStatus?.let { status ->
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedButton(
                    onClick = settingsViewModel::syncNow,
                    enabled = !isSyncing && !isCheckingConnection,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSyncing) "Syncing…" else "Sync now (upload + pull)")
                }
            }
        }

        HorizontalDivider()

        Button(
            onClick = {
                CsvExporter.exportToCsv(context, usageViewModel.allRecords.value)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export Data (CSV)")
        }

        Button(
            onClick = {
                scope.launch {
                    usageViewModel.clearAllData()
                    settingsViewModel.refreshUnsyncedCount()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Clear All Data")
        }
    }
}

@Composable
private fun RowWithSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
