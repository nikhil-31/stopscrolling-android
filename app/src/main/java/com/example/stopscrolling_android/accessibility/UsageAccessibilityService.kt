package com.example.stopscrolling_android.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.example.stopscrolling_android.data.database.UsageRecord
import com.example.stopscrolling_android.data.device.DeviceInfo
import com.example.stopscrolling_android.data.settings.BackendSettingsStore
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

    @Inject
    lateinit var settingsStore: BackendSettingsStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var enhancedTrackingEnabled = true
    private var lastPackageName: String? = null
    private var lastTitle: String? = null
    private var lastUrl: String? = null
    private var lastEventTime: Long = 0
    private var lastInteractionTime: Long = 0

    companion object {
        private const val IDLE_THRESHOLD_MS = 60_000L // 1 minute
    }

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            settingsStore.settings.collect { settings ->
                enhancedTrackingEnabled = settings.enhancedTrackingEnabled
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!enhancedTrackingEnabled) return

        val packageName = event.packageName?.toString() ?: return
        val currentTime = System.currentTimeMillis()

        // 1. Check for idle gap
        if (lastEventTime != 0L && (currentTime - lastInteractionTime) > IDLE_THRESHOLD_MS) {
            finalizeCurrentSession(lastInteractionTime)
        }

        val eventType = event.eventType

        // 2. Update interaction time for relevant events
        val isInteraction = when (eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> true
            else -> false
        }
        if (isInteraction) {
            lastInteractionTime = currentTime
        }

        // 3. Handle window changes and URL extraction
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            val rootNode = rootInActiveWindow
            val currentUrl = UrlExtractor.extract(rootNode, packageName)
            val currentTitle = event.text.joinToString(" ").ifBlank { null }

            val hasAppChanged = packageName != lastPackageName
            val hasUrlChanged = currentUrl != null && currentUrl != lastUrl
            // We only trigger a new session on significant changes or if we were idle
            if (hasAppChanged || hasUrlChanged || lastEventTime == 0L) {
                if (lastEventTime != 0L) {
                    finalizeCurrentSession(currentTime)
                }

                lastPackageName = packageName
                lastTitle = currentTitle
                lastUrl = currentUrl
                lastEventTime = currentTime
                lastInteractionTime = currentTime
            } else if (currentTitle != null && currentTitle != lastTitle) {
                // Update title for current session without starting a new one
                lastTitle = currentTitle
            }
            
            rootNode?.recycle()
        }
    }

    private fun finalizeCurrentSession(endTime: Long) {
        val pkg = lastPackageName ?: return
        val start = lastEventTime
        if (start == 0L || endTime <= start) return

        saveSession(
            pkgName = pkg,
            title = lastTitle,
            url = lastUrl,
            startTime = start,
            endTime = endTime
        )
        lastEventTime = 0
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
                deviceName = DeviceInfo.deviceName(),
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

    override fun onInterrupt() {
        finalizeCurrentSession(System.currentTimeMillis())
    }

    override fun onDestroy() {
        finalizeCurrentSession(System.currentTimeMillis())
        super.onDestroy()
    }
}
