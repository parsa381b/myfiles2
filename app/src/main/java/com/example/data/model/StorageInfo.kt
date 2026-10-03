package com.example.data.model

import java.io.File

enum class StorageType {
    INTERNAL,
    SD_CARD,
    USB_DRIVE
}

data class StorageInfo(
    val name: String,
    val rootFile: File,
    val totalBytes: Long,
    val freeBytes: Long,
    val isPrimary: Boolean,
    val type: StorageType = if (isPrimary) StorageType.INTERNAL else StorageType.SD_CARD
) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0L)
    val progress: Float get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}
