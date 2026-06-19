package com.example.stopscrolling_android.presentation.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.stopscrolling_android.data.database.UsageRecord
import com.example.stopscrolling_android.data.remote.dto.DeviceRow
import com.example.stopscrolling_android.presentation.UsageViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TimelineScreen(viewModel: UsageViewModel) {
    // History is rendered from the backend sessions API; the local outbox is overlaid
    // so not-yet-uploaded activity is still visible.
    val remoteRecords by viewModel.historyRecords.collectAsState()
    val outboxRecords by viewModel.allRecords.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val timelineStatus by viewModel.timelineStatus.collectAsState()
    val isLoading by viewModel.isLoadingTimeline.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshHistory()
    }

    val records = remember(remoteRecords, outboxRecords) {
        TimelineModel.mergeRecords(remoteRecords ?: emptyList(), outboxRecords)
    }

    val displayDevices = remember(records, devices) {
        val fromRecords = records.map { it.deviceName }.distinct().map { name ->
            val device = devices.find { it.deviceName == name }
            device ?: DeviceRow(
                deviceId = "",
                devicePlatform = "",
                deviceName = name,
                registeredAt = "",
                updatedAt = ""
            )
        }
        val fromDevices = devices
        (fromRecords + fromDevices).distinctBy { it.deviceName }.sortedBy { it.deviceName }
    }

    var selectedDeviceName by remember(displayDevices) {
        mutableStateOf(displayDevices.firstOrNull()?.deviceName ?: "This Device")
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredRecords = remember(records, searchQuery, selectedDeviceName) {
        records.filter { it.deviceName == selectedDeviceName }
            .filter { 
                if (searchQuery.isBlank()) true
                else it.appName.contains(searchQuery, ignoreCase = true) || 
                     it.title?.contains(searchQuery, ignoreCase = true) == true ||
                     it.url?.contains(searchQuery, ignoreCase = true) == true
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (displayDevices.size > 1) {
            ScrollableTabRow(
                selectedTabIndex = displayDevices.indexOfFirst { it.deviceName == selectedDeviceName }.coerceAtLeast(0),
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                displayDevices.forEach { device ->
                    Tab(
                        selected = selectedDeviceName == device.deviceName,
                        onClick = { selectedDeviceName = device.deviceName },
                        text = { 
                            Text(if (device.label.isNotBlank()) device.label else device.deviceName)
                        }
                    )
                }
            }
        }

        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Search apps, titles, URLs...") }
        )

        timelineStatus?.let { status ->
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (filteredRecords.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(
                    text = when {
                        isLoading -> "Loading…"
                        searchQuery.isNotBlank() -> "No matching activity."
                        else -> "No activity to show yet."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredRecords) { record ->
                UsageRecordItem(record)
            }
        }
    }
}

@Composable
fun UsageRecordItem(record: UsageRecord) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = record.appName, style = MaterialTheme.typography.titleMedium)
                Text(text = record.category, style = MaterialTheme.typography.labelSmall)
            }
            if (record.title != null) {
                Text(text = record.title, style = MaterialTheme.typography.bodyMedium)
            }
            if (record.url != null) {
                Text(text = record.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${dateFormat.format(Date(record.startTimeUTC))} - ${dateFormat.format(Date(record.endTimeUTC))}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(text = "${record.durationSeconds}s", style = MaterialTheme.typography.bodySmall)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Source: ${record.source}", style = MaterialTheme.typography.labelSmall)
                Text(text = record.deviceName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
