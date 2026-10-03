package com.example.data.model

import java.io.File

data class StorageCategoryBreakdown(
    val category: FileCategory?,
    val name: String,
    val bytes: Long,
    val count: Int,
    val colorHex: Long
)

data class StorageAnalysisResult(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val categories: List<StorageCategoryBreakdown>,
    val largeFiles: List<FileItem>,
    val emptyFolders: List<File>
)

data class DuplicateGroup(
    val id: String,
    val fileSize: Long,
    val items: List<FileItem>
)
