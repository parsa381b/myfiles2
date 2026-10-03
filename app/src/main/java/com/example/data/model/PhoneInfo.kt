package com.example.data.model

data class PhoneInfo(
    // Device identity
    val deviceName: String,
    val brand: String,
    val manufacturer: String,
    val model: String,
    val board: String,
    val hardware: String,

    // OS & Android
    val androidVersion: String,
    val androidCodeName: String,
    val apiLevel: Int,
    val securityPatch: String,
    val buildId: String,

    // Processor & Architecture
    val primaryArchitecture: String,
    val is64Bit: Boolean,
    val supportedAbis: List<String>,
    val cpuCores: Int,

    // Display
    val resolution: String,
    val densityDpi: Int,
    val densityBucket: String,
    val refreshRate: Float,

    // Battery
    val batteryLevel: Int,
    val batteryHealth: String,
    val batteryStatus: String,
    val batteryPlugged: String,
    val batteryTech: String,
    val batteryTempCelsius: Float,
    val batteryVoltageMv: Int,

    // RAM
    val totalRamBytes: Long,
    val freeRamBytes: Long
)
