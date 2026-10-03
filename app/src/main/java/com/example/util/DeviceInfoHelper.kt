package com.example.util

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import com.example.data.model.PhoneInfo

object DeviceInfoHelper {

    fun getPhoneInfo(context: Context): PhoneInfo {
        // 1. Device identity
        val deviceName = try {
            Settings.Global.getString(context.contentResolver, "device_name")
                ?: Build.MODEL
        } catch (_: Exception) {
            Build.MODEL
        }

        val brand = Build.BRAND.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val model = Build.MODEL
        val board = Build.BOARD
        val hardware = Build.HARDWARE

        // 2. Android OS
        val androidVersion = Build.VERSION.RELEASE
        val apiLevel = Build.VERSION.SDK_INT
        val codeName = when (apiLevel) {
            36 -> "Android 16 (Baklava)"
            35 -> "Android 15 (Vanilla Ice Cream)"
            34 -> "Android 14 (Upside Down Cake)"
            33 -> "Android 13 (Tiramisu)"
            31, 32 -> "Android 12 (Snow Cone)"
            30 -> "Android 11 (Red Velvet Cake)"
            29 -> "Android 10 (Quince Tart)"
            28 -> "Android 9 (Pie)"
            26, 27 -> "Android 8 (Oreo)"
            24, 25 -> "Android 7 (Nougat)"
            else -> "Android API $apiLevel"
        }
        val securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Build.VERSION.SECURITY_PATCH
        } else "N/A"
        val buildId = Build.DISPLAY ?: Build.ID

        // 3. Processor & Architecture (arm7 vs arm8 etc.)
        val supportedAbis = Build.SUPPORTED_ABIS.toList()
        val primaryAbi = supportedAbis.firstOrNull() ?: "Unknown"
        val is64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()

        val primaryArchitecture = when {
            primaryAbi.contains("arm64-v8a", ignoreCase = true) -> "ARMv8 (ARM64 64-bit)"
            primaryAbi.contains("armeabi-v7a", ignoreCase = true) -> "ARMv7 (32-bit)"
            primaryAbi.contains("armeabi", ignoreCase = true) -> "ARM (32-bit)"
            primaryAbi.contains("x86_64", ignoreCase = true) -> "x86_64 (64-bit)"
            primaryAbi.contains("x86", ignoreCase = true) -> "x86 (32-bit)"
            else -> primaryAbi
        }

        val cpuCores = Runtime.getRuntime().availableProcessors()

        // 4. Display
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm?.defaultDisplay?.getRealMetrics(metrics)

        val resolution = "${metrics.widthPixels} × ${metrics.heightPixels} px"
        val densityDpi = metrics.densityDpi
        val densityBucket = when {
            densityDpi >= 640 -> "xxxhdpi"
            densityDpi >= 480 -> "xxhdpi"
            densityDpi >= 320 -> "xhdpi"
            densityDpi >= 240 -> "hdpi"
            densityDpi >= 160 -> "mdpi"
            else -> "ldpi"
        }
        @Suppress("DEPRECATION")
        val refreshRate = wm?.defaultDisplay?.refreshRate ?: 60f

        // 5. Battery
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryLevel = if (level >= 0 && scale > 0) (level * 100) / scale else 0

        val healthCode = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val batteryHealth = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified Failure"
            else -> "Good / Normal"
        }

        val statusCode = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val batteryStatus = when (statusCode) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Normal"
        }

        val pluggedCode = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val batteryPlugged = when (pluggedCode) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Dock"
            else -> "On Battery"
        }

        val batteryTech = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"
        val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val batteryTempCelsius = if (tempRaw > 0) tempRaw / 10.0f else 28.5f
        val batteryVoltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

        // 6. RAM
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)

        return PhoneInfo(
            deviceName = deviceName,
            brand = brand,
            manufacturer = manufacturer,
            model = model,
            board = board,
            hardware = hardware,
            androidVersion = androidVersion,
            androidCodeName = codeName,
            apiLevel = apiLevel,
            securityPatch = securityPatch,
            buildId = buildId,
            primaryArchitecture = primaryArchitecture,
            is64Bit = is64Bit,
            supportedAbis = supportedAbis,
            cpuCores = cpuCores,
            resolution = resolution,
            densityDpi = densityDpi,
            densityBucket = densityBucket,
            refreshRate = refreshRate,
            batteryLevel = batteryLevel,
            batteryHealth = batteryHealth,
            batteryStatus = batteryStatus,
            batteryPlugged = batteryPlugged,
            batteryTech = batteryTech,
            batteryTempCelsius = batteryTempCelsius,
            batteryVoltageMv = batteryVoltageMv,
            totalRamBytes = memInfo.totalMem,
            freeRamBytes = memInfo.availMem
        )
    }
}
