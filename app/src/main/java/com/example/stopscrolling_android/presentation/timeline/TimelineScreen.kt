package com.example.stopscrolling_android.presentation.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.stopscrolling_android.data.database.UsageRecord
import com.example.stopscrolling_android.data.remote.dto.DeviceRow
import com.example.stopscrolling_android.presentation.UsageViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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
            PrimaryScrollableTabRow(
                selectedTabIndex = displayDevices.indexOfFirst { it.deviceName == selectedDeviceName }.coerceAtLeast(0),
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                displayDevices.forEach { device ->
                    Tab(
                        selected = selectedDeviceName == device.deviceName,
                        onClick = { selectedDeviceName = device.deviceName },
                        text = { 
                            Text(
                                text = if (device.label.isNotBlank()) device.label else device.deviceName,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    )
                }
            }
        } else if (displayDevices.isNotEmpty()) {
             Surface(
                 color = MaterialTheme.colorScheme.surface,
                 modifier = Modifier.fillMaxWidth()
             ) {
                 Row(
                     modifier = Modifier.padding(16.dp),
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     Icon(
                         imageVector = Icons.Default.Devices,
                         contentDescription = null,
                         tint = MaterialTheme.colorScheme.primary,
                         modifier = Modifier.size(20.dp)
                     )
                     Spacer(modifier = Modifier.width(8.dp))
                     val device = displayDevices.first()
                     Text(
                         text = if (device.label.isNotBlank()) device.label else device.deviceName,
                         style = MaterialTheme.typography.titleMedium
                     )
                 }
             }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Search apps, titles, URLs...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        timelineStatus?.let { status ->
             Surface(
                 color = MaterialTheme.colorScheme.errorContainer,
                 modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                 shape = RoundedCornerShape(8.dp)
             ) {
                 Text(
                     text = status,
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onErrorContainer,
                     modifier = Modifier.padding(8.dp)
                 )
             }
        }

        if (filteredRecords.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No matching activity." else "No activity to show yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                val groupedRecords = filteredRecords.groupBy { 
                    SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(it.startTimeUTC))
                }

                groupedRecords.forEach { (date, recordsForDate) ->
                    item {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
                        )
                    }
                    
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(IntrinsicSize.Min)
                        ) {
                            val segments = remember(recordsForDate) {
                                recordsForDate.map { record ->
                                    TimelineSegment(
                                        id = record.id,
                                        startTimeMs = record.startTimeUTC,
                                        endTimeMs = record.endTimeUTC,
                                        label = record.title ?: record.appName,
                                        subtitle = record.appName,
                                        category = record.category,
                                        appName = record.appName,
                                        deviceName = record.deviceName,
                                        url = record.url,
                                        durationSeconds = record.durationSeconds
                                    )
                                }
                            }
                            val (dayStart, dayEnd) = remember(recordsForDate) {
                                TimelineModel.dayBounds(recordsForDate.first().startTimeUTC)
                            }

                            // Vertical Graphical Timeline
                            VerticalTimelineBar(
                                segments = segments,
                                dayStartMs = dayStart,
                                dayEndMs = dayEnd,
                                modifier = Modifier
                                    .width(80.dp)
                                    .fillMaxHeight()
                                    .padding(vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Session Details List
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recordsForDate.forEach { record ->
                                    UsageRecordItem(record)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UsageRecordItem(record: UsageRecord) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val tint = CategoryColors.colorFor(record.category)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(tint)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.appName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1
                    )
                }
                Text(
                    text = timeFormat.format(Date(record.startTimeUTC)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (record.title != null && record.title != record.appName) {
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 4.dp, start = 16.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, start = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = TimelineModel.formatDuration(record.durationSeconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                if (record.source != "BackendSync") {
                     Text(
                         text = "Pending",
                         style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.outline
                     )
                }
            }
        }
    }
}
