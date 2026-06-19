package com.example.stopscrolling_android.presentation.timeline

import androidx.compose.ui.graphics.Color

object CategoryColors {
    private val categoryColors = mapOf(
        "Development" to Color(0xFF6B78FA),
        "Productivity" to Color(0xFF1FB894),
        "Communication" to Color(0xFF478FF5),
        "Messaging" to Color(0xFF478FF5),
        "Entertainment" to Color(0xFF9E6BF0),
        "Video" to Color(0xFFF05C94),
        "Social" to Color(0xFFFA8047),
        "Web" to Color(0xFF2EADEB),
        "Application" to Color(0xFF52BD85),
        "Utilities" to Color(0xFF848FA3),
        "Email" to Color(0xFF4D8AEB),
        "Gaming" to Color(0xFF9E6BF0),
        "News" to Color(0xFF2EADEB),
        "Finance" to Color(0xFF52BD85),
        "Shopping" to Color(0xFFFA8047),
        "Education" to Color(0xFF1FB894),
        "Travel" to Color(0xFF478FF5),
        "Health" to Color(0xFF52BD85),
    )

    private val spectrum = listOf(
        Color(0xFF6B78FA),
        Color(0xFF1FB894),
        Color(0xFFFA8047),
        Color(0xFF9E6BF0),
        Color(0xFFF05C94),
        Color(0xFF2EADEB),
        Color(0xFFDB9420),
        Color(0xFF5BAD70),
        Color(0xFF8A70E6),
        Color(0xFFE66B5C),
    )

    fun colorFor(category: String): Color {
        return categoryColors[category] ?: spectrum[stableIndex(category) % spectrum.size]
    }

    private fun stableIndex(key: String): Int {
        var hash = 0
        for (char in key) {
            hash = char.code + (hash * 31)
        }
        return kotlin.math.abs(hash)
    }
}
