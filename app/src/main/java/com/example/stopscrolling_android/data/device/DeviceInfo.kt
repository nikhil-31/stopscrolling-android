package com.example.stopscrolling_android.data.device

import android.os.Build
import java.util.TimeZone

object DeviceInfo {
    const val PLATFORM = "android"

    fun deviceName(): String = Build.MODEL.orEmpty().ifBlank { "Android device" }

    fun timeZone(): String = TimeZone.getDefault().id
}
