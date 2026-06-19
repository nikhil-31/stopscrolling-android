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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.stopscrolling_android.presentation.UsageViewModel
import com.example.stopscrolling_android.presentation.timeline.CategoryColors
import com.example.stopscrolling_android.presentation.timeline.HorizontalTimelineBar
import com.example.stopscrolling_android.presentation.timeline.TimelineModel
import com.example.stopscrolling_android.presentation.timeline.VerticalTimelineList
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(viewModel: UsageViewModel) {
    val records by viewModel.allRecords.collectAsState()
    val backendSegments by viewModel.daySegments.collectAsState()
    val timelineStatus by viewModel.timelineStatus.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshDayTimeline()
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
            Text(text = "Today", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = todayLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            timelineStatus?.let { status ->
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Screen time",
                    value = TimelineModel.formatDuration(totalScreenTime),
                    tint = CategoryColors.colorFor("Productivity"),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Sessions",
                    value = "${segments.size}",
                    tint = CategoryColors.colorFor("Development"),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Timeline",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalTimelineBar(
                        segments = segments,
                        dayStartMs = dayStartMs,
                        dayEndMs = dayEndMs,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Recent activity",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    VerticalTimelineList(
                        segments = segments,
                        sessionCount = segments.size,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            HorizontalDivider()
            Text(text = "Top apps today", style = MaterialTheme.typography.titleLarge)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = appName)
                    Text(
                        text = TimelineModel.formatDuration(duration),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = tint.copy(alpha = 0.10f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = tint
            )
        }
    }
}
