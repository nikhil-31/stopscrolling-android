package com.example.stopscrolling_android.presentation.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.stopscrolling_android.data.database.UsageRecord
import com.example.stopscrolling_android.presentation.UsageViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TimelineScreen(viewModel: UsageViewModel) {
    val records by viewModel.allRecords.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    val filteredRecords = remember(records, searchQuery) {
        if (searchQuery.isBlank()) records
        else records.filter { 
            it.appName.contains(searchQuery, ignoreCase = true) || 
            it.title?.contains(searchQuery, ignoreCase = true) == true ||
            it.url?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Search apps, titles, URLs...") }
        )

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
            Text(text = "Source: ${record.source}", style = MaterialTheme.typography.labelSmall)
        }
    }
}
