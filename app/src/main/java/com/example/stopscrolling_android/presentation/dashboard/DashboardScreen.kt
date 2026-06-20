package com.example.stopscrolling_android.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.stopscrolling_android.data.remote.dto.DeviceRow
import com.example.stopscrolling_android.presentation.UsageViewModel
import com.example.stopscrolling_android.presentation.timeline.CategoryColors
import com.example.stopscrolling_android.presentation.timeline.HorizontalTimelineBar
import com.example.stopscrolling_android.presentation.timeline.TimelineModel
import com.example.stopscrolling_android.presentation.timeline.TimelineSegment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(viewModel: UsageViewModel) {
    val records by viewModel.allRecords.collectAsState()
    val backendSegments by viewModel.daySegments.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val timelineStatus by viewModel.timelineStatus.collectAsState()

    var selectedSegment by remember { mutableStateOf<TimelineSegment?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshDayTimeline()
        viewModel.refreshHistory() // Also refreshes devices
    }

    val (dayStartMs, dayEndMs) = remember { TimelineModel.dayBounds() }
    // The outbox holds only not-yet-uploaded sessions; overlay them so the most
    // recent activity is visible before it has been uploaded to the backend.
    val outboxSegments = remember(records, dayStartMs, dayEndMs) {
        TimelineModel.toSegments(
            TimelineModel.recordsForDay(records, dayStartMs, dayEndMs),
            dayStartMs,
            dayEndMs
        )
    }
    val segments = remember(backendSegments, outboxSegments) {
        if (backendSegments != null) {
            TimelineModel.mergeSegments(backendSegments!!, outboxSegments)
        } else {
            outboxSegments
        }
    }
    val totalScreenTime = remember(segments) {
        segments.sumOf { it.durationSeconds }
    }

    val topApps = remember(segments) {
        segments.groupBy { it.appName }
            .mapValues { entry -> entry.value.sumOf { it.durationSeconds } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
    }

    val todayLabel = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            timelineStatus?.let { status ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            Text(
                text = todayLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Screen time",
                    value = TimelineModel.formatDuration(totalScreenTime),
                    icon = Icons.Default.HourglassEmpty,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Sessions",
                    value = "${segments.size}",
                    icon = Icons.AutoMirrored.Filled.List,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Usage Timeline",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val displayDevices = remember(segments, devices) {
                        val namesFromSegments = segments.map { it.deviceName }.distinct()
                        val fromSegments = namesFromSegments.map { name ->
                            val device = devices.find { it.deviceName == name }
                            device ?: DeviceRow(
                                deviceId = "",
                                devicePlatform = "",
                                deviceName = name,
                                registeredAt = "",
                                updatedAt = ""
                            )
                        }
                        (fromSegments + devices).distinctBy { it.deviceName }.sortedBy { it.deviceName }
                    }

                    if (displayDevices.isEmpty()) {
                        HorizontalTimelineBar(
                            segments = emptyList(),
                            dayStartMs = dayStartMs,
                            dayEndMs = dayEndMs,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        displayDevices.forEach { device ->
                            val deviceSegments = segments.filter { it.deviceName == device.deviceName }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 4.dp, top = if (displayDevices.first() == device) 0.dp else 12.dp)
                            ) {
                                Text(
                                    text = if (device.label.isNotBlank()) device.label else device.deviceName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                val deviceTime = deviceSegments.sumOf { it.durationSeconds }
                                if (deviceTime > 0) {
                                    Text(
                                        text = TimelineModel.formatDuration(deviceTime),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            HorizontalTimelineBar(
                                segments = deviceSegments,
                                dayStartMs = dayStartMs,
                                dayEndMs = dayEndMs,
                                modifier = Modifier.fillMaxWidth(),
                                barHeight = 48.dp,
                                onSegmentClick = { selectedSegment = it }
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Top Apps", style = MaterialTheme.typography.titleLarge)
        }

        if (topApps.isEmpty()) {
            item {
                Text(
                    text = "No app usage recorded yet today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(topApps.size) { index ->
                val (appName, duration) = topApps[index]
                val maxDuration = topApps.first().second
                val fraction = if (maxDuration > 0) duration.toFloat() / maxDuration else 0f
                
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = appName, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = TimelineModel.formatDuration(duration),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = CategoryColors.colorFor(appName), // Fallback to app name if category unknown
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }

    selectedSegment?.let { segment ->
        AlertDialog(
            onDismissRequest = { selectedSegment = null },
            confirmButton = {
                TextButton(onClick = { selectedSegment = null }) {
                    Text("Close")
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = segment.appName)
                }
            },
            text = {
                val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow(label = "Category", value = segment.category)
                    DetailRow(label = "Duration", value = TimelineModel.formatDuration(segment.durationSeconds))
                    DetailRow(
                        label = "Time", 
                        value = "${timeFormat.format(Date(segment.startTimeMs))} - ${timeFormat.format(Date(segment.endTimeMs))}"
                    )
                    if (segment.label != segment.appName) {
                        DetailRow(label = "Detail", value = segment.label)
                    }
                    if (segment.url != null) {
                        DetailRow(label = "URL", value = segment.url)
                    }
                    DetailRow(label = "Device", value = segment.deviceName)
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
