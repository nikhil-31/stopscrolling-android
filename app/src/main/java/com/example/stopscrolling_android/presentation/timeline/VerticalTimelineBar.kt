package com.example.stopscrolling_android.presentation.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun VerticalTimelineBar(
    segments: List<TimelineSegment>,
    dayStartMs: Long,
    dayEndMs: Long,
    modifier: Modifier = Modifier,
    barWidth: Dp = 20.dp,
    onSegmentClick: ((TimelineSegment) -> Unit)? = null
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    Row(modifier = modifier) {
        // Hour Labels
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            TimelineModel.dayGridHours.forEach { hour ->
                Text(
                    text = TimelineModel.hourLabel(hour),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // The Bar
        Layout(
            modifier = Modifier
                .width(barWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(barWidth / 2))
                .background(trackColor),
            content = {
                // Hour markers
                TimelineModel.dayGridHours.forEach { hour ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = if (hour == 0 || hour == 24) 0.12f else 0.06f
                                )
                            )
                    )
                }

                // Segments
                segments.forEach { segment ->
                    val color = CategoryColors.colorFor(segment.category)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color.copy(alpha = 0.9f))
                            .then(
                                if (onSegmentClick != null) {
                                    Modifier.clickable { onSegmentClick(segment) }
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }
        ) { measurables, constraints ->
            val height = if (constraints.hasBoundedHeight) constraints.maxHeight else 0
            val width = if (constraints.hasBoundedWidth) constraints.maxWidth else 0

            val hourMeasurables = measurables.subList(0, TimelineModel.dayGridHours.size)
            val segmentMeasurables = measurables.subList(TimelineModel.dayGridHours.size, measurables.size)

            val hourPlaceables = hourMeasurables.map { it.measure(constraints.copy(minHeight = 0)) }

            val segmentPlaceables = segmentMeasurables.mapIndexed { index, measurable ->
                val segment = segments[index]
                val fraction = segment.fraction(dayStartMs, dayEndMs)
                val segmentHeight = (fraction.width * height).toInt().coerceAtLeast(2)
                measurable.measure(Constraints.fixed(width, segmentHeight))
            }

            layout(width, height) {
                hourPlaceables.forEachIndexed { index, placeable ->
                    val hour = TimelineModel.dayGridHours[index]
                    val y = (hour / 24f * height).toInt()
                    placeable.placeRelative(0, y)
                }

                segmentPlaceables.forEachIndexed { index, placeable ->
                    val segment = segments[index]
                    val fraction = segment.fraction(dayStartMs, dayEndMs)
                    val y = (fraction.x * height).toInt()
                    placeable.placeRelative(0, y)
                }
            }
        }
    }
}
