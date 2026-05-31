package com.example.stopscrolling_android.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.stopscrolling_android.data.database.UsageRecord
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {
    fun exportToCsv(context: Context, records: List<UsageRecord>) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        
        val csvHeader = "starttime(UTC),endtime(UTC),title,url,app,category,platform\n"
        val csvData = StringBuilder(csvHeader)
        
        records.forEach { record ->
            csvData.append("${dateFormat.format(Date(record.startTimeUTC))},")
            csvData.append("${dateFormat.format(Date(record.endTimeUTC))},")
            csvData.append("\"${record.title ?: ""}\",")
            csvData.append("\"${record.url ?: ""}\",")
            csvData.append("\"${record.appName}\",")
            csvData.append("\"${record.category}\",")
            csvData.append("${record.platform}\n")
        }
        
        val file = File(context.cacheDir, "usage_export_${System.currentTimeMillis()}.csv")
        file.writeText(csvData.toString())
        
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Export Data"))
    }
}
