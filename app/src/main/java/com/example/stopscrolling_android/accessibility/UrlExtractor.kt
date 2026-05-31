package com.example.stopscrolling_android.accessibility

import android.view.accessibility.AccessibilityNodeInfo

object UrlExtractor {
    fun extract(rootNode: AccessibilityNodeInfo?, packageName: String): String? {
        if (rootNode == null) return null
        
        return when (packageName) {
            "com.android.chrome" -> extractFromChrome(rootNode)
            "com.microsoft.emmx" -> extractFromEdge(rootNode) // Edge
            "com.brave.browser" -> extractFromBrave(rootNode)
            "org.mozilla.firefox" -> extractFromFirefox(rootNode)
            "com.sec.android.app.sbrowser" -> extractFromSamsung(rootNode)
            else -> null
        }
    }

    private fun extractFromChrome(rootNode: AccessibilityNodeInfo): String? {
        // Find address bar by resource id
        val addressBar = rootNode.findAccessibilityNodeInfosByViewId("com.android.chrome:id/url_bar")
        return addressBar.firstOrNull()?.text?.toString()
    }

    private fun extractFromEdge(rootNode: AccessibilityNodeInfo): String? {
        val addressBar = rootNode.findAccessibilityNodeInfosByViewId("com.microsoft.emmx:id/url_bar")
        return addressBar.firstOrNull()?.text?.toString()
    }

    private fun extractFromBrave(rootNode: AccessibilityNodeInfo): String? {
        val addressBar = rootNode.findAccessibilityNodeInfosByViewId("com.brave.browser:id/url_bar")
        return addressBar.firstOrNull()?.text?.toString()
    }

    private fun extractFromFirefox(rootNode: AccessibilityNodeInfo): String? {
        val addressBar = rootNode.findAccessibilityNodeInfosByViewId("org.mozilla.firefox:id/url_bar_title")
        return addressBar.firstOrNull()?.text?.toString()
    }

    private fun extractFromSamsung(rootNode: AccessibilityNodeInfo): String? {
        val addressBar = rootNode.findAccessibilityNodeInfosByViewId("com.sec.android.app.sbrowser:id/location_bar_edit_text")
        return addressBar.firstOrNull()?.text?.toString()
    }
}
