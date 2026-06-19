package com.example.stopscrolling_android.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.stopscrolling_android.data.remote.dto.DeviceRow
import com.example.stopscrolling_android.presentation.UsageViewModel
import com.example.stopscrolling_android.util.CsvExporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val isChecking by settingsViewModel.isCheckingConnection.collectAsState()
    val devices by settingsViewModel.devices.collectAsState()

    LaunchedEffect(Unit) {
        settingsViewModel.refreshUnsyncedCount()
        settingsViewModel.refreshDevices()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsSection(title = "Data Synchronization") {
                RowWithSwitch(
                    label = "Sync to backend",
                    icon = Icons.Default.CloudSync,
                    checked = backendSettings.syncEnabled,
                    onCheckedChange = settingsViewModel::updateSyncEnabled
                )

                OutlinedTextField(
                    value = backendSettings.apiBaseUrl,
                    onValueChange = settingsViewModel::updateApiBaseUrl,
                    label = { Text("API base URL") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) }
                )

                Text(
                    text = "Backend endpoint used for multi-device sync and backup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Button(
                    onClick = settingsViewModel::testConnection,
                    enabled = !isChecking && !isSyncing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isChecking) "Checking…" else "Test Connection")
                }
            }
        }

        item {
            SettingsSection(title = "Advanced Tracking") {
                RowWithSwitch(
                    label = "Enhanced tracking",
                    icon = Icons.Default.Troubleshoot,
                    checked = backendSettings.enhancedTrackingEnabled,
                    onCheckedChange = settingsViewModel::updateEnhancedTrackingEnabled
                )

                Text(
                    text = "Accessibility tracking allows capturing URLs from browsers and more accurate idle detection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SettingsSection(title = "Sync Status") {
                Text(
                    text = "$unsyncedCount events pending sync",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )

                syncStatus?.let { status ->
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Button(
                    onClick = settingsViewModel::syncNow,
                    enabled = !isSyncing && !isChecking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (isSyncing) "Syncing…" else "Sync Now")
                }
            }
        }

        if (devices.isNotEmpty()) {
            item {
                SettingsSection(title = "My Devices") {
                    devices.forEachIndexed { index, device ->
                        DeviceItem(device)
                        if (index < devices.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(title = "Data Management") {
                OutlinedButton(
                    onClick = {
                        CsvExporter.exportToCsv(context, usageViewModel.allRecords.value)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export to CSV")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        scope.launch {
                            usageViewModel.clearAllData()
                            settingsViewModel.refreshUnsyncedCount()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Clear All Data")
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun DeviceItem(device: DeviceRow) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (device.label.isNotBlank()) device.label else device.deviceName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
            Text(
                text = "${device.devicePlatform.replaceFirstChar { it.uppercase() }} • ${device.sessionCount} sessions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (device.lastSeenAt != null) {
                Text(
                    text = "Last seen: ${device.lastSeenAt.substringBefore("T")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = when(device.devicePlatform.lowercase()) {
                "android" -> Icons.Default.Android
                "macos", "ios" -> Icons.Default.Smartphone // Fallback as Apple icon might not be in default set
                "windows" -> Icons.Default.Laptop
                else -> Icons.Default.Devices
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun RowWithSwitch(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
