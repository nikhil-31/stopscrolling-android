package com.example.stopscrolling_android.presentation.onboarding

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.ViewModel
import com.example.stopscrolling_android.accessibility.UsageAccessibilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isUsageStatsPermissionGranted = MutableStateFlow(hasUsageStatsPermission())
    val isUsageStatsPermissionGranted = _isUsageStatsPermissionGranted.asStateFlow()

    private val _isAccessibilityPermissionGranted = MutableStateFlow(isAccessibilityServiceEnabled())
    val isAccessibilityPermissionGranted = _isAccessibilityPermissionGranted.asStateFlow()

    val allPermissionsGranted: Boolean
        get() = _isUsageStatsPermissionGranted.value && _isAccessibilityPermissionGranted.value

    fun checkPermissions() {
        _isUsageStatsPermissionGranted.value = hasUsageStatsPermission()
        _isAccessibilityPermissionGranted.value = isAccessibilityServiceEnabled()
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponent = ComponentName(context, UsageAccessibilityService::class.java)
        val accessibilityManager =
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        if (enabledServices.any { serviceInfo ->
                val service = serviceInfo.resolveInfo.serviceInfo
                expectedComponent.packageName == service.packageName &&
                    expectedComponent.className == service.name
            }
        ) {
            return true
        }

        val enabledSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val flattened = expectedComponent.flattenToString()
        return enabledSetting.split(':').any { it.equals(flattened, ignoreCase = true) }
    }

    fun openUsageStatsSettings() {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openAccessibilitySettings() {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
