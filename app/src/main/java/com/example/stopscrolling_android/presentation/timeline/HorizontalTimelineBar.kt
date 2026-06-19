package com.example.stopscrolling_android.presentation.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun HorizontalTimelineBar(
    segments: List<TimelineSegment>,
    dayStartMs: Long,
    dayEndMs: Long,
    modifier: Modifier = Modifier,
    barHeight: androidx.compose.ui.unit.Dp = 48.dp
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    Column(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(trackColor)
        ) {
            val totalWidth = maxWidth
            DayGridLines()

            segments.forEach { segment ->
                val fraction = segment.fraction(dayStartMs, dayEndMs)
                val blockWidth = (fraction.width * totalWidth.value).coerceAtLeast(2f).dp
                val xOffset = (fraction.x * totalWidth.value).dp
                val color = CategoryColors.colorFor(segment.category)

                Box(
                    modifier = Modifier
                        .offset(x = xOffset)
                        .padding(vertical = if (barHeight < 30.dp) 2.dp else 4.dp)
                        .width(blockWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(if (barHeight < 30.dp) 2.dp else 4.dp))
                        .background(color.copy(alpha = 0.9f))
                )
            }
        }

        if (barHeight > 30.dp) {
            HourAxis(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun DayGridLines() {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
        TimelineModel.dayGridHours.forEach { hour ->
            val x = (hour / 24f) * maxWidth.value
            Box(
                modifier = Modifier
                    .offset(x = x.dp)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (hour == 0 || hour == 24) 0.12f else 0.06f
                        )
                    )
            )
        }
    }
}

@Composable
private fun HourAxis(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.height(16.dp)) {
        TimelineModel.dayGridHours.forEach { hour ->
            val label = TimelineModel.hourLabel(hour)
            val x = (hour / 24f) * maxWidth.value
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .offset(x = (x - 12).coerceAtLeast(0f).dp)
                    .align(Alignment.TopStart)
            )
        }
    }
}
