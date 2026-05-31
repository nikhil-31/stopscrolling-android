package com.example.stopscrolling_android.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.example.stopscrolling_android.data.database.UsageRecord
import com.example.stopscrolling_android.domain.repository.UsageRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class UsageAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var repository: UsageRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastPackageName: String? = null
    private var lastTitle: String? = null
    private var lastUrl: String? = null
    private var lastEventTime: Long = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName == null) return
        val packageName = event.packageName.toString()
        
        val eventType = event.eventType
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            val currentTime = System.currentTimeMillis()
            val rootNode = rootInActiveWindow
            val currentUrl = UrlExtractor.extract(rootNode, packageName)
            val currentTitle = event.text.joinToString(" ").ifBlank { null }

            if (packageName != lastPackageName || currentUrl != lastUrl || (currentTitle != null && currentTitle != lastTitle)) {
                
                // Finalize previous session if needed
                if (lastPackageName != null && lastEventTime != 0L) {
                    saveSession(
                        pkgName = lastPackageName!!,
                        title = lastTitle,
                        url = lastUrl,
                        startTime = lastEventTime,
                        endTime = currentTime
                    )
                }

                lastPackageName = packageName
                lastTitle = currentTitle
                lastUrl = currentUrl
                lastEventTime = currentTime
            }
            rootNode?.recycle()
        }
    }

    private fun saveSession(pkgName: String, title: String?, url: String?, startTime: Long, endTime: Long) {
        if (endTime <= startTime) return
        
        serviceScope.launch {
            val appLabel = getAppLabel(pkgName)
            val record = UsageRecord(
                startTimeUTC = startTime,
                endTimeUTC = endTime,
                title = title,
                url = url,
                appName = appLabel,
                packageName = pkgName,
                category = getAppCategory(pkgName),
                durationSeconds = (endTime - startTime) / 1000,
                source = "Accessibility"
            )
            repository.saveRecord(record)
        }
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun getAppCategory(packageName: String): String {
        return "Unknown"
    }

    override fun onInterrupt() {}
}
