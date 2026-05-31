package com.example.stopscrolling_android.usage

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build

object CategoryMapper {
    fun getCategory(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (appInfo.category) {
                    android.content.pm.ApplicationInfo.CATEGORY_GAME -> "Gaming"
                    android.content.pm.ApplicationInfo.CATEGORY_AUDIO -> "Entertainment"
                    android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> "Entertainment"
                    android.content.pm.ApplicationInfo.CATEGORY_IMAGE -> "Entertainment"
                    android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> "Social"
                    android.content.pm.ApplicationInfo.CATEGORY_NEWS -> "News"
                    android.content.pm.ApplicationInfo.CATEGORY_MAPS -> "Travel"
                    android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
                    else -> mapPackageToCategory(packageName)
                }
            } else {
                mapPackageToCategory(packageName)
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun mapPackageToCategory(packageName: String): String {
        return when {
            packageName.contains("messaging") || packageName.contains("whatsapp") || packageName.contains("telegram") -> "Messaging"
            packageName.contains("social") || packageName.contains("facebook") || packageName.contains("instagram") || packageName.contains("twitter") || packageName.contains("tiktok") -> "Social"
            packageName.contains("mail") || packageName.contains("gmail") || packageName.contains("outlook") -> "Productivity"
            packageName.contains("bank") || packageName.contains("finance") || packageName.contains("wallet") -> "Finance"
            packageName.contains("shop") || packageName.contains("amazon") || packageName.contains("ebay") -> "Shopping"
            packageName.contains("video") || packageName.contains("youtube") || packageName.contains("netflix") || packageName.contains("disney") -> "Entertainment"
            packageName.contains("edu") || packageName.contains("learn") -> "Education"
            packageName.contains("news") -> "News"
            packageName.contains("travel") || packageName.contains("uber") || packageName.contains("lyft") -> "Travel"
            packageName.contains("health") || packageName.contains("fit") -> "Health"
            packageName.contains("game") -> "Gaming"
            else -> "Unknown"
        }
    }
}
