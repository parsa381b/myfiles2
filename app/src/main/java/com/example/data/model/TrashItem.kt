package com.example.data.model

import java.io.File

data class TrashItem(
    val id: String,
    val originalPath: String,
    val trashedFile: File,
    val name: String,
    val trashedTimestamp: Long,
    val size: Long,
    val isDirectory: Boolean
) {
    val extension: String
        get() = if (isDirectory) "" else File(originalPath).extension.lowercase()
}
