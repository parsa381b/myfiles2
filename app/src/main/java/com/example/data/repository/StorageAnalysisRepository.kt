package com.example.data.repository

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.data.model.StorageAnalysisResult
import com.example.data.model.StorageCategoryBreakdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class StorageAnalysisRepository {

    suspend fun analyzeStorage(context: Context): StorageAnalysisResult = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        val (total, free) = try {
            val stat = StatFs(root.path)
            Pair(stat.totalBytes, stat.availableBytes)
        } catch (_: Exception) {
            Pair(0L, 0L)
        }
        val used = (total - free).coerceAtLeast(0L)

        // 1. Gather category sizes from MediaStore & Files
        var imageBytes = 0L
        var imageCount = 0
        var videoBytes = 0L
        var videoCount = 0
        var audioBytes = 0L
        var audioCount = 0
        var docBytes = 0L
        var docCount = 0
        var apkBytes = 0L
        var apkCount = 0
        var archiveBytes = 0L
        var archiveCount = 0

        val largeFiles = ArrayList<FileItem>()

        val docExtensions = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "csv", "epub")
        val apkExtensions = setOf("apk", "xapk", "apks")
        val archiveExtensions = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")

        try {
            val uri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DATE_MODIFIED
            )

            context.contentResolver.query(
                uri,
                projection,
                "${MediaStore.Files.FileColumns.SIZE} > 0",
                null,
                null
            )?.use { cursor ->
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                val mimeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                val dateCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val path = if (dataCol != -1) cursor.getString(dataCol) else null
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    val mime = if (mimeCol != -1) cursor.getString(mimeCol) ?: "" else ""
                    val dateModified = if (dateCol != -1) cursor.getLong(dateCol) * 1000L else 0L

                    if (path == null || size <= 0L) continue

                    val ext = path.substringAfterLast('.', "").lowercase()

                    when {
                        mime.startsWith("image/") || ext in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "svg") -> {
                            imageBytes += size
                            imageCount++
                        }
                        mime.startsWith("video/") || ext in setOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "flv", "ts") -> {
                            videoBytes += size
                            videoCount++
                        }
                        mime.startsWith("audio/") || ext in setOf("mp3", "wav", "flac", "ogg", "m4a", "aac", "wma", "opus") -> {
                            audioBytes += size
                            audioCount++
                        }
                        ext in docExtensions || mime.contains("pdf") || mime.contains("document") || mime.startsWith("text/") -> {
                            docBytes += size
                            docCount++
                        }
                        ext in apkExtensions -> {
                            apkBytes += size
                            apkCount++
                        }
                        ext in archiveExtensions -> {
                            archiveBytes += size
                            archiveCount++
                        }
                    }

                    // Check for large files (>= 50 MB)
                    if (size >= 50 * 1024 * 1024L) {
                        val file = File(path)
                        if (file.exists() && file.isFile) {
                            largeFiles.add(
                                FileItem(
                                    file = file,
                                    name = file.name,
                                    path = file.absolutePath,
                                    isDirectory = false,
                                    size = size,
                                    lastModified = if (dateModified > 0) dateModified else file.lastModified()
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback or file-system check if MediaStore is limited
        if (largeFiles.isEmpty() && root != null && root.exists()) {
            val queue = ArrayDeque<File>()
            queue.add(root)
            var count = 0
            while (queue.isNotEmpty() && count < 2500) {
                val dir = queue.removeFirst()
                val list = dir.listFiles() ?: continue
                for (f in list) {
                    count++
                    if (f.isDirectory) {
                        if (!f.name.startsWith(".") && f.name != "Android") {
                            queue.add(f)
                        }
                    } else if (f.isFile && f.length() >= 50 * 1024 * 1024L) {
                        largeFiles.add(
                            FileItem(
                                file = f,
                                name = f.name,
                                path = f.absolutePath,
                                isDirectory = false,
                                size = f.length(),
                                lastModified = f.lastModified()
                            )
                        )
                    }
                }
            }
        }

        largeFiles.sortByDescending { it.size }
        val topLargeFiles = largeFiles.take(40)

        // 2. Scan for empty folders
        val emptyFolders = ArrayList<File>()
        try {
            if (root != null && root.exists()) {
                val queue = ArrayDeque<File>()
                queue.add(root)
                var scannedDirs = 0
                while (queue.isNotEmpty() && scannedDirs < 1500 && emptyFolders.size < 60) {
                    val dir = queue.removeFirst()
                    scannedDirs++
                    val children = dir.listFiles()
                    if (children != null) {
                        if (children.isEmpty() && dir != root) {
                            if (!dir.name.startsWith(".") && !dir.absolutePath.contains("/Android/")) {
                                emptyFolders.add(dir)
                            }
                        } else {
                            for (child in children) {
                                if (child.isDirectory && !child.name.startsWith(".") && child.name != "Android") {
                                    queue.add(child)
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        val knownBytes = imageBytes + videoBytes + audioBytes + docBytes + apkBytes + archiveBytes
        val otherBytes = (used - knownBytes).coerceAtLeast(0L)

        val categories = listOf(
            StorageCategoryBreakdown(FileCategory.IMAGES, "Images", imageBytes, imageCount, 0xFF3B82F6),
            StorageCategoryBreakdown(FileCategory.VIDEOS, "Videos", videoBytes, videoCount, 0xFF8B5CF6),
            StorageCategoryBreakdown(FileCategory.AUDIO, "Audio", audioBytes, audioCount, 0xFFEC4899),
            StorageCategoryBreakdown(FileCategory.DOCUMENTS, "Documents", docBytes, docCount, 0xFFF59E0B),
            StorageCategoryBreakdown(FileCategory.INSTALLATION_FILES, "Installation Files", apkBytes, apkCount, 0xFF10B981),
            StorageCategoryBreakdown(null, "Archives", archiveBytes, archiveCount, 0xFF06B6D4),
            StorageCategoryBreakdown(null, "System & Other", otherBytes, 0, 0xFF64748B)
        )

        StorageAnalysisResult(
            totalBytes = total,
            usedBytes = used,
            freeBytes = free,
            categories = categories,
            largeFiles = topLargeFiles,
            emptyFolders = emptyFolders
        )
    }

    suspend fun cleanEmptyFolders(folders: List<File>): Int = withContext(Dispatchers.IO) {
        var deleted = 0
        for (f in folders) {
            try {
                if (f.exists() && f.isDirectory && (f.listFiles()?.isEmpty() == true)) {
                    if (f.delete()) deleted++
                }
            } catch (_: Exception) {}
        }
        deleted
    }
}
