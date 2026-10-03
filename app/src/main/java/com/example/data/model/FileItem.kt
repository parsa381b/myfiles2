package com.example.data.model

import java.io.File

data class FileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (isDirectory) 0L else file.length(),
    val lastModified: Long = file.lastModified(),
    val extension: String = if (isDirectory) "" else file.extension.lowercase(),
    val subItemCount: Int = 0
)

enum class SortOption {
    NAME_ASC,
    NAME_DESC,
    DATE_DESC,
    DATE_ASC,
    SIZE_DESC,
    SIZE_ASC
}
